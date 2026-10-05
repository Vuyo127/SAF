package com.example.saf2.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Application(
    @DocumentId
    val applicationId: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val fundingType: String = "",
    val institution: String = "",
    val listingId: String = "",
    val listingTitle: String = "",
    val ownerId: String = "",
    val status: String = "Pending",
    @ServerTimestamp
    val appliedAt: Date? = null
)

data class Report(
    @DocumentId
    val reportId: String = "",
    val listingId: String = "",
    val listingTitle: String = "",
    val reporterId: String = "",
    val reporterName: String = "Anonymous",
    val reason: String = "",
    val description: String = "",
    val proofImageUrls: List<String> = emptyList(),
    val anonymous: Boolean = false,
    val status: String = "Pending",
    @ServerTimestamp
    val createdAt: Date? = null
)

data class Review(
    @DocumentId
    val reviewId: String = "",
    val listingId: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val overallRating: Int = 0,
    val cleanliness: Int = 0,
    val security: Int = 0,
    val valueForMoney: Int = 0,
    val ownerResponse: Int = 0,
    val comment: String = "",
    @ServerTimestamp
    val createdAt: Date? = null
)
