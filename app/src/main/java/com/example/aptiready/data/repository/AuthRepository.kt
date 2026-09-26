package com.example.aptiready.data.repository

import com.example.aptiready.data.model.AuthState
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val authState: StateFlow<AuthState>
    val isFirebaseConfigured: Boolean
    val currentUserId: String?
    val currentUserEmail: String?

    suspend fun registerWithEmail(displayName: String, email: String, password: String): Result<Unit>
    suspend fun loginWithEmail(email: String, password: String): Result<Boolean>
    suspend fun sendEmailVerification(): Result<Unit>
    suspend fun reloadUserAndCheckVerification(): Result<Boolean>
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>
    fun signOut()
}