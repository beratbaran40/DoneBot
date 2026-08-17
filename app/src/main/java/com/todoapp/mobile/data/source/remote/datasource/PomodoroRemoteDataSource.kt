package com.todoapp.mobile.data.source.remote.datasource

import com.todoapp.mobile.common.handleRequest
import com.todoapp.mobile.data.model.network.data.PomodoroSessionListData
import com.todoapp.mobile.data.model.network.data.PomodoroUploadData
import com.todoapp.mobile.data.model.network.request.PomodoroSessionDto
import com.todoapp.mobile.data.model.network.request.PomodoroSessionUploadRequest
import com.todoapp.mobile.data.source.remote.api.ToDoApi
import javax.inject.Inject

interface PomodoroRemoteDataSource {
    suspend fun upload(sessions: List<PomodoroSessionDto>): Result<PomodoroUploadData>

    /** [from] and [to] are epoch days in the device's own zone, inclusive. */
    suspend fun list(from: Long, to: Long): Result<PomodoroSessionListData>
}

class PomodoroRemoteDataSourceImpl
@Inject
constructor(
    private val api: ToDoApi,
) : PomodoroRemoteDataSource {

    override suspend fun upload(sessions: List<PomodoroSessionDto>): Result<PomodoroUploadData> = handleRequest { api.uploadPomodoroSessions(PomodoroSessionUploadRequest(sessions)) }

    override suspend fun list(from: Long, to: Long): Result<PomodoroSessionListData> = handleRequest { api.getPomodoroSessions(from = from, to = to) }
}
