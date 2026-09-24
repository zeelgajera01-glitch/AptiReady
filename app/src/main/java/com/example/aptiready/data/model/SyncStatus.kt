package com.example.aptiready.data.model

enum class SyncStatus {
    INITIALIZING,
    OFF,
    WAITING_FOR_VERIFICATION,
    PENDING,
    SYNCING,
    UP_TO_DATE,
    OFFLINE,
    ERROR,
    DELETION_PENDING;

    fun getDisplayName(): String {
        return when (this) {
            INITIALIZING -> "Checking backup status..."
            OFF -> "Cloud Backup Off"
            WAITING_FOR_VERIFICATION -> "Waiting for Email Verification"
            PENDING -> "Pending Changes Queued"
            SYNCING -> "Syncing Cloud Records..."
            UP_TO_DATE -> "Up to Date"
            OFFLINE -> "Offline (Local Only)"
            ERROR -> "Sync Error"
            DELETION_PENDING -> "Account Deletion Pending"
        }
    }
}