package com.aptirise.app.data.remote

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.aptirise.app.BuildConfig
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings

class ForceUpdateManager(private val activity: Activity) {

    fun checkForUpdate() {
        val remoteConfig = FirebaseRemoteConfig.getInstance()
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(if (BuildConfig.DEBUG) 0L else 3600L)
            .build()

        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.setDefaultsAsync(
            mapOf(
                KEY_MIN_REQUIRED_VERSION to 1L,
                KEY_UPDATE_TITLE to "Update Required",
                KEY_UPDATE_MESSAGE to "A new version of AptiRise is available. Please update the app to continue.",
                KEY_UPDATE_URL to "https://play.google.com/store/apps/details?id=${activity.packageName}"
            )
        )

        remoteConfig.fetchAndActivate()
            .addOnCompleteListener(activity) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "RemoteConfig fetch succeeded")
                } else {
                    Log.w(TAG, "RemoteConfig fetch failed: ${task.exception?.message}")
                }
                checkVersionAndPrompt(remoteConfig)
            }
    }

    private fun checkVersionAndPrompt(remoteConfig: FirebaseRemoteConfig) {
        val currentVersionCode = BuildConfig.VERSION_CODE
        val minRequiredVersion = remoteConfig.getLong(KEY_MIN_REQUIRED_VERSION)

        Log.d(TAG, "Current VersionCode: $currentVersionCode, MinRequired: $minRequiredVersion")

        if (currentVersionCode < minRequiredVersion) {
            val title = remoteConfig.getString(KEY_UPDATE_TITLE).ifEmpty { "Update Required" }
            val message = remoteConfig.getString(KEY_UPDATE_MESSAGE).ifEmpty { "A new version of AptiRise is available. Please update to continue." }
            val playStoreUrl = remoteConfig.getString(KEY_UPDATE_URL).ifEmpty { "https://play.google.com/store/apps/details?id=${activity.packageName}" }

            showForceUpdateDialog(title, message, playStoreUrl)
        }
    }

    private fun showForceUpdateDialog(title: String, message: String, updateUrl: String) {
        if (activity.isFinishing || activity.isDestroyed) return

        MaterialAlertDialogBuilder(activity)
            .setTitle(title)
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("Update Now") { _, _ ->
                openPlayStore(updateUrl)
            }
            .setOnCancelListener {
                activity.finish()
            }
            .show()
            .apply {
                setCanceledOnTouchOutside(false)
            }
    }

    private fun openPlayStore(url: String) {
        val packageName = activity.packageName
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            activity.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            activity.startActivity(intent)
        }
    }

    companion object {
        private const val TAG = "ForceUpdateManager"
        private const val KEY_MIN_REQUIRED_VERSION = "min_required_version_code"
        private const val KEY_UPDATE_TITLE = "update_title"
        private const val KEY_UPDATE_MESSAGE = "update_message"
        private const val KEY_UPDATE_URL = "update_url"
    }
}