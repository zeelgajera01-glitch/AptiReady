package com.example.aptiready.data.repository

import android.content.Context
import android.util.Log
import com.example.aptiready.data.model.ProfileState
import com.example.aptiready.data.model.UserProfile
import com.example.aptiready.data.remote.FirebaseConfigManager
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FirestoreProfileRepository(
    private val context: Context
) : ProfileRepository {

    private val isConfigured = FirebaseConfigManager.isConfigured(context)

    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Loading)
    override val profileState: StateFlow<ProfileState> = _profileState.asStateFlow()

    private var firestore: FirebaseFirestore? = null

    init {
        if (isConfigured) {
            try {
                firestore = FirebaseFirestore.getInstance()
                val projectId = FirebaseApp.getInstance().options.projectId
                Log.d("FirestoreProfileRepo", "Initialized FirebaseFirestore for project: $projectId")
            } catch (e: Exception) {
                Log.e("FirestoreProfileRepo", "Firestore initialization failed: ${e.message}")
                _profileState.value = ProfileState.Error("Firestore initialization failed.")
            }
        } else {
            _profileState.value = ProfileState.Error("Firebase not configured.")
        }
    }

    override suspend fun loadProfile(uid: String): Result<UserProfile?> = withContext(Dispatchers.IO) {
        if (!isConfigured || firestore == null) {
            val err = "Firebase is not configured."
            _profileState.value = ProfileState.Error(err)
            return@withContext Result.failure(IllegalStateException(err))
        }

        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        val currentUid = currentUser?.uid

        if (currentUid == null || currentUid != uid) {
            val err = "Auth UID mismatch. Requested: $uid, Current Auth UID: $currentUid"
            Log.w("FirestoreProfileRepo", err)
            _profileState.value = ProfileState.Error(err)
            return@withContext Result.failure(SecurityException(err))
        }

        Log.d("FirestoreProfileRepo", "loadProfile - Target doc: users/$uid, Auth UID: $currentUid, isEmailVerified: ${currentUser.isEmailVerified}")
        _profileState.value = ProfileState.Loading

        try {
            val docRef = firestore!!.collection("users").document(uid)
            val snapshot = docRef.get().await()

            if (snapshot.exists()) {
                val displayName = snapshot.getString("displayName") ?: "AptiRise Learner"
                val dailyGoal = (snapshot.getLong("dailyGoal") ?: 10L).toInt()
                val profile = UserProfile(uid = uid, displayName = displayName, dailyGoal = dailyGoal)
                _profileState.value = ProfileState.Success(profile)
                Result.success(profile)
            } else {
                Log.d("FirestoreProfileRepo", "loadProfile - Document users/$uid does not exist.")
                _profileState.value = ProfileState.NotFound
                Result.success(null)
            }
        } catch (e: FirebaseFirestoreException) {
            val diag = "Firestore READ Error [Code: ${e.code.name}]: ${e.message}"
            Log.e("FirestoreProfileRepo", diag)
            _profileState.value = ProfileState.Error(diag, isRecoverable = true)
            Result.failure(e)
        } catch (e: Exception) {
            val diag = "READ Error: ${e.localizedMessage}"
            Log.e("FirestoreProfileRepo", diag)
            _profileState.value = ProfileState.Error(diag, isRecoverable = true)
            Result.failure(e)
        }
    }

    override suspend fun ensureProfileExists(uid: String, displayName: String): Result<UserProfile> = withContext(Dispatchers.IO) {
        if (!isConfigured || firestore == null) {
            return@withContext Result.failure(IllegalStateException(FirebaseConfigManager.getConfigurationMessage()))
        }

        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        val currentUid = currentUser?.uid

        if (currentUid == null || currentUid != uid) {
            val err = "Auth UID mismatch. Requested: $uid, Current Auth UID: $currentUid"
            Log.w("FirestoreProfileRepo", err)
            return@withContext Result.failure(SecurityException(err))
        }

        // Force token refresh before write to ensure email_verified claim is fresh
        try {
            currentUser.reload().await()
            val tokenResult = currentUser.getIdToken(true).await()
            val verifiedClaim = (tokenResult.claims["email_verified"] as? Boolean) == true
            Log.d("FirestoreProfileRepo", "ensureProfileExists token refresh - isEmailVerified: ${currentUser.isEmailVerified}, claim: $verifiedClaim")

            if (!currentUser.isEmailVerified || !verifiedClaim) {
                val err = "Email is not verified in Firebase Auth ID token claim. Cannot write profile."
                Log.w("FirestoreProfileRepo", err)
                return@withContext Result.failure(IllegalStateException(err))
            }
        } catch (e: Exception) {
            Log.w("FirestoreProfileRepo", "Failed to refresh ID token before ensureProfileExists: ${e.message}")
        }

        val cleanName = displayName.trim().ifEmpty { "AptiRise Learner" }
        val docRef = firestore!!.collection("users").document(uid)

        try {
            val snapshot = docRef.get().await()

            if (snapshot.exists()) {
                val dName = snapshot.getString("displayName") ?: cleanName
                val goal = (snapshot.getLong("dailyGoal") ?: 10L).toInt()
                val existing = UserProfile(uid = uid, displayName = dName, dailyGoal = goal)
                _profileState.value = ProfileState.Success(existing)
                return@withContext Result.success(existing)
            }

            // Create Missing Profile - Exactly 4 fields: displayName, dailyGoal (int), createdAt, updatedAt
            val createPayload = hashMapOf<String, Any>(
                "displayName" to cleanName,
                "dailyGoal" to 10,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            Log.d("FirestoreProfileRepo", "Executing CREATE on users/$uid with fields: ${createPayload.keys}")
            docRef.set(createPayload).await()

            val created = UserProfile(uid = uid, displayName = cleanName, dailyGoal = 10)
            _profileState.value = ProfileState.Success(created)
            Result.success(created)
        } catch (e: FirebaseFirestoreException) {
            val diag = "Firestore CREATE Error [Code: ${e.code.name}]: ${e.message}"
            Log.e("FirestoreProfileRepo", diag)
            Result.failure(Exception(diag, e))
        } catch (e: Exception) {
            Log.e("FirestoreProfileRepo", "CREATE Error: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun updateProfile(uid: String, displayName: String, dailyGoal: Int): Result<Unit> = withContext(Dispatchers.IO) {
        if (!isConfigured || firestore == null) {
            return@withContext Result.failure(IllegalStateException(FirebaseConfigManager.getConfigurationMessage()))
        }

        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        val currentUid = currentUser?.uid

        if (currentUid == null || currentUid != uid) {
            val err = "Auth UID mismatch. Requested: $uid, Current Auth UID: $currentUid"
            Log.w("FirestoreProfileRepo", err)
            return@withContext Result.failure(SecurityException(err))
        }

        // Force token refresh before write to ensure email_verified claim is fresh
        try {
            currentUser.reload().await()
            val tokenResult = currentUser.getIdToken(true).await()
            val verifiedClaim = (tokenResult.claims["email_verified"] as? Boolean) == true
            Log.d("FirestoreProfileRepo", "updateProfile token refresh - isEmailVerified: ${currentUser.isEmailVerified}, claim: $verifiedClaim")

            if (!currentUser.isEmailVerified || !verifiedClaim) {
                val err = "Email is not verified in Firebase Auth ID token claim. Cannot update profile."
                Log.w("FirestoreProfileRepo", err)
                return@withContext Result.failure(IllegalStateException(err))
            }
        } catch (e: Exception) {
            Log.w("FirestoreProfileRepo", "Failed to refresh ID token before updateProfile: ${e.message}")
        }

        val cleanName = displayName.trim()
        if (cleanName.length !in 1..50) {
            return@withContext Result.failure(IllegalArgumentException("Display name must be between 1 and 50 characters."))
        }
        if (dailyGoal !in 5..100) {
            return@withContext Result.failure(IllegalArgumentException("Daily question goal must be between 5 and 100."))
        }

        val docRef = firestore!!.collection("users").document(uid)

        try {
            val snapshot = docRef.get().await()

            if (!snapshot.exists()) {
                // Document missing -> Create missing profile with createdAt & updatedAt
                val createPayload = hashMapOf<String, Any>(
                    "displayName" to cleanName,
                    "dailyGoal" to dailyGoal,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                Log.d("FirestoreProfileRepo", "Document missing. Executing CREATE on users/$uid with fields: ${createPayload.keys}")
                docRef.set(createPayload).await()
            } else {
                // Document exists -> Update displayName, dailyGoal, updatedAt (preserves createdAt)
                val updatePayload = hashMapOf<String, Any>(
                    "displayName" to cleanName,
                    "dailyGoal" to dailyGoal,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                Log.d("FirestoreProfileRepo", "Executing UPDATE on users/$uid with fields: ${updatePayload.keys}")
                docRef.update(updatePayload).await()
            }

            val updated = UserProfile(uid = uid, displayName = cleanName, dailyGoal = dailyGoal)
            _profileState.value = ProfileState.Success(updated)
            Result.success(Unit)
        } catch (e: FirebaseFirestoreException) {
            val diag = "Firestore UPDATE Error [Code: ${e.code.name}]: ${e.message}"
            Log.e("FirestoreProfileRepo", diag)
            Result.failure(Exception(diag, e))
        } catch (e: Exception) {
            Log.e("FirestoreProfileRepo", "UPDATE Error: ${e.message}")
            Result.failure(e)
        }
    }

    override fun clearProfileState() {
        _profileState.value = ProfileState.NotFound
    }
}