package com.todoapp.mobile.data.source.local.converter

import androidx.room.TypeConverter
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

class StringListConverter {
    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = ListSerializer(String.serializer())

    @TypeConverter
    fun fromList(value: List<String>): String = json.encodeToString(serializer, value)

    @TypeConverter
    fun toList(value: String): List<String> = runCatching { json.decodeFromString(serializer, value) }.getOrDefault(emptyList())
}
