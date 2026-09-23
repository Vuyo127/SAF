package com.example.saf2.viewmodel

import androidx.lifecycle.ViewModel
import com.example.saf2.ui.theme.screen.AccommodationProperty
import com.example.saf2.ui.theme.screen.DashboardApplication
import com.example.saf2.ui.theme.screen.DashboardProperty
import com.example.saf2.ui.theme.screen.DashboardReport
import com.example.saf2.ui.theme.screen.ProfileReview
import com.example.saf2.ui.theme.screen.ProfileApplication
import com.example.saf2.ui.theme.screen.ProfileFavoriteProperty
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ListingsViewModel : ViewModel() {
    private val _approvedListings = MutableStateFlow<List<AccommodationProperty>>(emptyList())
    val approvedListings: StateFlow<List<AccommodationProperty>> = _approvedListings.asStateFlow()

    private val _selectedProperty = MutableStateFlow<AccommodationProperty?>(null)
    val selectedProperty: StateFlow<AccommodationProperty?> = _selectedProperty.asStateFlow()

    private val _myListings = MutableStateFlow<List<DashboardProperty>>(emptyList())
    val myListings: StateFlow<List<DashboardProperty>> = _myListings.asStateFlow()

    private val _applications = MutableStateFlow<List<DashboardApplication>>(emptyList())
    val applications: StateFlow<List<DashboardApplication>> = _applications.asStateFlow()

    private val _studentApplications = MutableStateFlow<List<ProfileApplication>>(emptyList())
    val studentApplications: StateFlow<List<ProfileApplication>> = _studentApplications.asStateFlow()

    private val _reports = MutableStateFlow<List<DashboardReport>>(emptyList())
    val reports: StateFlow<List<DashboardReport>> = _reports.asStateFlow()

    private val _reviews = MutableStateFlow<List<ProfileReview>>(emptyList())
    val reviews: StateFlow<List<ProfileReview>> = _reviews.asStateFlow()

    private val _favorites = MutableStateFlow<List<ProfileFavoriteProperty>>(emptyList())
    val favorites: StateFlow<List<ProfileFavoriteProperty>> = _favorites.asStateFlow()

    fun getApprovedListings() {
        // TODO BACKEND: backend/Database must fill in GET /api/listings?status=APPROVED to return real imageUrl, title, price, ownerId.
    }

    fun getListingDetails(listingId: String) {
        _selectedProperty.value = _approvedListings.value.find { it.id == listingId }
        // TODO BACKEND: backend/Database must fill in GET /api/listings/{listingId} to return real property details, imageUrls, price, title, location, roomType, rating, amenities, and ownerId.
    }

    fun getMyListings() {
        // TODO BACKEND: backend/Database must fill in GET /api/listings?ownerId=currentUserId
    }

    fun getReviews(listingId: String) {
        // TODO BACKEND: backend/Database must fill in GET /api/reviews?listingId={id}
    }

    fun getApplications() {
        // TODO BACKEND: backend/Database must fill in GET /api/applications?userId=...
    }

    fun getFavorites() {
        // TODO BACKEND: backend/Database must fill in GET /api/favorites?userId=...
    }

    fun postReview(listingId: String, comment: String, rating: Int, propertyTitle: String) {
        // TODO BACKEND: backend/Database must fill in POST /api/reviews {listingId, userId, comment, ratings} and GET /api/reviews?listingId={id} must return updated list
        getReviews(listingId)
    }

    fun updateListing(id: String, title: String, price: Int, totalBeds: Int) {
        // TODO BACKEND: backend/Database must fill in PUT /api/listings/{id} must actually save title, location, price to DB. GET /api/listings?ownerId=currentUserId must return owner listings.
        getMyListings()
    }

    fun applyForAccommodation(listingId: String, ownerId: String) {
        // TODO BACKEND: backend/Database must fill in POST /api/applications { listingId, studentId, ownerId, status=PENDING }
    }
}
