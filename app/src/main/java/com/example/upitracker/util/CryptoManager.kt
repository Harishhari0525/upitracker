package com.example.upitracker.util

import android.content.Context
import com.google.crypto.tink.Aead
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.AesGcmKeyManager
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import java.io.IOException
import java.security.GeneralSecurityException

object CryptoManager {
    private const val KEYSET_NAME = "master_keyset"
    private const val PREFERENCE_FILE = "master_key_preference"
    private const val MASTER_KEY_URI = "android-keystore://master_key"

    @Volatile
    private var _aead: Aead? = null

    // Initialize the Aead primitive
    // This should be done once, for example, in your Application's onCreate
    fun initialize(context: Context) {
        if (_aead == null) {
            synchronized(this) {
                if (_aead == null) {
                    try {
                        AeadConfig.register()
                        _aead = AndroidKeysetManager.Builder()
                            .withSharedPref(context, KEYSET_NAME, PREFERENCE_FILE)
                            .withKeyTemplate(AesGcmKeyManager.aes256GcmTemplate())
                            .withMasterKeyUri(MASTER_KEY_URI)
                            .build()
                            .keysetHandle
                            .getPrimitive(Aead::class.java)
                    } catch (e: GeneralSecurityException) {
                        throw GeneralSecurityException("Failed to initialize CryptoManager", e)
                    } catch (e: IOException) {
                        throw IOException("Failed to initialize CryptoManager", e)
                    }
                }
            }
        }
    }

    fun getAead(): Aead {
        return _aead ?: throw IllegalStateException("CryptoManager has not been initialized. Call initialize() first.")
    }
}