package com.example.aptiready.data.repository

import android.content.Context
import android.util.Log
import com.example.aptiready.data.model.AuthState
import com.example.aptiready.data.remote.FirebaseConfigManager
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FirebaseAuthRepository(
    private val context: Context,
    private val profileRepository: ProfileRepository? = null
) : AuthRepository {

    override val isFirebaseConfigured: Boolean = FirebaseConfigManager.isConfigured(context)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Initializing)
    override val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private var firebaseAuth: FirebaseAuth? = null
    private var authListener: FirebaseAuth.AuthStateListener? = null
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    init {
        if (isFirebaseConfigured) {
            try {
                firebaseAuth = FirebaseAuth.getInstance()
                val projectId = FirebaseApp.getInstance().options.projectId
                Log.d("FirebaseAuthRepo", "Initialized FirebaseAuth for project: $projectId")
                setupAuthListener()
            } catch (e: Exception) {
                Log.e("FirebaseAuthRepo", "FirebaseAuth initialization failed: ${e.message}")
                _authState.value = AuthState.SignedOut
            }
        } else {
            _authState.value = AuthState.SignedOut
        }
    }

    private fun setupAuthListener() {
        val auth = firebaseAuth ?: return
        authListener = FirebaseAuth.AuthStateListener { authInstance ->
            val user = authInstance.currentUser
            if (user == null) {
                _authState.value = AuthState.SignedOut
                profileRepository?.clearProfileState()
            } else {
                repositoryScope.launch {
                    try {
                        user.reload().await()
                        val email = user.email ?: ""
                        if (user.isEmailVerified) {
                            val tokenResult = user.getIdToken(true).await()
                            val verifiedClaim = (tokenResult.claims["email_verified"] as? Boolean) == true
                            Log.d("FirebaseAuthRepo", "AuthListener UID: ${user.uid}, isEmailVerified: ${user.isEmailVerified}, claim: $verifiedClaim")
                            if (verifiedClaim) {
                                _authState.value = AuthState.SignedInVerified(user.uid, email)
                            } else {
                                _authState.value = AuthState.SignedInUnverified(email)
                            }
                        } else {
                            _authState.value = AuthState.SignedInUnverified(email)
                        }
                    } catch (e: Exception) {
                        Log.w("FirebaseAuthRepo", "AuthListener token refresh failed: ${e.message}")
                        val email = user.email ?: ""
                        _authState.value = AuthState.SignedInUnverified(email)
                    }
                }
            }
        }
        auth.addAuthStateListener(authListener!!)
    }

    override val currentUserId: String?
        get() = if (isFirebaseConfigured) firebaseAuth?.currentUser?.uid else null

    override val currentUserEmail: String?
        get() = if (isFirebaseConfigured) firebaseAuth?.currentUser?.email else null

    override suspend fun registerWithEmail(displayName: String, email: String, password: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isFirebaseConfigured) {
            return@withContext Result.failure(IllegalStateException(FirebaseConfigManager.getConfigurationMessage()))
        }
        val cleanEmail = email.trim()
        val cleanName = displayName.trim()

        if (cleanName.length !in 1..50) {
            return@withContext Result.failure(IllegalArgumentException("Display name must be between 1 and 50 characters."))
        }
        if (password.length < 6) {
            return@withContext Result.failure(IllegalArgumentException("Password must be at least 6 characters long."))
        }

        try {
            val authResult = firebaseAuth!!.createUserWithEmailAndPassword(cleanEmail, password).await()
            val user = authResult.user ?: throw IllegalStateException("Registration succeeded but no user returned.")
            
            user.sendEmailVerification().await()
            _authState.value = AuthState.SignedInUnverified(cleanEmail)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapAuthException(e))
        }
    }

    override suspend fun loginWithEmail(
        email: String,
        password: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {

        if (!isFirebaseConfigured) {
            return@withContext Result.failure(
                IllegalStateException(
                    FirebaseConfigManager.getConfigurationMessage()
                )
            )
        }

        val cleanEmail = email.trim()

        try {
            val authResult = firebaseAuth!!
                .signInWithEmailAndPassword(cleanEmail, password)
                .await()

            val user = authResult.user
                ?: throw IllegalStateException("Sign in failed.")

            // Refresh Firebase user data so email verification status is current.
            user.reload().await()

            val verified = user.isEmailVerified
            val userEmail = user.email ?: cleanEmail

            Log.d(
                "FirebaseAuthRepo",
                "Login UID=${user.uid}, email=$userEmail, emailVerified=$verified"
            )

            if (verified) {
                _authState.value = AuthState.SignedInVerified(
                    user.uid,
                    userEmail
                )
            } else {
                _authState.value = AuthState.SignedInUnverified(
                    userEmail
                )
            }

            // Return the verification result to LoginViewModel.
            Result.success(verified)

        } catch (e: Exception) {
            Result.failure(mapAuthException(e))
        }
    }

    override suspend fun sendEmailVerification(): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isFirebaseConfigured) return@withContext Result.failure(IllegalStateException("Firebase is not configured."))
        val user = firebaseAuth?.currentUser ?: return@withContext Result.failure(IllegalStateException("No signed in user."))

        try {
            user.sendEmailVerification().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapAuthException(e))
        }
    }

    override suspend fun reloadUserAndCheckVerification(): Result<Boolean> = withContext(Dispatchers.IO) {
        if (!isFirebaseConfigured) return@withContext Result.failure(IllegalStateException("Firebase is not configured."))
        val user = firebaseAuth?.currentUser ?: return@withContext Result.failure(IllegalStateException("No signed in user."))

        try {
            user.reload().await()
            val tokenResult = user.getIdToken(true).await()
            val verifiedClaim = (tokenResult.claims["email_verified"] as? Boolean) == true
            val verified = user.isEmailVerified && verifiedClaim
            Log.d("FirebaseAuthRepo", "reloadUserAndCheckVerification - UID: ${user.uid}, isEmailVerified: ${user.isEmailVerified}, claim: $verifiedClaim")

            if (verified) {
                _authState.value = AuthState.SignedInVerified(user.uid, user.email ?: "")
            } else {
                _authState.value = AuthState.SignedInUnverified(user.email ?: "")
            }
            Result.success(verified)
        } catch (e: Exception) {
            Result.failure(mapAuthException(e))
        }
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isFirebaseConfigured) {
            return@withContext Result.failure(IllegalStateException(FirebaseConfigManager.getConfigurationMessage()))
        }
        val cleanEmail = email.trim()

        try {
            firebaseAuth!!.sendPasswordResetEmail(cleanEmail).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.success(Unit)
        }
    }

    override fun signOut() {
        if (isFirebaseConfigured) {
            firebaseAuth?.signOut()
        }
        profileRepository?.clearProfileState()
        _authState.value = AuthState.SignedOut
    }

    private fun mapAuthException(e: Exception): Throwable {
        return when (e) {
            is FirebaseAuthInvalidUserException -> Exception("Account not found or disabled. Please check your email or register.")
            is FirebaseAuthInvalidCredentialsException -> Exception("Invalid password or credentials. Please try again.")
            is FirebaseAuthWeakPasswordException -> Exception("Password is too weak. Please use at least 6 characters.")
            is FirebaseAuthUserCollisionException -> Exception("An account already exists with this email address.")
            else -> Exception(e.localizedMessage ?: "An unexpected authentication error occurred.")
        }
    }
}