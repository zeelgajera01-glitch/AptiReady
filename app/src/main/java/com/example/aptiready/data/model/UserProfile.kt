package com.example.aptiready.data.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class UserProfile(
    val uid: String = "",
    val displayName: String = "",
    val dailyGoal: Int = 10,
    @get:ServerTimestamp
    val createdAt: Date? = null,
    @get:ServerTimestamp
    val updatedAt: Date? = null
) {
    fun isValid(): Boolean {
        return displayName.trim().length in 1..50 && dailyGoal in 5..100
    }
}