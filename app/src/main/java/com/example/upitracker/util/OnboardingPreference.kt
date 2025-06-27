package com.example.upitracker.util

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

// Define the DataStore instance for onboarding preferences
val Context.onboardingDataStore by preferencesDataStore(name = "onboarding_prefs")

object OnboardingPreference {
    private val ONBOARDING_COMPLETED_KEY = booleanPreferencesKey("onboarding_completed")

    private val TOUR_COMPLETED_KEY = booleanPreferencesKey("tour_completed")
    fun isOnboardingCompletedFlow(context: Context): Flow<Boolean> =
        context.onboardingDataStore.data
            .catch { exception ->
                // Handle potential exceptions during DataStore read, e.g., IOException
                if (exception is IOException) {
                    // android.util.Log.e("OnboardingPreference", "Error reading onboarding preference", exception)
                    emit(androidx.datastore.preferences.core.emptyPreferences()) // Emit empty preferences on error
                } else {
                    throw exception // Rethrow other exceptions
                }
            }
            .map { preferences ->
                preferences[ONBOARDING_COMPLETED_KEY] ?: false // Default to false if key doesn't exist
            }

    /**
     * Sets the onboarding completion status.
     */
    suspend fun setOnboardingCompleted(context: Context, completed: Boolean) {
        context.onboardingDataStore.edit { preferences ->
            preferences[ONBOARDING_COMPLETED_KEY] = completed
        }
    }
    fun isTourCompletedFlow(context: Context): Flow<Boolean> =
        context.onboardingDataStore.data
            .map { preferences ->
                preferences[TOUR_COMPLETED_KEY] ?: false
            }

    suspend fun setTourCompleted(context: Context, completed: Boolean) {
        context.onboardingDataStore.edit { preferences ->
            preferences[TOUR_COMPLETED_KEY] = completed
        }
    }

}