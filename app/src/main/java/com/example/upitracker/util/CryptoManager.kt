package com.example.upitracker.util

import android.content.Context
import com.google.crypto.tink.Aead
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.aead.AesGcmKeyManager
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyStore
import androidx.core.content.edit

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
                        // ✨ RECOVERY ENGINE: Intercepts unreadable/corrupted hardware references on clean install
                        when (e) {
                            is GeneralSecurityException -> {
                                try {
                                    // 1. Force-flush the stale hardware alias from the Android System KeyStore daemon
                                    val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
                                    if (keyStore.containsAlias("master_key")) {
                                        keyStore.deleteEntry("master_key")
                                    }

                                    // 2. Clear out the corrupted internal XML tracking preference file tags
                                    context.getSharedPreferences(PREFERENCE_FILE, Context.MODE_PRIVATE)
                                        .edit(commit = true) {
                                            clear()
                                        }

                                    // 3. Re-attempt a clean key generation pass from a fresh slate
                                    _aead = initAeadPrimitive(context)
                                } catch (retryException: Exception) {
                                    throw GeneralSecurityException(
                                        "Fatal failure during crypto provider recovery loop",
                                        retryException
                                    )
                                }
                            }

                            is IOException -> {
                                throw IOException("Failed to initialize CryptoManager due to file I/O error", e)
                            }

                            else -> {
                                throw e
                            }
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

        return keysetHandle.getPrimitive(Aead::class.java)
    }

    fun getAead(): Aead {
        return _aead ?: throw IllegalStateException("CryptoManager has not been initialized. Call initialize() first.")
    }
}