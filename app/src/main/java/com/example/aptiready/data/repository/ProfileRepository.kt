package com.example.aptiready.data.repository

import com.example.aptiready.data.model.ProfileState
import com.example.aptiready.data.model.UserProfile
import kotlinx.coroutines.flow.StateFlow

interface ProfileRepository {
    val profileState: StateFlow<ProfileState>

    suspend fun loadProfile(uid: String): Result<UserProfile?>
    suspend fun ensureProfileExists(uid: String, displayName: String): Result<UserProfile>
    suspend fun updateProfile(uid: String, displayName: String, dailyGoal: Int): Result<Unit>
    fun clearProfileState()
}