package com.example.upitracker.util

import android.content.Context
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometricHelper {

    const val TAG = "BiometricHelper"

    /**
     * Checks if strong biometric authentication can be performed.
     * Returns a BiometricAvailability an Int code from BiometricManager (e.g., BIOMETRIC_SUCCESS).
     */
    fun getBiometricAvailability(context: Context): Int {
        val biometricManager = BiometricManager.from(context)
        // We prefer strong biometrics.
        // You could also allow BIOMETRIC_WEAK or DEVICE_CREDENTIAL depending on your security needs.
        return biometricManager.canAuthenticate(BIOMETRIC_STRONG)
    }

    /**
     * Convenience function to check if biometrics are available and enrolled.
     */
    fun isBiometricReady(context: Context): Boolean {
        return getBiometricAvailability(context) == BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Creates and returns a BiometricPrompt instance.
     * The actual authentication is launched by calling biometricPrompt.authenticate(promptInfo).
     *
     * @param activity The FragmentActivity that will host the prompt.
     * @param onAuthenticationSucceeded Lambda to execute on successful authentication.
     * @param onAuthenticationError Lambda to execute on authentication error (unrecoverable).
     * @param onAuthenticationFailed Lambda to execute on authentication failure (recoverable, e.g., wrong fingerprint).
     * @return Configured BiometricPrompt instance.
     */
    fun getBiometricPrompt(
        activity: FragmentActivity,
        onAuthenticationSucceeded: () -> Unit,
        onAuthenticationError: (Int, CharSequence) -> Unit,
        onAuthenticationFailed: () -> Unit
    ): BiometricPrompt {
        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                Log.i(TAG, "Biometric authentication succeeded.")
                onAuthenticationSucceeded()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                Log.e(TAG, "Biometric authentication error: $errorCode - $errString")
                // Examples:
                // BiometricPrompt.ERROR_HW_UNAVAILABLE, ERROR_NO_BIOMETRICS, ERROR_USER_CANCELED
                // ERROR_LOCKOUT (too many attempts), ERROR_TIMEOUT
                onAuthenticationError(errorCode, errString)
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Log.w(TAG, "Biometric authentication failed (e.g., unrecognized fingerprint).")
                onAuthenticationFailed()
            }
        }
        return BiometricPrompt(activity, executor, callback)
    }

    /**
     * Creates the PromptInfo for the BiometricPrompt.
     *
     * @param title The title for the prompt.
     * @param subtitle An optional subtitle.
     * @param negativeButtonText Text for the negative/cancel button (e.g., "Cancel" or "Use PIN").
     * If using "Use PIN", the onClick for negative button should handle that.
     * @param requireConfirmation Set to false if you want implicit authentication (e.g. face unlock completes immediately). Default is true.
     * @return Configured BiometricPrompt.PromptInfo instance.
     */
    fun getPromptInfo(
        title: String,
        subtitle: String? = null,
        negativeButtonText: String, // User must provide this, as it could be "Cancel" or "Use App PIN"
        requireConfirmation: Boolean = true
    ): BiometricPrompt.PromptInfo {
        val builder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setAllowedAuthenticators(BIOMETRIC_STRONG) // Enforce strong biometrics
            .setConfirmationRequired(requireConfirmation) // Usually true for fingerprint

        subtitle?.let { builder.setSubtitle(it) }
        // If negativeButtonText is "Use App PIN", clicking it should let the user enter the app's PIN.
        // If it's just "Cancel", it dismisses the prompt.
        // The BiometricPrompt.ERROR_NEGATIVE_BUTTON or ERROR_USER_CANCELED will be called in onAuthenticationError.
        builder.setNegativeButtonText(negativeButtonText)

        return builder.build()
    }
}