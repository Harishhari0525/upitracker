package com.example.upitracker.util

import android.content.Context
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

// Define the DataStore instance at the top level
val Context.regexDataStore by preferencesDataStore(name = "regex_settings")

object RegexPreference {
    // Key for storing the set of regex patterns
    private val REGEX_SET_KEY = stringSetPreferencesKey("upi_regex_patterns")

    /**
     * Retrieves the Flow of custom regex patterns.
     * Emits an empty set if no patterns are stored or if there's an error.
     */
    fun getRegexPatterns(context: Context): Flow<Set<String>> =
        context.regexDataStore.data
            .map { prefs ->
                prefs[REGEX_SET_KEY] ?: emptySet() // Default to an empty set if not found
            }
            .catch { _ ->
                // Handle potential exceptions during DataStore read (e.g., IOException)
                // Log the error and emit an empty set as a fallback
                // android.util.Log.e("RegexPreference", "Error reading regex patterns", exception)
                emit(emptySet())
            }

    /**
     * Saves the set of custom regex patterns.
     * Replaces any existing set.
     */
    suspend fun setRegexPatterns(context: Context, patterns: Set<String>) {
        context.regexDataStore.edit { prefs ->
            prefs[REGEX_SET_KEY] = patterns
        }
    }
}