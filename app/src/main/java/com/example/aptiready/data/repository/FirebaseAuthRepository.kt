package com.example.aptiready.data.repository

import android.content.Context
import android.util.Log
import com.example.aptiready.data.model.AuthState
import com.example.aptiready.data.remote.FirebaseConfigManager
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
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
                        _authState.value = AuthState.SignedInVerified(user.uid, email)
                    } catch (e: Exception) {
                        Log.w("FirebaseAuthRepo", "AuthListener refresh failed: ${e.message}")
                        val email = user.email ?: ""
                        _authState.value = AuthState.SignedInVerified(user.uid, email)
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

    override suspend fun signInWithGoogle(idToken: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isFirebaseConfigured) {
            return@withContext Result.failure(IllegalStateException(FirebaseConfigManager.getConfigurationMessage()))
        }

        try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseAuth!!.signInWithCredential(credential).await()
            val user = authResult.user ?: throw IllegalStateException("Google sign-in succeeded but returned no user.")

            val email = user.email ?: ""
            _authState.value = AuthState.SignedInVerified(user.uid, email)
            profileRepository?.ensureProfileExists(user.uid, user.displayName ?: email.substringBefore("@"))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(mapAuthException(e))
        }
    }

    override fun signOut() {
        if (isFirebaseConfigured) {
            firebaseAuth?.signOut()
            try {
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
                GoogleSignIn.getClient(context, gso).signOut()
            } catch (_: Exception) {}
        }
        profileRepository?.clearProfileState()
        _authState.value = AuthState.SignedOut
    }

    private fun mapAuthException(e: Exception): Throwable {
        return when (e) {
            is FirebaseAuthInvalidUserException -> Exception("Account not found or disabled.")
            is FirebaseAuthInvalidCredentialsException -> Exception("Invalid credentials. Please try again.")
            else -> Exception(e.localizedMessage ?: "An unexpected authentication error occurred.")
        }
    }
}