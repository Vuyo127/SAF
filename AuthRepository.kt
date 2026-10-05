package com.example.saf2.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.saf2.data.model.UserProfile
import kotlinx.coroutines.tasks.await

sealed class AuthResult {
    data class Success(val uid: String) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    val currentUserId: String?
        get() = auth.currentUser?.uid

    suspend fun signUp(
        role: String,
        fullName: String,
        email: String,
        phone: String,
        password: String,
        fundingType: String = "",
        bursaryName: String = "",
        businessName: String = ""
    ): AuthResult {
        return try {
            val authResultTask = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = authResultTask.user?.uid
                ?: return AuthResult.Error("Sign up failed: no user returned")

            val profile = UserProfile(
                uid = uid,
                role = role,
                fullName = fullName,
                email = email,
                phone = phone,
                fundingType = fundingType,
                bursaryName = bursaryName,
                businessName = businessName
            )

            firestore.collection("users").document(uid).set(profile).await()
            AuthResult.Success(uid)
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Sign up failed")
        }
    }

    suspend fun login(email: String, password: String): AuthResult {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: return AuthResult.Error("Login failed: no user returned")
            AuthResult.Success(uid)
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Invalid email or password")
        }
    }

    suspend fun sendPasswordResetEmail(email: String): AuthResult {
        return try {
            auth.sendPasswordResetEmail(email).await()
            AuthResult.Success(uid = "")
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Could not send reset email")
        }
    }

    suspend fun getUserProfile(uid: String): UserProfile? {
        return try {
            firestore.collection("users").document(uid).get().await()
                .toObject(UserProfile::class.java)
        } catch (e: Exception) {
            null
        }
    }

    /** ProfileScreen / OwnerProfile "Save Changes" button. */
    suspend fun updateUserProfile(uid: String, updates: Map<String, Any?>): AuthResult {
        return try {
            firestore.collection("users").document(uid).update(updates).await()
            AuthResult.Success(uid)
        } catch (e: Exception) {
            AuthResult.Error(e.message ?: "Could not update profile")
        }
    }

    fun logout() {
        auth.signOut()
    }
}
