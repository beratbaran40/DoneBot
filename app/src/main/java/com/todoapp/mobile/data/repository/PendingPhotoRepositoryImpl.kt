// Detekt mis-flags code after `?: return false` guards as unreachable (false positive).
@file:Suppress("UnreachableCode")

package com.todoapp.mobile.data.repository

import android.content.Context
import com.todoapp.mobile.data.model.entity.PendingPhotoEntity
import com.todoapp.mobile.data.source.local.PendingPhotoDao
import com.todoapp.mobile.di.IoDispatcher
import com.todoapp.mobile.domain.repository.PendingPhotoRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PendingPhotoRepositoryImpl
@Inject
constructor(
    @ApplicationContext private val context: Context,
    private val dao: PendingPhotoDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : PendingPhotoRepository {
    private val baseDir: File by lazy {
        File(context.filesDir, "pending_photos").also { if (!it.exists()) it.mkdirs() }
    }

    override suspend fun queue(localTaskId: Long, bytes: ByteArray, mimeType: String): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            val file = File(baseDir, "${UUID.randomUUID()}.bin")
            file.writeBytes(bytes)
            dao.insert(
                PendingPhotoEntity(
                    localTaskId = localTaskId,
                    mimeType = mimeType,
                    filePath = file.absolutePath,
                ),
            )
            Timber.tag("PendingPhoto").d("queued localTaskId=$localTaskId path=${file.name} bytes=${bytes.size}")
        }
    }

    override suspend fun drain(
        localTaskId: Long,
        remoteTaskId: Long,
        upload: suspend (ByteArray, String) -> Result<Unit>,
    ): Int = withContext(ioDispatcher) {
        val pending = dao.getByLocalTaskId(localTaskId)
        Timber.tag("PendingPhoto").d("drain localTaskId=$localTaskId -> remoteId=$remoteTaskId pending=${pending.size}")
        var uploaded = 0
        for (row in pending) {
            if (drainOne(row, upload)) uploaded++
        }
        uploaded
    }

    private suspend fun drainOne(
        row: com.todoapp.mobile.data.model.entity.PendingPhotoEntity,
        upload: suspend (ByteArray, String) -> Result<Unit>,
    ): Boolean {
        val file = File(row.filePath)
        if (!file.exists()) {
            Timber.tag("PendingPhoto").w("missing file ${row.filePath}; deleting row")
            dao.deleteById(row.id)
            return false
        }
        val bytes = runCatching { file.readBytes() }.getOrNull() ?: return false
        val result = upload(bytes, row.mimeType)
        if (result.isFailure) {
            Timber.tag("PendingPhoto").w(result.exceptionOrNull(), "upload failed; keeping queued row=${row.id}")
            return false
        }
        dao.deleteById(row.id)
        runCatching { file.delete() }
        return true
    }

    override suspend fun clearAll() = withContext(ioDispatcher) {
        runCatching {
            dao.deleteAll()
            baseDir.listFiles()?.forEach { it.delete() }
        }
        Unit
    }
}
