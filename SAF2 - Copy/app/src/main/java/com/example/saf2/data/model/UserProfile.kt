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
    // @JvmField keeps the Firestore field name exactly "isVerifiedOwner" (the admin console uses it).
    // Without it, Firestore strips the "is" prefix and the value is never read back.
    @JvmField
    val isVerifiedOwner: Boolean = false,
    // Set to true by the admin console's Delete button (a soft ban: the login itself still exists).
    val disabled: Boolean = false,
    @ServerTimestamp
    val createdAt: Date? = null
)