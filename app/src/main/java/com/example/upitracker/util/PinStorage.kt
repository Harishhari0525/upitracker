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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.google.protobuf.ByteString
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

private val Context.pinDataStore: DataStore<UserPreferences> by dataStore(
    fileName = "secure_user_prefs.pb",
    serializer = SecureUserPreferencesSerializer(aeadProvider = { CryptoManager.getAead() })
)

object PinStorage {

    private const val MAX_FAILED_ATTEMPTS = 5
    private const val LOCKOUT_DURATION_MILLIS = 30_000L
    private const val PIN_HASH_ITERATIONS = 120_000
    private const val PIN_HASH_BITS = 256
    private const val PIN_SALT_BYTES = 16

    /**
     * Saves the user's PIN securely. This is a suspend function.
     */
    suspend fun savePin(context: Context, pin: String) {
        val salt = ByteArray(PIN_SALT_BYTES).also(SecureRandom()::nextBytes)
        val hash = hashPin(pin, salt)
        try {
            context.pinDataStore.updateData { preferences ->
                preferences.toBuilder()
                    .clearPin()
                    .setPinSalt(ByteString.copyFrom(salt))
                    .setPinHash(ByteString.copyFrom(hash))
                    .setFailedPinAttempts(0)
                    .setPinLockoutUntil(0)
                    .build()
            }
        } finally {
            salt.fill(0)
            hash.fill(0)
        }
    }

    /**
     * Retrieves the saved PIN. Returns null if no PIN is set.
     */
    /**
     * Verifies if the provided PIN matches the stored PIN.
     */
    suspend fun checkPin(context: Context, providedPin: String): Boolean {
        val now = System.currentTimeMillis()
        var verified = false
        context.pinDataStore.updateData { preferences ->
            if (preferences.pinLockoutUntil > now) {
                return@updateData preferences
            }

            val storedHash = preferences.pinHash.toByteArray()
            val storedSalt = preferences.pinSalt.toByteArray()
            val verifiedByHash = if (storedHash.isNotEmpty() && storedSalt.isNotEmpty()) {
                val candidate = hashPin(providedPin, storedSalt)
                try {
                    MessageDigest.isEqual(storedHash, candidate)
                } finally {
                    candidate.fill(0)
                    storedHash.fill(0)
                    storedSalt.fill(0)
                }
            } else {
                false
            }
            val verifiedLegacyPin =
                storedHash.isEmpty() && preferences.pin.isNotEmpty() && preferences.pin == providedPin

            if (verifiedByHash || verifiedLegacyPin) {
                verified = true
                val builder = preferences.toBuilder()
                    .setFailedPinAttempts(0)
                    .setPinLockoutUntil(0)
                if (verifiedLegacyPin) {
                    val salt = ByteArray(PIN_SALT_BYTES).also(SecureRandom()::nextBytes)
                    val hash = hashPin(providedPin, salt)
                    builder.clearPin()
                        .setPinSalt(ByteString.copyFrom(salt))
                        .setPinHash(ByteString.copyFrom(hash))
                    salt.fill(0)
                    hash.fill(0)
                }
                builder.build()
            } else {
                val failedAttempts = preferences.failedPinAttempts + 1
                preferences.toBuilder()
                    .setFailedPinAttempts(if (failedAttempts >= MAX_FAILED_ATTEMPTS) 0 else failedAttempts)
                    .setPinLockoutUntil(
                        if (failedAttempts >= MAX_FAILED_ATTEMPTS) now + LOCKOUT_DURATION_MILLIS else 0
                    )
                    .build()
            }
        }
        return verified
    }

    /**
     * Helper to verify if a PIN exists. Safely handles reading errors.
     */
    suspend fun isPinSet(context: Context): Boolean {
        val preferences = context.pinDataStore.data.first()
        return preferences.pinHash.size() > 0 || preferences.pin.isNotEmpty()
    }

    /**
     * Retrieves the lockout timestamp.
     */
    suspend fun getPinLockoutUntil(context: Context): Long {
        return try {
            context.pinDataStore.data.first().pinLockoutUntil
        } catch (_: Exception) {
            0L
        }
    }

    private fun hashPin(pin: String, salt: ByteArray): ByteArray {
        val pinChars = pin.toCharArray()
        val spec = PBEKeySpec(pinChars, salt, PIN_HASH_ITERATIONS, PIN_HASH_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec)
                .encoded
        } finally {
            pinChars.fill('\u0000')
            spec.clearPassword()
        }
    }
}

// Serializer for the UserPreferences protobuf with dynamic, deferred encryption initialization
private class SecureUserPreferencesSerializer(
    private val aeadProvider: () -> Aead
) : Serializer<UserPreferences> {

    override val defaultValue: UserPreferences = UserPreferences.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): UserPreferences {
        try {
            val encryptedInput = input.readBytes()
            val decryptedInput = if (encryptedInput.isNotEmpty()) {
                // Fetch Aead lazily on actual data transaction
                aeadProvider().decrypt(encryptedInput, null)
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
        // Fetch Aead lazily on actual data transaction
        val encryptedBytes = aeadProvider().encrypt(byteArray, null)
        withContext(Dispatchers.IO) {
            output.write(encryptedBytes)
        }
    }
}
