package com.example.aptiready.data.remote

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
        return "Firebase setup required: Please place your downloaded google-services.json file into the 'app/' directory as documented in FIREBASE_SETUP.md to enable Cloud Authentication and Firestore Profile Sync."
    }
}