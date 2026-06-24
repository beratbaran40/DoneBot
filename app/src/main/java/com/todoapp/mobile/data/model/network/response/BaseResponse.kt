package com.todoapp.mobile.data.model.network.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
open class BaseResponse<T>(
    @SerialName("code") open val code: Int,
    @SerialName("message") open val message: String,
    @SerialName("data") val data: T?,
    @SerialName("errorCode") open val errorCode: String? = null,
)

typealias ErrorResponse = BaseResponse<Nothing>
