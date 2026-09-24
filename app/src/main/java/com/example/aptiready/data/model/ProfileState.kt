package com.example.aptiready.data.model

sealed class ProfileState {
    object Loading : ProfileState()
    data class Success(val profile: UserProfile) : ProfileState()
    object NotFound : ProfileState()
    data class Error(val message: String, val isRecoverable: Boolean = true) : ProfileState()
}