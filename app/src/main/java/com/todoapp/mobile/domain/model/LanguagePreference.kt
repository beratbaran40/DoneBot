package com.todoapp.mobile.domain.model

import java.util.Locale

enum class LanguagePreference {
    ENGLISH,
    TURKISH,
    ;

    fun toLocale(): Locale = when (this) {
        ENGLISH -> Locale.ENGLISH
        TURKISH -> Locale.forLanguageTag("tr")
    }

    companion object {
        const val PREFS_NAME = "language_prefs"
        const val PREFS_KEY = "app_language"
    }
}
