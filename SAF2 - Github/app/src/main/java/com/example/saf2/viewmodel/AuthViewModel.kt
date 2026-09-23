package com.example.saf2.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class User(
    val id: String = "user_1",
    val name: String = "Student User",
    val email: String = "student@test.com",
    val phone: String = "+27821234567",
    val role: String = "student"
)

class AuthViewModel : ViewModel() {
    private val _currentUser = MutableStateFlow<User?>(User())
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    fun getProfile() {
        // TODO BACKEND: backend must fill in GET /api/users/me to return logged in user profile, name, profileImageUrl. Frontend uses DataStore for now.
    }

    fun updateProfile(name: String, email: String, phone: String) {
        _currentUser.value = _currentUser.value?.copy(name = name, email = email, phone = phone)
        // TODO BACKEND: backend must fill in PUT /api/users/me
    }
}
