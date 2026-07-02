package com.todoapp.mobile.data.auth

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
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

    fun decrypt(stored: String): String? {
        if (!stored.startsWith(PREFIX)) return stored
        return runCatching {
            val blob = Base64.decode(stored.removePrefix(PREFIX), Base64.NO_WRAP)
            val iv = blob.copyOfRange(0, GCM_IV_LENGTH)
            val ciphertext = blob.copyOfRange(GCM_IV_LENGTH, blob.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        }.getOrNull()
    }

    private fun secretKey(): SecretKey {
        cachedKey?.let { return it }
        synchronized(this) {
            cachedKey?.let { return it }
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            val existing = (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey
            return (existing ?: generateKey()).also { cachedKey = it }
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
    }
}
