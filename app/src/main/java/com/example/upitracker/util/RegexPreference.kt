package com.example.upitracker.util

import android.content.Context
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.regexDataStore by preferencesDataStore(name = "regex_settings")

object RegexPreference {
    private val REGEX_SET_KEY = stringSetPreferencesKey("upi_regex_patterns")

    fun getRegexPatterns(context: Context): Flow<Set<String>> =
        context.regexDataStore.data.map { prefs ->
            prefs[REGEX_SET_KEY] ?: emptySet()
        }

    suspend fun setRegexPatterns(context: Context, patterns: Set<String>) {
        context.regexDataStore.edit { prefs ->
            prefs[REGEX_SET_KEY] = patterns
        }
    }
}
