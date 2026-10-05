package com.example.saf2.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.saf2.data.model.UserProfile
import com.example.saf2.data.repository.AuthRepository
import com.example.saf2.data.repository.AuthResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val role: String = "student"
)

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

class AuthViewModel(
    private val repository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        // Picks up an already-signed-in user (e.g. app restart) without waiting for getProfile() to be called.
        repository.currentUserId?.let { getProfile() }
    }

    /** SignUpScreen "Sign Up" button. */
    fun signUp(
        role: String,
        fullName: String,
        email: String,
        phone: String,
        password: String,
        fundingType: String = "",
        bursaryName: String = "",
        businessName: String = "",
        onResult: (success: Boolean, error: String?) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            when (val result = repository.signUp(role, fullName, email, phone, password, fundingType, bursaryName, businessName)) {
                is AuthResult.Success -> {
                    getProfile()
                    _uiState.value = AuthUiState.Idle
                    onResult(true, null)
                }
                is AuthResult.Error -> {
                    _uiState.value = AuthUiState.Error(result.message)
                    onResult(false, result.message)
                }
            }
        }
    }

    /** LoginScreen "Login" button. onResult's role param lets the screen route to the right dashboard. */
    fun login(email: String, password: String, onResult: (success: Boolean, role: String?, error: String?) -> Unit) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            when (val result = repository.login(email, password)) {
                is AuthResult.Success -> {
                    val profile = repository.getUserProfile(result.uid)
                    applyProfile(result.uid, profile)
                    _uiState.value = AuthUiState.Idle
                    onResult(true, profile?.role, null)
                }
                is AuthResult.Error -> {
                    _uiState.value = AuthUiState.Error(result.message)
                    onResult(false, null, result.message)
                }
            }
        }
    }

    /** ForgotPasswordScreen "Send Reset Link" button. */
    fun sendPasswordReset(email: String, onResult: (success: Boolean, error: String?) -> Unit) {
        viewModelScope.launch {
            when (val result = repository.sendPasswordResetEmail(email)) {
                is AuthResult.Success -> onResult(true, null)
                is AuthResult.Error -> onResult(false, result.message)
            }
        }
    }

    /** AccommodationScreen / ProfileScreen / OwnerProfile LaunchedEffect. */
    fun getProfile() {
        val uid = repository.currentUserId ?: return
        viewModelScope.launch {
            val profile = repository.getUserProfile(uid)
            applyProfile(uid, profile)
        }
    }

    /** ProfileScreen / OwnerProfile "Save Changes" button. */
    fun updateProfile(name: String, email: String, phone: String) {
        val uid = repository.currentUserId ?: return
        _currentUser.value = _currentUser.value?.copy(name = name, email = email, phone = phone)
        viewModelScope.launch {
            repository.updateUserProfile(uid, mapOf("fullName" to name, "email" to email, "phone" to phone))
        }
    }

    /** Logout button on every drawer. */
    fun logout() {
        repository.logout()
        _currentUser.value = null
    }

    private fun applyProfile(uid: String, profile: UserProfile?) {
        _currentUser.value = User(
            id = uid,
            name = profile?.fullName ?: "",
            email = profile?.email ?: "",
            phone = profile?.phone ?: "",
            role = profile?.role ?: "student"
        )
    }
}
