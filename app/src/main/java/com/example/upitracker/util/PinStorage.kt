package com.example.upitracker.util

import android.content.Context
import com.example.upitracker.data.UserPreferences
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream
import com.google.crypto.tink.Aead
import kotlinx.coroutines.flow.first
import androidx.datastore.core.DataStore

// Define the DataStore using the dataStore delegate
private val Context.pinDataStore: DataStore<UserPreferences> by dataStore( // ✨ ADD THIS TYPE DEFINITION
    fileName = "secure_user_prefs.pb", // Using .pb for protobuf
    serializer = SecureUserPreferencesSerializer(CryptoManager.getAead())
)

object PinStorage {

    /**
     * Saves the user's PIN securely. This is a suspend function.
     */
    suspend fun savePin(context: Context, pin: String) {
        context.pinDataStore.updateData { preferences ->
            preferences.toBuilder().setPin(pin).build()
        }
    }

    /**
     * Retrieves the saved PIN. Returns null if no PIN is set.
     */
    suspend fun getPin(context: Context): String? {
        val preferences = context.pinDataStore.data.first()
        // For proto3, we check if the string is not empty instead of using hasPin()
        return preferences.pin.ifEmpty {
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
        context.pinDataStore.updateData { preferences ->
            preferences.toBuilder().clearPin().build()
        }
    }
}

// Serializer for the UserPreferences protobuf with encryption
private class SecureUserPreferencesSerializer(private val aead: Aead) : Serializer<UserPreferences> {
    override val defaultValue: UserPreferences = UserPreferences.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): UserPreferences {
        try {
            val encryptedInput = input.readBytes()
            val decryptedInput = if (encryptedInput.isNotEmpty()) {
                aead.decrypt(encryptedInput, null)
            } else {
                ByteArray(0)
            }
            return UserPreferences.parseFrom(decryptedInput)
        } catch (e: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", e)
        }
    }

    override suspend fun writeTo(t: UserPreferences, output: OutputStream) {
        val byteArray = t.toByteArray()
        val encryptedBytes = aead.encrypt(byteArray, null)
        output.write(encryptedBytes)
    }
}