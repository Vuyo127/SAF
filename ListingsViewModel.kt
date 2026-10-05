package com.example.saf2.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.example.saf2.data.model.Application
import com.example.saf2.data.model.Listing
import com.example.saf2.data.model.Report
import com.example.saf2.data.model.Review
import com.example.saf2.data.repository.ApplicationsRepository
import com.example.saf2.data.repository.FavoritesRepository
import com.example.saf2.data.repository.ListingsRepository
import com.example.saf2.data.repository.ReportsRepository
import com.example.saf2.data.repository.ReviewsRepository
import com.example.saf2.ui.theme.screen.AccommodationProperty
import com.example.saf2.ui.theme.screen.DashboardApplication
import com.example.saf2.ui.theme.screen.DashboardProperty
import com.example.saf2.ui.theme.screen.DashboardReport
import com.example.saf2.ui.theme.screen.ProfileApplication
import com.example.saf2.ui.theme.screen.ProfileFavoriteProperty
import com.example.saf2.ui.theme.screen.ProfileReview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class ListingsViewModel(
    private val listingsRepo: ListingsRepository = ListingsRepository(),
    private val applicationsRepo: ApplicationsRepository = ApplicationsRepository(),
    private val reviewsRepo: ReviewsRepository = ReviewsRepository(),
    private val reportsRepo: ReportsRepository = ReportsRepository(),
    private val favoritesRepo: FavoritesRepository = FavoritesRepository()
) : ViewModel() {

    private val currentUid: String? get() = FirebaseAuth.getInstance().currentUser?.uid
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

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

    // Raw Listing docs seen so far, so applications/reports/favorites (which only store a listingId)
    // can be mapped back to a title/price without an extra read every time.
    private var listingCache: Map<String, Listing> = emptyMap()

    // --- Listings ---

    fun getApprovedListings() {
        viewModelScope.launch {
            listingsRepo.observeApprovedListings().collect { listings ->
                listingCache = listingCache + listings.associateBy { it.listingId }
                _approvedListings.value = listings.map { it.toAccommodationProperty() }
            }
        }
    }

    fun getListingDetails(listingId: String) {
        viewModelScope.launch {
            val listing = listingsRepo.getListing(listingId)
            if (listing != null) {
                listingCache = listingCache + (listingId to listing)
                _selectedProperty.value = listing.toAccommodationProperty()
                listingsRepo.incrementViews(listingId)
            }
        }
    }

    fun getMyListings() {
        val ownerId = currentUid ?: return
        viewModelScope.launch {
            listingsRepo.observeOwnerListings(ownerId).collect { listings ->
                listingCache = listingCache + listings.associateBy { it.listingId }
                _myListings.value = listings.map { it.toDashboardProperty() }
            }
        }
    }

    /** AddListingScreen "Submit for Approval" / "Save Changes" button (new listing path). */
    fun createListing(
        title: String,
        location: String,
        price: Int,
        depositPrice: Int,
        roomTypes: List<String>,
        totalBeds: Int,
        imageUrls: List<String>,
        amenities: List<String>,
        description: String,
        rules: String,
        contactNumber: String,
        isNsfasAccredited: Boolean,
        acceptedFundingTypes: List<String>,
        onResult: (success: Boolean, error: String?) -> Unit
    ) {
        val ownerId = currentUid ?: return onResult(false, "Not signed in")
        viewModelScope.launch {
            val listing = Listing(
                ownerId = ownerId,
                title = title,
                location = location,
                price = price,
                depositPrice = depositPrice,
                roomTypes = roomTypes,
                totalBeds = totalBeds,
                availableBeds = totalBeds,
                imageUrls = imageUrls,
                amenities = amenities,
                description = description,
                rules = rules,
                contactNumber = contactNumber,
                isNsfasAccredited = isNsfasAccredited,
                acceptedFundingTypes = acceptedFundingTypes
            )
            val result = listingsRepo.createListing(listing)
            onResult(result.isSuccess, result.exceptionOrNull()?.message)
        }
    }

    /** AddListingScreen "Save Changes" button (edit-existing path). */
    fun updateListing(id: String, title: String, price: Int, totalBeds: Int) {
        viewModelScope.launch {
            listingsRepo.updateListing(id, mapOf("title" to title, "price" to price, "totalBeds" to totalBeds))
            getMyListings()
        }
    }

    /** Owner Dashboard "Delete" icon. */
    fun deleteListing(id: String) {
        viewModelScope.launch { listingsRepo.deleteListing(id) }
    }

    // --- Applications ---

    /** Owner Dashboard "Applications" tab. */
    fun getApplications() {
        val ownerId = currentUid ?: return
        viewModelScope.launch {
            applicationsRepo.observeOwnerApplications(ownerId).collect { apps ->
                _applications.value = apps.map { it.toDashboardApplication() }
            }
        }
    }

    /** ProfileScreen "My Applications" tab (student view). */
    fun getStudentApplications() {
        val studentId = currentUid ?: return
        viewModelScope.launch {
            applicationsRepo.observeStudentApplications(studentId).collect { apps ->
                _studentApplications.value = apps.map { it.toProfileApplication() }
            }
        }
    }

    /** DetailScreen "Confirm" button on the Apply dialog. */
    fun applyForAccommodation(
        listingId: String,
        ownerId: String,
        studentName: String = "",
        fundingType: String = "",
        institution: String = ""
    ) {
        val studentId = currentUid ?: return
        viewModelScope.launch {
            val application = Application(
                studentId = studentId,
                studentName = studentName,
                fundingType = fundingType,
                institution = institution,
                listingId = listingId,
                listingTitle = listingCache[listingId]?.title ?: "",
                ownerId = ownerId
            )
            applicationsRepo.applyForListing(application)
        }
    }

    /** Owner Dashboard "Accept" button. */
    fun acceptApplication(applicationId: String, listingId: String) {
        viewModelScope.launch { applicationsRepo.acceptApplication(applicationId, listingId) }
    }

    /** Owner Dashboard "Reject" button. */
    fun rejectApplication(applicationId: String) {
        viewModelScope.launch { applicationsRepo.rejectApplication(applicationId) }
    }

    // --- Reviews ---

    /** Pass a listingId for DetailScreen's reviews; pass "" for OwnerProfile's cross-listing reviews tab. */
    fun getReviews(listingId: String) {
        viewModelScope.launch {
            if (listingId.isNotEmpty()) {
                reviewsRepo.observeReviewsForListing(listingId).collect { reviewList ->
                    _reviews.value = reviewList.map { it.toProfileReview(listingCache[listingId]?.title ?: "") }
                }
            } else {
                val ownerId = currentUid ?: return@launch
                val ownerListingIds = listingCache.values.filter { it.ownerId == ownerId }.map { it.listingId }
                reviewsRepo.observeReviewsForListings(ownerListingIds).collect { reviewList ->
                    _reviews.value = reviewList.map { review ->
                        review.toProfileReview(listingCache[review.listingId]?.title ?: "")
                    }
                }
            }
        }
    }

    /** ReviewScreen "Post Review" button. */
    fun postReview(listingId: String, comment: String, rating: Int, propertyTitle: String) {
        val studentId = currentUid ?: return
        viewModelScope.launch {
            val review = Review(listingId = listingId, studentId = studentId, overallRating = rating, comment = comment)
            reviewsRepo.postReview(review)
        }
    }

    // --- Reports ---

    /** Owner Dashboard / OwnerProfile "Reports" tab. */
    fun getReports() {
        val ownerId = currentUid ?: return
        viewModelScope.launch {
            val ownerListingIds = listingCache.values.filter { it.ownerId == ownerId }.map { it.listingId }
            reportsRepo.observeReportsForListings(ownerListingIds).collect { reportList ->
                _reports.value = reportList.map { it.toDashboardReport() }
            }
        }
    }

    /** Owner Dashboard "Mark as Resolved" button. */
    fun resolveReport(reportId: String) {
        viewModelScope.launch { reportsRepo.resolveReport(reportId) }
    }

    /** ReportScreen "Submit Report" button. */
    fun submitReport(listingId: String, reason: String, description: String, anonymous: Boolean) {
        val reporterId = currentUid ?: ""
        viewModelScope.launch {
            val report = Report(
                listingId = listingId,
                listingTitle = listingCache[listingId]?.title ?: "",
                reporterId = if (anonymous) "" else reporterId,
                anonymous = anonymous,
                reason = reason,
                description = description
            )
            reportsRepo.submitReport(report)
        }
    }

    // --- Favorites ---

    /** ProfileScreen "Favorites" tab. */
    fun getFavorites() {
        val studentId = currentUid ?: return
        viewModelScope.launch {
            favoritesRepo.observeFavoriteListingIds(studentId).collect { ids ->
                val listings = ids.mapNotNull { id -> listingCache[id] ?: listingsRepo.getListing(id) }
                _favorites.value = listings.map { it.toFavoriteProperty() }
            }
        }
    }

    /** DetailScreen favorite-heart toggle. */
    fun toggleFavorite(listingId: String) {
        val studentId = currentUid ?: return
        viewModelScope.launch { favoritesRepo.toggleFavorite(studentId, listingId) }
    }

    // --- Firestore model -> existing UI data class mappers ---

    private fun Listing.toAccommodationProperty() = AccommodationProperty(
        id = listingId,
        title = title,
        location = location,
        price = price,
        roomType = roomTypes.joinToString(" / "),
        rating = rating,
        isVerified = isVerified,
        isNsfasAccredited = isNsfasAccredited,
        fundingTypes = acceptedFundingTypes,
        imageUrl = imageUrls.firstOrNull(),
        ownerId = ownerId
    )

    private fun Listing.toDashboardProperty() = DashboardProperty(
        id = listingId,
        title = title,
        price = price,
        status = status,
        funding = acceptedFundingTypes,
        totalBeds = totalBeds,
        availableBeds = availableBeds
    )

    private fun Listing.toFavoriteProperty() = ProfileFavoriteProperty(
        id = listingId, title = title, price = price, status = status, funding = acceptedFundingTypes
    )

    private fun Application.toDashboardApplication() = DashboardApplication(
        id = applicationId,
        name = studentName.ifEmpty { "Student" },
        funding = fundingType,
        property = listingTitle,
        date = appliedAt?.let { dateFormat.format(it) } ?: "",
        institution = institution,
        status = status
    )

    private fun Application.toProfileApplication() = ProfileApplication(
        title = listingTitle,
        price = listingCache[listingId]?.price ?: 0,
        date = appliedAt?.let { dateFormat.format(it) } ?: "",
        status = status
    )

    private fun Report.toDashboardReport() = DashboardReport(
        id = reportId,
        property = listingTitle,
        reason = reason,
        description = description,
        reporter = if (anonymous) "Anonymous" else reporterName,
        date = createdAt?.let { dateFormat.format(it) } ?: "",
        status = status
    )

    private fun Review.toProfileReview(propertyTitle: String) = ProfileReview(
        property = propertyTitle,
        stars = overallRating,
        comment = comment,
        date = createdAt?.let { dateFormat.format(it) } ?: ""
    )
}
