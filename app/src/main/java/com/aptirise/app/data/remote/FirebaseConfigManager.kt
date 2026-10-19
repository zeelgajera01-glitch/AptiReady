package com.aptirise.app.data.remote

import android.content.Context
import com.google.firebase.FirebaseApp

object FirebaseConfigManager {

    fun isConfigured(context: Context): Boolean {
        return try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                val app = FirebaseApp.initializeApp(context)
                app != null
            } else {
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    fun getConfigurationMessage(): String {
        return "Update configuration is unavailable."
    }
}