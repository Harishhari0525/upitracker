package com.example.upitracker.util

import android.content.Context
import androidx.biometric.BiometricManager
// import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
// import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL // Not recommended for app lock alone

object BiometricHelper {
    /**
     * Checks if strong biometric authentication is available and enrolled on the device.
     *
     * @param context Context to access BiometricManager.
     * @return True if strong biometrics are available, false otherwise.
     */
    fun isBiometricStrongAvailable(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true // App can authenticate using biometrics.
            // BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> false // No biometric features available on this device.
            // BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> false // Biometric features are currently unavailable.
            // BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> false // The user hasn't enrolled any biometrics.
            // BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> false
            // BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> false
            // BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> false
            else -> false // Other cases indicate unavailability or issues.
        }
    }

    // For actual biometric prompt, you would typically use androidx.biometric.BiometricPrompt
    // within an Activity or Fragment. That prompt itself has a system-styled UI.
    // Example (conceptual, actual implementation is more involved):
    // fun showBiometricPrompt(activity: FragmentActivity, onSuccess: () -> Unit, onError: (String) -> Unit) {
    //     val promptInfo = BiometricPrompt.PromptInfo.Builder()
    //         .setTitle("Biometric login for UPI Tracker")
    //         .setSubtitle("Log in using your biometric credential")
    //         .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
    //         // .setNegativeButtonText("Use App PIN") // Or handle cancellation
    //         .build()
    //
    //     val biometricPrompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity),
    //         object : BiometricPrompt.AuthenticationCallback() {
    //             override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
    //                 super.onAuthenticationSucceeded(result)
    //                 onSuccess()
    //             }
    //             override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
    //                 super.onAuthenticationError(errorCode, errString)
    //                 onError("Authentication error: $errString ($errorCode)")
    //             }
    //             override fun onAuthenticationFailed() {
    //                 super.onAuthenticationFailed()
    //                 onError("Authentication failed.")
    //             }
    //         })
    //     biometricPrompt.authenticate(promptInfo)
    // }
}