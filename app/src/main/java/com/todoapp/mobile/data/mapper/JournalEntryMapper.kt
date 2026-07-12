package com.todoapp.mobile.data.mapper

import com.todoapp.mobile.data.model.entity.JournalEntryEntity
import com.todoapp.mobile.data.source.local.converter.StringListConverter
import com.todoapp.mobile.domain.model.JournalEntry

private val stringListConverter = StringListConverter()

fun JournalEntryEntity.toDomain(): JournalEntry = JournalEntry(
    id = id,
    title = title,
    content = content,
    photoPaths = stringListConverter.toList(photoPaths),
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun JournalEntry.toEntity(): JournalEntryEntity = JournalEntryEntity(
    id = id,
    title = title,
    content = content,
    photoPaths = stringListConverter.fromList(photoPaths),
    createdAt = createdAt,
    updatedAt = updatedAt,
)
