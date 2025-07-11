package com.example.upitracker.util

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

// Define the DataStore instance at the top level, associated with the Context.
// The name "settings" is generic; ensure it doesn't clash if you add more general settings.
// If this DataStore is only for theme, "theme_settings" might be more specific.
val Context.settingsDataStore by preferencesDataStore(name = "app_settings") // Renamed for clarity if it holds more than just theme

enum class AppTheme(val displayName: String) {
    DEFAULT("Default"),
    FOREST("Forest"),
    OCEAN("Ocean"),
    ROSE("Rose"),
    LAVENDER("Lavender"),
    SUNSET("Sunset"),
    MINT("Mint")
}

object ThemePreference {
    private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode_enabled") // Slightly more descriptive key name
    private val UPI_LITE_ENABLED_KEY = booleanPreferencesKey("upi_lite_enabled")
    private val APP_THEME_KEY = stringPreferencesKey("app_theme")

    private val REFUND_KEYWORD_KEY = stringPreferencesKey("refund_keyword")

    private val LAST_SEEN_VERSION_KEY = stringPreferencesKey("last_seen_version")

    /**
     * Retrieves the Flow for the dark mode preference.
     * Emits false (light mode) if not set or if there's an error.
     */
    fun isDarkModeFlow(context: Context): Flow<Boolean> =
        context.settingsDataStore.data.map { prefs: Preferences ->
            prefs[DARK_MODE_KEY] == true // Default to light mode (false)
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

    fun isUpiLiteEnabledFlow(context: Context): Flow<Boolean> =
        context.settingsDataStore.data.map { prefs ->
            // Default to true, so existing users still see it.
            prefs[UPI_LITE_ENABLED_KEY] != false
        }

    suspend fun setUpiLiteEnabled(context: Context, enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[UPI_LITE_ENABLED_KEY] = enabled
        }
    }

    fun getAppThemeFlow(context: Context): Flow<AppTheme> =
        context.settingsDataStore.data.map { prefs ->
            // Default to AppTheme.DEFAULT if nothing is set
            AppTheme.valueOf(prefs[APP_THEME_KEY] ?: AppTheme.DEFAULT.name)
        }

    suspend fun setAppTheme(context: Context, theme: AppTheme) {
        context.settingsDataStore.edit { prefs ->
            prefs[APP_THEME_KEY] = theme.name
        }
    }

    fun getRefundKeywordFlow(context: Context): Flow<String> =
        context.settingsDataStore.data.map { prefs ->
            // Default to "Refund" if the user hasn't set anything
            prefs[REFUND_KEYWORD_KEY] ?: "Refund"
        }

    suspend fun setRefundKeyword(context: Context, keyword: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[REFUND_KEYWORD_KEY] = keyword
        }
    }

    fun getLastSeenVersionFlow(context: Context): Flow<String> =
        context.settingsDataStore.data.map { prefs ->
            prefs[LAST_SEEN_VERSION_KEY] ?: ""
        }

    suspend fun setLastSeenVersion(context: Context, versionName: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[LAST_SEEN_VERSION_KEY] = versionName
        }
    }

}