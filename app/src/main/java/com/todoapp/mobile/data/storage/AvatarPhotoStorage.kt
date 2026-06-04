package com.todoapp.mobile.data.storage

import android.content.Context
import android.graphics.Bitmap
import com.todoapp.mobile.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Short-lived storage for cropped avatar images. The crop screen writes the cropped bitmap here, the
 * uploading ViewModel reads its bytes, then deletes it — so files are transient (unlike
 * [JournalPhotoStorage], whose cleanup is tied to journal-entry deletion). Lives in its own
 * `avatar_crop_tmp` dir so a stray temp never gets mistaken for a journal photo.
 */
@Singleton
class AvatarPhotoStorage @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val tmpDir: File by lazy {
        File(context.filesDir, DIR_NAME).apply {
            if (!exists()) mkdirs()
        }
    }

    /**
     * Compresses a cropped [Bitmap] to a JPEG in app-private storage and returns its absolute path.
     * Does NOT recycle [bitmap] — the caller owns its lifecycle. Returns `null` on IO failure.
     */
    suspend fun savePhotoFromBitmap(bitmap: Bitmap): String? = withContext(ioDispatcher) {
        runCatching {
            val target = File(tmpDir, "${UUID.randomUUID()}.jpg")
            target.outputStream().use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
            }
            target.absolutePath
        }.onFailure {
            Timber.tag(TAG).w(it, "Failed to save cropped avatar bitmap")
        }.getOrNull()
    }

    /** Reads the persisted cropped JPEG back into memory for upload. Returns `null` on IO failure. */
    suspend fun readPhotoBytes(path: String): ByteArray? = withContext(ioDispatcher) {
        runCatching { File(path).readBytes() }
            .onFailure { Timber.tag(TAG).w(it, "Failed to read cropped avatar %s", path) }
            .getOrNull()
    }

    suspend fun deletePhoto(path: String) {
        withContext(ioDispatcher) {
            runCatching { File(path).delete() }
                .onFailure { Timber.tag(TAG).w(it, "Failed to delete cropped avatar %s", path) }
        }
    }

    private companion object {
        const val DIR_NAME = "avatar_crop_tmp"
        const val TAG = "AvatarPhotoStorage"
        const val JPEG_QUALITY = 92
    }
}
