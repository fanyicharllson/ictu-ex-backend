package com.fanyiadrien.ictu_ex.data.remote.firebase

import com.fanyiadrien.ictu_ex.data.remote.api.AuthResult
import com.fanyiadrien.ictu_ex.data.remote.api.AuthService
import com.fanyiadrien.ictu_ex.data.remote.api.AuthUser
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * STEP 2 — FIREBASE IMPLEMENTATION
 *
 * Wraps your existing Firebase logic into the AuthService interface.
 * Nothing in the app changes — it still calls authService.login(...)
 * but now it goes through this wrapper instead of Firebase directly.
 */
class FirebaseAuthService @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthService {

    override suspend fun register(
        email: String,
        password: String,
        displayName: String,
        studentId: String
    ): Result<AuthResult> = runCatching {
        val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
        val firebaseUser = result.user!!

        // Save extra fields to Firestore (your existing pattern)
        val userDoc = mapOf(
            "id"          to firebaseUser.uid,
            "email"       to email,
            "displayName" to displayName,
            "studentId"   to studentId,
            "userType"    to "STUDENT"
        )
        firestore.collection("users").document(firebaseUser.uid).set(userDoc).await()

        val token = firebaseUser.getIdToken(false).await().token ?: ""
        AuthResult(
            token = token,
            user  = AuthUser(
                id          = firebaseUser.uid,
                email       = email,
                displayName = displayName,
                studentId   = studentId,
                userType    = "STUDENT"
            ),
            message = "Registered successfully!"
        )
    }

    override suspend fun login(email: String, password: String): Result<AuthResult> = runCatching {
        val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
        val firebaseUser = result.user!!
        val token = firebaseUser.getIdToken(false).await().token ?: ""

        // Fetch extra fields from Firestore
        val doc = firestore.collection("users").document(firebaseUser.uid).get().await()
        AuthResult(
            token = token,
            user  = AuthUser(
                id          = firebaseUser.uid,
                email       = firebaseUser.email ?: email,
                displayName = doc.getString("displayName") ?: "",
                studentId   = doc.getString("studentId") ?: "",
                userType    = doc.getString("userType") ?: "STUDENT"
            ),
            message = "Login successful!"
        )
    }

    override suspend fun logout(): Result<Unit> = runCatching {
        firebaseAuth.signOut()
    }

    override suspend fun getCurrentUser(): AuthUser? {
        val firebaseUser = firebaseAuth.currentUser ?: return null
        return runCatching {
            val doc = firestore.collection("users").document(firebaseUser.uid).get().await()
            AuthUser(
                id          = firebaseUser.uid,
                email       = firebaseUser.email ?: "",
                displayName = doc.getString("displayName") ?: "",
                studentId   = doc.getString("studentId") ?: "",
                userType    = doc.getString("userType") ?: "STUDENT"
            )
        }.getOrNull()
    }

    // Firebase doesn't have a verification code flow — email verification is link-based.
    // These are no-ops that succeed silently so the UI doesn't break when on Firebase mode.
    override suspend fun verifyCode(email: String, code: String): Result<Unit> =
        Result.success(Unit)

    override suspend fun resendVerificationCode(email: String): Result<Unit> =
        Result.success(Unit)

    override suspend fun updateUserType(userType: String): Result<AuthUser> = runCatching {
        val firebaseUser = firebaseAuth.currentUser
            ?: throw IllegalStateException("Not authenticated")
        firestore.collection("users")
            .document(firebaseUser.uid)
            .update("userType", userType)
            .await()
        getCurrentUser()!!
    }
}
