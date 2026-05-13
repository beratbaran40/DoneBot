package com.todoapp.mobile.data.storage

import android.content.Context
import android.net.Uri
import com.todoapp.mobile.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JournalPhotoStorage @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val photosDir: File by lazy {
        File(context.filesDir, DIR_NAME).apply {
            if (!exists()) mkdirs()
        }
    }

    /**
     * Copies a [Uri] (typically returned by a system Photo Picker) into app-private
     * storage and returns the absolute path of the persisted file. Resolves opaque
     * `content://` URIs that the OS may revoke after the launching activity is gone.
     *
     * Returns `null` on failure (unreadable source, IO error).
     */
    suspend fun savePhoto(uri: Uri): String? = withContext(ioDispatcher) {
        runCatching {
            val target = File(photosDir, "${UUID.randomUUID()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } ?: return@runCatching null
            target.absolutePath
        }.onFailure {
            Timber.tag(TAG).w(it, "Failed to copy journal photo")
        }.getOrNull()
    }

    suspend fun deletePhoto(path: String) {
        withContext(ioDispatcher) {
            runCatching { File(path).delete() }
                .onFailure { Timber.tag(TAG).w(it, "Failed to delete journal photo %s", path) }
        }
    }

    suspend fun deletePhotos(paths: Collection<String>) {
        paths.forEach { deletePhoto(it) }
    }

    private companion object {
        const val DIR_NAME = "journal_photos"
        const val TAG = "JournalPhotoStorage"
    }
}
