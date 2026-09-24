package com.example.aptiready.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.syncDataStore: DataStore<Preferences> by preferencesDataStore(name = "aptirise_sync_prefs")

class SyncPreferencesRepository(private val context: Context) {

    fun isBackupEnabled(uid: String): Flow<Boolean> {
        val key = booleanPreferencesKey("cloud_backup_enabled_${uid.ifEmpty { "default" }}")
        return context.syncDataStore.data.map { prefs ->
            prefs[key] ?: false
        }
    }

    fun lastSuccessfulSyncTime(uid: String): Flow<Long> {
        val key = longPreferencesKey("last_successful_sync_time_${uid.ifEmpty { "default" }}")
        return context.syncDataStore.data.map { prefs ->
            prefs[key] ?: 0L
        }
    }

    fun lastSyncError(uid: String): Flow<String?> {
        val key = stringPreferencesKey("last_sync_error_${uid.ifEmpty { "default" }}")
        return context.syncDataStore.data.map { prefs ->
            prefs[key]
        }
    }

    suspend fun setBackupEnabled(uid: String, enabled: Boolean) {
        val key = booleanPreferencesKey("cloud_backup_enabled_${uid.ifEmpty { "default" }}")
        context.syncDataStore.edit { prefs ->
            prefs[key] = enabled
        }
    }

    suspend fun recordSuccessfulSync(uid: String, timestamp: Long) {
        val keyTime = longPreferencesKey("last_successful_sync_time_${uid.ifEmpty { "default" }}")
        val keyErr = stringPreferencesKey("last_sync_error_${uid.ifEmpty { "default" }}")
        context.syncDataStore.edit { prefs ->
            prefs[keyTime] = timestamp
            prefs.remove(keyErr)
        }
    }

    suspend fun recordSyncError(uid: String, errorMessage: String) {
        val keyErr = stringPreferencesKey("last_sync_error_${uid.ifEmpty { "default" }}")
        context.syncDataStore.edit { prefs ->
            prefs[keyErr] = errorMessage
        }
    }
}