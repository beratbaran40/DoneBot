package com.todoapp.mobile.data.mapper

import com.todoapp.mobile.data.model.entity.JournalEntryEntity
import com.todoapp.mobile.data.source.local.converter.StringListConverter
import com.todoapp.mobile.domain.model.JournalEntry
import com.todoapp.mobile.domain.model.JournalMood

private val stringListConverter = StringListConverter()

fun JournalEntryEntity.toDomain(): JournalEntry = JournalEntry(
    id = id,
    title = title,
    content = content,
    mood = mood?.let { runCatching { JournalMood.valueOf(it) }.getOrNull() },
    photoPaths = stringListConverter.toList(photoPaths),
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun JournalEntry.toEntity(): JournalEntryEntity = JournalEntryEntity(
    id = id,
    title = title,
    content = content,
    mood = mood?.name,
    photoPaths = stringListConverter.fromList(photoPaths),
    createdAt = createdAt,
    updatedAt = updatedAt,
)
