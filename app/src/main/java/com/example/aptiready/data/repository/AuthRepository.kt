package com.example.aptiready.data.repository

import com.example.aptiready.data.model.AuthState
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val authState: StateFlow<AuthState>
    val isFirebaseConfigured: Boolean
    val currentUserId: String?
    val currentUserEmail: String?

    suspend fun signInWithGoogle(idToken: String): Result<Unit>
    fun signOut()
}