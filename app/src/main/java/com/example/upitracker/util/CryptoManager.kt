package com.example.upitracker.util

import android.content.Context
import com.google.crypto.tink.Aead
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.AesGcmKeyManager
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import java.io.IOException
import java.security.GeneralSecurityException

object CryptoManager {
    private const val KEYSET_NAME = "master_keyset"
    private const val PREFERENCE_FILE = "master_key_preference"
    private const val MASTER_KEY_URI = "android-keystore://master_key"

    @Volatile
    private var _aead: Aead? = null

    fun initialize(context: Context) {
        if (_aead == null) {
            synchronized(this) {
                if (_aead == null) {
                    try {
                        AeadConfig.register()
                        _aead = initAeadPrimitive(context)
                    } catch (e: Exception) {
                        // Never delete an existing keyset automatically. That would permanently
                        // destroy access to encrypted preferences and device-bound backups.
                        throw when (e) {
                            is GeneralSecurityException -> e
                            is IOException -> e
                            else -> GeneralSecurityException("Failed to initialize CryptoManager", e)
                        }
                    }
                }
            }
        }
    }

    /**
     * Internal factory abstraction helper that handles keyset instantiation cleanly using
     * non-deprecated modern Tink casting implementations.
     */
    @Throws(GeneralSecurityException::class, IOException::class)
    private fun initAeadPrimitive(context: Context): Aead {
        val keysetHandle = AndroidKeysetManager.Builder()
            .withSharedPref(context, KEYSET_NAME, PREFERENCE_FILE)
            .withKeyTemplate(AesGcmKeyManager.aes256GcmTemplate())
            .withMasterKeyUri(MASTER_KEY_URI)
            .build()
            .keysetHandle

        return keysetHandle.getPrimitive(RegistryConfiguration.get(), Aead::class.java)
    }

    fun getAead(): Aead {
        return _aead ?: throw IllegalStateException("CryptoManager has not been initialized. Call initialize() first.")
    }
}
