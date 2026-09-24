package com.example.aptiready.data.model

sealed class AuthState {
    object Initializing : AuthState()
    object LocalGuest : AuthState()
    object SignedOut : AuthState()
    data class SignedInUnverified(val email: String) : AuthState()
    data class SignedInVerified(val uid: String, val email: String) : AuthState()
}