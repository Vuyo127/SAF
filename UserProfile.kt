package com.example.saf2.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class UserProfile(
    @DocumentId
    val uid: String = "",
    val role: String = "",
    val fullName: String = "",
    val email: String = "",
    val phone: String = "",
    val profileImageUrl: String = "",
    val fundingType: String = "",
    val bursaryName: String = "",
    val institution: String = "",
    val businessName: String = "",
    val isVerifiedOwner: Boolean = false,
    @ServerTimestamp
    val createdAt: Date? = null
)
