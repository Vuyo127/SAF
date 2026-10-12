package com.example.saf2.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.example.saf2.data.model.Application
import com.example.saf2.data.model.Listing
import com.example.saf2.data.model.Report
import com.example.saf2.data.model.Review
import com.example.saf2.data.repository.ApplicationsRepository
import com.example.saf2.data.repository.AuthRepository
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
    private val favoritesRepo: FavoritesRepository = FavoritesRepository(),
    private val authRepo: AuthRepository = AuthRepository()
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
            try {
                listingsRepo.observeApprovedListings().collect { listings ->
                    listingCache = listingCache + listings.associateBy { it.listingId }
                    _approvedListings.value = listings.map { it.toAccommodationProperty() }
                }
            } catch (_: Exception) { }
        }
    }

    fun getListingDetails(listingId: String, countView: Boolean = true) {
        viewModelScope.launch {
            try {
                val listing = listingsRepo.getListing(listingId)
                if (listing != null) {
                    listingCache = listingCache + (listingId to listing)
                    _selectedProperty.value = listing.toAccommodationProperty()
                    if (countView) {
                        listingsRepo.incrementViews(listingId)
                    }
                }
            } catch (_: Exception) { }
        }
    }

    fun getMyListings() {
        val ownerId = currentUid ?: return
        viewModelScope.launch {
            try {
                listingsRepo.observeOwnerListings(ownerId).collect { listings ->
                    listingCache = listingCache + listings.associateBy { it.listingId }
                    _myListings.value = listings.map { it.toDashboardProperty() }
                }
            } catch (_: Exception) { }
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
            try {
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
            } catch (e: Exception) {
                onResult(false, e.message)
            }
        }
    }

    /** AddListingScreen "Save Changes" button (edit-existing path). */
    fun updateListing(id: String, title: String, price: Int, totalBeds: Int) {
        viewModelScope.launch {
            try {
                listingsRepo.updateListing(id, mapOf("title" to title, "price" to price, "totalBeds" to totalBeds))
                getMyListings()
            } catch (_: Exception) { }
        }
    }

    /** Owner Dashboard "Delete" icon. */
    fun deleteListing(id: String) {
        viewModelScope.launch {
            try {
                listingsRepo.deleteListing(id)
            } catch (_: Exception) { }
        }
    }

    // --- Applications ---

    /** Owner Dashboard "Applications" tab. */
    fun getApplications() {
        val ownerId = currentUid ?: return
        viewModelScope.launch {
            try {
                applicationsRepo.observeOwnerApplications(ownerId).collect { apps ->
                    _applications.value = apps.map { it.toDashboardApplication() }
                }
            } catch (_: Exception) { }
        }
    }

    /** ProfileScreen "My Applications" tab (student view). */
    fun getStudentApplications() {
        val studentId = currentUid ?: return
        viewModelScope.launch {
            try {
                applicationsRepo.observeStudentApplications(studentId).collect { apps ->
                    val mappedApps = apps.map { app ->
                        val listing = listingCache[app.listingId] ?: listingsRepo.getListing(app.listingId)
                        ProfileApplication(
                            title = app.listingTitle.ifEmpty { listing?.title ?: "Accommodation" },
                            price = listing?.price ?: 0,
                            date = app.appliedAt?.let { dateFormat.format(it) } ?: "",
                            status = app.status
                        )
                    }
                    _studentApplications.value = mappedApps
                }
            } catch (_: Exception) { }
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
            try {
                val userProfile = authRepo.getUserProfile(studentId)
                val finalName = studentName.ifBlank { userProfile?.fullName ?: "Student" }
                val finalFunding = fundingType.ifBlank { userProfile?.fundingType ?: "Self-Funded" }
                val finalInstitution = institution.ifBlank { userProfile?.institution ?: "" }

                val listing = listingCache[listingId] ?: listingsRepo.getListing(listingId)
                val finalTitle = listing?.title ?: ""
                val finalOwnerId = ownerId.ifBlank { listing?.ownerId ?: "" }

                val application = Application(
                    studentId = studentId,
                    studentName = finalName,
                    fundingType = finalFunding,
                    institution = finalInstitution,
                    listingId = listingId,
                    listingTitle = finalTitle,
                    ownerId = finalOwnerId
                )
                applicationsRepo.applyForListing(application)
            } catch (_: Exception) { }
        }
    }

    /** Owner Dashboard "Accept" button. */
    fun acceptApplication(applicationId: String, listingId: String) {
        viewModelScope.launch {
            try {
                applicationsRepo.acceptApplication(applicationId, listingId)
            } catch (_: Exception) { }
        }
    }

    /** Owner Dashboard "Reject" button. */
    fun rejectApplication(applicationId: String) {
        viewModelScope.launch {
            try {
                applicationsRepo.rejectApplication(applicationId)
            } catch (_: Exception) { }
        }
    }

    // --- Reviews ---

    /** Pass a listingId for DetailScreen's reviews; pass "" for OwnerProfile's cross-listing reviews tab. */
    fun getReviews(listingId: String) {
        viewModelScope.launch {
            try {
                if (listingId.isNotEmpty()) {
                    reviewsRepo.observeReviewsForListing(listingId).collect { reviewList ->
                        val listing = listingCache[listingId] ?: listingsRepo.getListing(listingId)
                        _reviews.value = reviewList.map { it.toProfileReview(listing?.title ?: "Accommodation") }
                    }
                } else {
                    val ownerId = currentUid ?: return@launch
                    listingsRepo.observeOwnerListings(ownerId).collect { ownerListings ->
                        listingCache = listingCache + ownerListings.associateBy { it.listingId }
                        val ownerListingIds = ownerListings.map { it.listingId }
                        if (ownerListingIds.isNotEmpty()) {
                            reviewsRepo.observeReviewsForListings(ownerListingIds).collect { reviewList ->
                                _reviews.value = reviewList.map { review ->
                                    val listing = listingCache[review.listingId] ?: listingsRepo.getListing(review.listingId)
                                    review.toProfileReview(listing?.title ?: "Accommodation")
                                }
                            }
                        } else {
                            _reviews.value = emptyList()
                        }
                    }
                }
            } catch (_: Exception) { }
        }
    }

    /** Student Profile "Reviews" tab. */
    fun getStudentReviews() {
        val studentId = currentUid ?: return
        viewModelScope.launch {
            try {
                reviewsRepo.observeReviewsForStudent(studentId).collect { reviewList ->
                    val mapped = reviewList.map { review ->
                        val listing = listingCache[review.listingId] ?: listingsRepo.getListing(review.listingId)
                        review.toProfileReview(listing?.title ?: "Accommodation")
                    }
                    _reviews.value = mapped
                }
            } catch (_: Exception) { }
        }
    }

    /** ReviewScreen "Post Review" button. */
    fun postReview(
        listingId: String,
        propertyTitle: String = "",
        studentName: String = "",
        overall: Int = 0,
        cleanliness: Int = 0,
        security: Int = 0,
        valueForMoney: Int = 0,
        ownerResponse: Int = 0,
        comment: String = "",
        onResult: ((Boolean, String?) -> Unit)? = null
    ) {
        val studentId = currentUid ?: return
        viewModelScope.launch {
            try {
                val userProfile = authRepo.getUserProfile(studentId)
                val finalName = studentName.ifBlank { userProfile?.fullName ?: "Student" }
                val review = Review(
                    listingId = listingId,
                    studentId = studentId,
                    studentName = finalName,
                    overallRating = overall,
                    cleanliness = cleanliness,
                    security = security,
                    valueForMoney = valueForMoney,
                    ownerResponse = ownerResponse,
                    comment = comment
                )
                val result = reviewsRepo.postReview(review)
                onResult?.invoke(result.isSuccess, result.exceptionOrNull()?.message)
            } catch (e: Exception) {
                onResult?.invoke(false, e.message)
            }
        }
    }

    /** Delete a review on Student Profile. */
    fun deleteReview(reviewId: String, listingId: String) {
        viewModelScope.launch {
            try {
                reviewsRepo.deleteReview(reviewId, listingId)
                getStudentReviews()
            } catch (_: Exception) { }
        }
    }

    // --- Reports ---

    /** Owner Dashboard / OwnerProfile "Reports" tab. */
    fun getReports() {
        val ownerId = currentUid ?: return
        viewModelScope.launch {
            try {
                listingsRepo.observeOwnerListings(ownerId).collect { ownerListings ->
                    listingCache = listingCache + ownerListings.associateBy { it.listingId }
                    val ownerListingIds = ownerListings.map { it.listingId }
                    if (ownerListingIds.isNotEmpty()) {
                        reportsRepo.observeReportsForListings(ownerListingIds).collect { reportList ->
                            _reports.value = reportList.map { it.toDashboardReport() }
                        }
                    } else {
                        _reports.value = emptyList()
                    }
                }
            } catch (_: Exception) { }
        }
    }

    /** Student Profile "Reports" tab. */
    fun getStudentReports() {
        val studentId = currentUid ?: return
        viewModelScope.launch {
            try {
                reportsRepo.observeReportsForReporter(studentId).collect { reportList ->
                    _reports.value = reportList.map { it.toDashboardReport() }
                }
            } catch (_: Exception) { }
        }
    }

    /** Owner Dashboard "Mark as Resolved" button. */
    fun resolveReport(reportId: String) {
        viewModelScope.launch {
            try {
                reportsRepo.resolveReport(reportId)
            } catch (_: Exception) { }
        }
    }

    /** ReportScreen "Submit Report" button. */
    fun submitReport(
        listingId: String,
        reason: String,
        description: String,
        anonymous: Boolean,
        reporterName: String = "",
        proofImageUrls: List<String> = emptyList(),
        onResult: ((Boolean, String?) -> Unit)? = null
    ) {
        val reporterId = currentUid ?: ""
        viewModelScope.launch {
            try {
                val userProfile = if (reporterId.isNotEmpty()) authRepo.getUserProfile(reporterId) else null
                val listing = listingCache[listingId] ?: listingsRepo.getListing(listingId)
                val finalName = if (anonymous) "Anonymous" else reporterName.ifBlank { userProfile?.fullName ?: "Student" }
                val report = Report(
                    listingId = listingId,
                    listingTitle = listing?.title ?: "",
                    reporterId = if (anonymous) "" else reporterId,
                    reporterName = finalName,
                    proofImageUrls = proofImageUrls,
                    anonymous = anonymous,
                    reason = reason,
                    description = description
                )
                val result = reportsRepo.submitReport(report)
                onResult?.invoke(result.isSuccess, result.exceptionOrNull()?.message)
            } catch (e: Exception) {
                onResult?.invoke(false, e.message)
            }
        }
    }

    // --- Favorites ---

    /** ProfileScreen "Favorites" tab. */
    fun getFavorites() {
        val studentId = currentUid ?: return
        viewModelScope.launch {
            try {
                favoritesRepo.observeFavoriteListingIds(studentId).collect { ids ->
                    val listings = ids.mapNotNull { id -> listingCache[id] ?: listingsRepo.getListing(id) }
                    _favorites.value = listings.map { it.toFavoriteProperty() }
                }
            } catch (_: Exception) { }
        }
    }

    /** DetailScreen favorite-heart toggle. */
    fun toggleFavorite(listingId: String) {
        val studentId = currentUid ?: return
        viewModelScope.launch {
            try {
                favoritesRepo.toggleFavorite(studentId, listingId)
                getFavorites()
            } catch (_: Exception) { }
        }
    }

    // --- Firestore model -> existing UI data class mappers ---

    private fun Listing.toAccommodationProperty() = AccommodationProperty(
        id = listingId,
        title = title,
        location = location,
        price = price,
        roomType = roomTypes.joinToString(" / "),
        rating = rating,
        reviewCount = reviewCount,
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
        availableBeds = availableBeds,
        rating = rating,
        reviewCount = reviewCount
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
        status = status,
        listingId = listingId
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
        id = reviewId,
        listingId = listingId,
        property = propertyTitle,
        stars = overallRating,
        comment = comment,
        date = createdAt?.let { dateFormat.format(it) } ?: ""
    )
}
