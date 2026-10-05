package com.example.saf2.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Listing(
    @DocumentId
    val listingId: String = "",
    val ownerId: String = "",
    val title: String = "",
    val location: String = "",
    val price: Int = 0,
    val depositPrice: Int = 0,
    val roomTypes: List<String> = emptyList(),
    val totalBeds: Int = 0,
    val availableBeds: Int = 0,
    val imageUrls: List<String> = emptyList(),
    val amenities: List<String> = emptyList(),
    val description: String = "",
    val rules: String = "",
    val contactNumber: String = "",
    val rating: Double = 0.0,
    val reviewCount: Int = 0,
    val isVerified: Boolean = false,
    val isNsfasAccredited: Boolean = false,
    val acceptedFundingTypes: List<String> = emptyList(),
    val genderAllowed: String = "Mixed",
    val viewsCount: Int = 0,
    val status: String = "Pending",
    @ServerTimestamp
    val createdAt: Date? = null
)
