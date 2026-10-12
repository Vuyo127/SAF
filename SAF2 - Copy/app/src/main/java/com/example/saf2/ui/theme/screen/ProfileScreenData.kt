package com.example.saf2.ui.theme.screen

import androidx.compose.runtime.mutableStateListOf

object ProfileScreenData {
    var savedName: String = "Tsoseletso Mashiane"
    var savedEmail: String = "tsoseletso@email.com"
    var savedPhone: String = "+27 123 456 789"
    
    val reviewsList = mutableStateListOf(
        ProfileReview(property = "Campus View", stars = 5, comment = "Great place!", date = "2026-09-10"),
        ProfileReview(property = "Student Lodge", stars = 4, comment = "Safe and clean.", date = "2026-09-12")
    )
}
