package com.example.upitracker.util

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

// Define the DataStore instance at the top level, associated with the Context.
// The name "settings" is generic; ensure it doesn't clash if you add more general settings.
// If this DataStore is only for theme, "theme_settings" might be more specific.
val Context.settingsDataStore by preferencesDataStore(name = "app_settings") // Renamed for clarity if it holds more than just theme

object ThemePreference {
    private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode_enabled") // Slightly more descriptive key name

    /**
     * Retrieves the Flow for the dark mode preference.
     * Emits false (light mode) if not set or if there's an error.
     */
    fun isDarkModeFlow(context: Context): Flow<Boolean> =
        context.settingsDataStore.data.map { prefs: Preferences ->
            prefs[DARK_MODE_KEY] ?: false // Default to light mode (false)
        }
            .catch { exception ->
                // Handle potential exceptions during DataStore read
                // Log the error and emit false as a fallback
                // android.util.Log.e("ThemePreference", "Error reading dark mode preference", exception)
                emit(false)
            }

    /**
     * Sets the dark mode preference.
     */
    suspend fun setDarkMode(context: Context, enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[DARK_MODE_KEY] = enabled
        }
    }
}