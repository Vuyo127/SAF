package com.example.saf2.ui.theme.screen

import androidx.compose.runtime.mutableStateListOf

object ProfileScreenData {
    var savedName: String = "Tsoseletso Mashiane"
    var savedEmail: String = "tsoseletso@email.com"
    var savedPhone: String = "+27 123 456 789"
    
    val reviewsList = mutableStateListOf(
        ProfileReview("Campus View", 5, "Great place!", "2026-09-10"),
        ProfileReview("Student Lodge", 4, "Safe and clean.", "2026-09-12")
    )
}
