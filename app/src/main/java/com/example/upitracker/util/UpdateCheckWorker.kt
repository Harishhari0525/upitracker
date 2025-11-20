package com.example.upitracker.util

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.upitracker.network.UpdateService

class UpdateCheckWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val WORK_NAME = "UpdateCheckWorker"
    }

    override suspend fun doWork(): Result {
        Log.d(WORK_NAME, "Worker starting: Checking for new app version...")
        try {
            // Get the latest release info from GitHub
            val latestRelease = UpdateService.getLatestRelease() ?: return Result.success()

            // Get the current version of the installed app
            val currentVersionName = try {
                applicationContext.packageManager.getPackageInfo(applicationContext.packageName, 0).versionName
            } catch (e: PackageManager.NameNotFoundException) {
                Log.e(WORK_NAME, "Could not get current package version", e)
                return Result.failure()
            }

            // Compare versions (e.g., GitHub tag "v1.8" vs. app version "1.7")
            // This simple check removes the "v" prefix for comparison.
            val latestVersionTag = latestRelease.tagName.removePrefix("v")

            if (currentVersionName == null) {
                Log.e(WORK_NAME, "Current version name is null, cannot compare.")
                return Result.failure()
            }

            if (isNewerVersion(latestVersionTag, currentVersionName)) {
                Log.i(WORK_NAME, "New version found! GitHub: ${latestRelease.tagName}, Current: $currentVersionName. Showing notification.")
                // Use the NotificationHelper to show the update notification
                NotificationHelper.showUpdateAvailableNotification(applicationContext, latestRelease)
            } else {
                Log.d(WORK_NAME, "App is up to date. GitHub: ${latestRelease.tagName}, Current: $currentVersionName")
            }

            return Result.success()
        } catch (e: Exception) {
            Log.e(WORK_NAME, "Error during update check.", e)
            return Result.failure()
        }
    }

    // Helper function to compare version strings (e.g., "1.8" > "1.7")
    // Helper function to compare version strings (e.g., "1.10" > "1.9")
    private fun isNewerVersion(latestVersion: String, currentVersion: String): Boolean {
        val latestParts = latestVersion.split(".").map { it.toIntOrNull() ?: 0 }
        val currentParts = currentVersion.split(".").map { it.toIntOrNull() ?: 0 }

        val length = maxOf(latestParts.size, currentParts.size)

        for (i in 0 until length) {
            val latestPart = if (i < latestParts.size) latestParts[i] else 0
            val currentPart = if (i < currentParts.size) currentParts[i] else 0

            if (latestPart > currentPart) return true
            if (latestPart < currentPart) return false
        }
        return false
    }
}