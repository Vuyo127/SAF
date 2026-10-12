package com.example.saf2.data.repository

import com.example.saf2.data.model.UserProfile
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

sealed class AuthResult {
    data class Success(val uid: String) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

private enum class AuthAction { LOGIN, SIGN_UP, RESET }

/** Turns Firebase's technical exceptions into something a student can act on. */
private fun friendlyMessage(e: Exception, action: AuthAction, fallback: String): String = when {
    e is FirebaseNetworkException -> "No internet connection. Please try again."
    e is FirebaseAuthUserCollisionException -> "An account with this email already exists."
    e is FirebaseAuthWeakPasswordException -> "That password is too weak. Use at least 8 characters."
    e is FirebaseAuthInvalidUserException && action == AuthAction.RESET -> "No account was found with that email."
    e is FirebaseAuthInvalidCredentialsException && action != AuthAction.LOGIN -> "That email address doesn't look valid."
    e is FirebaseAuthInvalidUserException || e is FirebaseAuthInvalidCredentialsException -> "Incorrect email or password."
    else -> e.message ?: fallback
}

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    val currentUserId: String?
        get() = auth.currentUser?.uid

    /**
     * Creates the login AND the /users/<uid> profile. If the profile can't be saved,
     * the new login is deleted again, so the person can simply retry instead of being
     * stuck with a login that has no profile.
     */
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
            val created = auth.createUserWithEmailAndPassword(email, password).await()
            val user = created.user ?: return AuthResult.Error("Sign up failed: no user returned")

            val profile = UserProfile(
                uid = user.uid,
                role = role,
                fullName = fullName,
                email = email,
                phone = phone,
                fundingType = fundingType,
                bursaryName = bursaryName,
                businessName = businessName
            )

            try {
                firestore.collection("users").document(user.uid).set(profile).await()
            } catch (e: Exception) {
                try { user.delete().await() } catch (_: Exception) { }
                auth.signOut()
                return AuthResult.Error(
                    "Could not save your profile (" + (e.message ?: "unknown error") + "). Please try again."
                )
            }
            AuthResult.Success(user.uid)
        } catch (e: Exception) {
            AuthResult.Error(friendlyMessage(e, AuthAction.SIGN_UP, "Sign up failed"))
        }
    }

    suspend fun login(email: String, password: String): AuthResult {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: return AuthResult.Error("Login failed: no user returned")
            AuthResult.Success(uid)
        } catch (e: Exception) {
            AuthResult.Error(friendlyMessage(e, AuthAction.LOGIN, "Login failed"))
        }
    }

    suspend fun sendPasswordResetEmail(email: String): AuthResult {
        return try {
            auth.sendPasswordResetEmail(email).await()
            AuthResult.Success(uid = "")
        } catch (e: Exception) {
            AuthResult.Error(friendlyMessage(e, AuthAction.RESET, "Could not send reset email"))
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