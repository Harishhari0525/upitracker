package com.example.upitracker.util

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability

class InAppUpdateManager(private val context: Context) {

    private val appUpdateManager = AppUpdateManagerFactory.create(context)

    fun checkForUpdate() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            // Check if an update is available and if the flexible flow is allowed.
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {

                // An update is available. Start the flexible update flow.
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    context as Activity,
                    AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build(),
                    UPDATE_REQUEST_CODE
                )
                Log.d("InAppUpdateManager", "Flexible update flow started.")
            } else {
                Log.d("InAppUpdateManager", "No update available or flow not allowed.")
            }
        }.addOnFailureListener { e ->
            Log.e("InAppUpdateManager", "Failed to check for update.", e)
        }
    }

    companion object {
        const val UPDATE_REQUEST_CODE = 123
    }
}