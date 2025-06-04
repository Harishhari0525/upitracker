package com.example.upitracker.util

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// Define the DataStore instance at the top level, associated with the Context
val Context.pinDataStore by preferencesDataStore(name = "pin_store")

object PinStorage {
    // Define the key for the PIN preference
    private val PIN_KEY = stringPreferencesKey("user_pin")

    /**
     * Saves the user's PIN securely.
     * Consider encrypting the PIN before saving for enhanced security,
     * though DataStore itself is stored in app-private storage.
     */
    suspend fun savePin(context: Context, pin: String) {
        // Basic validation could be added here if not handled elsewhere (e.g., pin length, format)
        context.pinDataStore.edit { prefs ->
            prefs[PIN_KEY] = pin // Store the PIN
        }
    }

    /**
     * Retrieves the saved PIN. Returns null if no PIN is set.
     */
    suspend fun getPin(context: Context): String? {
        return try {
            context.pinDataStore.data
                .map { preferences ->
                    preferences[PIN_KEY]
                }
                .first() // Get the first emitted value (current state)
        } catch (e: Exception) {
            // Handle potential exceptions during DataStore read (e.g., IOException)
            // Log the error or return null
            // android.util.Log.e("PinStorage", "Error reading PIN from DataStore", e)
            null
        }
    }

    /**
     * Checks if a PIN has been set by the user.
     */
    suspend fun isPinSet(context: Context): Boolean {
        return getPin(context) != null
    }

    /**
     * Verifies if the provided PIN matches the stored PIN.
     */
    suspend fun checkPin(context: Context, providedPin: String): Boolean {
        val storedPin = getPin(context)
        return storedPin != null && storedPin == providedPin
    }

    /**
     * Clears the stored PIN.
     */
    suspend fun clearPin(context: Context) {
        context.pinDataStore.edit { prefs ->
            prefs.remove(PIN_KEY)
        }
    }
}