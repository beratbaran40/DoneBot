package com.todoapp.mobile.data.auth

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.util.Base64
import com.google.firebase.crashlytics.FirebaseCrashlytics
import java.security.KeyStore
import javax.crypto.AEADBadTagException
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Encrypts session token *values* with a hardware-backed AndroidKeyStore AES-256-GCM key before they are
 * written to the (plaintext) Preferences DataStore, so a rooted device or forensic pull can't read the raw
 * JWTs. Tokens stay in DataStore (robust) — only the value is ciphertext, so the "tokens live in DataStore"
 * backup/wipe invariants documented in LocalStorageModule + the backup rules stay true.
 *
 * Stored form is `"$PREFIX${base64(iv || ciphertext)}"`. The prefix lets [decrypt] tell an encrypted blob
 * from a legacy plaintext token (written before this class existed) or a Keystore-broken fallback:
 * - prefixed + decryptable  -> plaintext
 * - prefixed + undecryptable (key lost on reinstall/restore, or tampering) -> null => "no token" => clean logout
 * - unprefixed -> passthrough (legacy plaintext keeps working until the next write re-encrypts it)
 *
 * The key uses no user-authentication requirement on purpose: tokens must be readable for background network
 * calls while the device is locked. This is at-rest confidentiality, not per-use authorization.
 */
@Singleton
class TokenCipher
@Inject
constructor() {
    @Volatile
    private var cachedKey: SecretKey? = null

    // Last blob we decrypted successfully in this process. Lets a later *transient* keystore fault on
    // the SAME stored value fall back to a known-good plaintext instead of a flaky null (which would
    // read as "no token" and force a spurious logout).
    @Volatile
    private var lastGoodCipher: String? = null

    @Volatile
    private var lastGoodPlain: String? = null

    fun encrypt(plain: String): String = runCatching {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        PREFIX + Base64.encodeToString(iv + ciphertext, Base64.NO_WRAP)
    }.getOrElse {
        // Keystore utterly unavailable (extremely rare). Never break auth: store as-is; the unprefixed
        // value still reads back via decrypt()'s passthrough — it just isn't encrypted on this device.
        plain
    }

    /**
     * Decrypts a stored token value, hardened against *transient* AndroidKeyStore faults (a "cold"
     * keystore right after boot/unlock, OEM hiccups). Those must NOT masquerade as "no token" and
     * trigger a spurious logout:
     *  - a genuinely-undecryptable blob (wrong/lost key, corruption, tamper) still returns null — the
     *    documented "null => clean logout" contract — but is now recorded instead of failing silently;
     *  - a transient fault is retried (dropping a possibly-wedged key handle first), and if this exact
     *    blob decrypted successfully earlier in the process we return that cached plaintext rather than
     *    a flaky null.
     */
    fun decrypt(stored: String): String? {
        if (!stored.startsWith(PREFIX)) return stored
        var lastCause: Throwable? = null
        repeat(DECRYPT_MAX_ATTEMPTS) { attempt ->
            val outcome = runCatching { decryptOnce(stored) }
            outcome.getOrNull()?.let { plain ->
                lastGoodCipher = stored
                lastGoodPlain = plain
                return plain
            }
            lastCause = outcome.exceptionOrNull()
            if (lastCause?.let(::isGenuineUndecryptable) == true) {
                recordDecryptFailure(stored, lastCause, transient = false)
                return null
            }
            // Transient keystore fault: trust a prior success for the SAME blob over a flaky failure.
            if (stored == lastGoodCipher) return lastGoodPlain
            cachedKey = null // drop a possibly-wedged key handle so the next attempt reloads it
            if (attempt < DECRYPT_MAX_ATTEMPTS - 1) runCatching { Thread.sleep(DECRYPT_BACKOFF_MS) }
        }
        recordDecryptFailure(stored, lastCause, transient = true)
        return null
    }

    private fun decryptOnce(stored: String): String {
        val blob = Base64.decode(stored.removePrefix(PREFIX), Base64.NO_WRAP)
        val iv = blob.copyOfRange(0, GCM_IV_LENGTH)
        val ciphertext = blob.copyOfRange(GCM_IV_LENGTH, blob.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }

    // A wrong/lost key or corrupted/tampered blob is permanent → honour the "null = clean logout"
    // contract. Everything else the keystore throws is treated as transient (bias to keeping session).
    // (AEADBadTagException is a BadPaddingException subclass; both are listed for clarity.)
    private fun isGenuineUndecryptable(t: Throwable): Boolean = t is AEADBadTagException || t is BadPaddingException || t is KeyPermanentlyInvalidatedException

    // Records straight to Crashlytics (bypassing Timber's redaction), so this must NEVER receive the
    // token/ciphertext — only lengths, flags and exception class-names.
    private fun recordDecryptFailure(stored: String, cause: Throwable?, transient: Boolean) {
        runCatching {
            FirebaseCrashlytics.getInstance().apply {
                setCustomKey("token_decrypt_transient", transient)
                setCustomKey("token_decrypt_blob_len", stored.length)
                setCustomKey("token_decrypt_cause", cause?.javaClass?.simpleName ?: "none")
                recordException(cause ?: IllegalStateException("token decrypt failed (transient=$transient)"))
            }
        }
    }

    private fun secretKey(): SecretKey {
        cachedKey?.let { return it }
        synchronized(this) {
            cachedKey?.let { return it }
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            val existing = (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey
            if (existing != null) return existing.also { cachedKey = it }
            // getEntry returned null. Mint a key ONLY when the alias truly does not exist (first run /
            // reinstall). If the alias IS present but the entry came back null, the keystore is
            // transiently wedged — throw so decrypt() retries. Regenerating here would orphan every
            // previously-stored ciphertext forever (a permanent forced logout).
            if (keyStore.containsAlias(KEY_ALIAS)) {
                error("Keystore alias present but entry unavailable (transient)")
            }
            return generateKey().also { cachedKey = it }
        }
    }

    private fun generateKey(): SecretKey {
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec
                .Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(AES_KEY_SIZE)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "donebot_session_token_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val PREFIX = "enc1:"
        const val AES_KEY_SIZE = 256
        const val GCM_IV_LENGTH = 12
        const val GCM_TAG_BITS = 128
        const val DECRYPT_MAX_ATTEMPTS = 3
        const val DECRYPT_BACKOFF_MS = 40L
    }
}
