package com.example.saf2.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.example.saf2.data.model.Review
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ReviewsRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val reviewsRef = firestore.collection("reviews")
    private val listingsRef = firestore.collection("listings")

    fun observeReviewsForListing(listingId: String): Flow<List<Review>> = callbackFlow {
        val subscription = reviewsRef
            .whereEqualTo("listingId", listingId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObjects(Review::class.java) ?: emptyList())
            }
        awaitClose { subscription.remove() }
    }

    /** whereIn supports at most 10 values - fine for one owner's listing count here. */
    fun observeReviewsForListings(listingIds: List<String>): Flow<List<Review>> = callbackFlow {
        if (listingIds.isEmpty()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val subscription = reviewsRef
            .whereIn("listingId", listingIds.take(10))
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObjects(Review::class.java) ?: emptyList())
            }
        awaitClose { subscription.remove() }
    }

    /** Posts the review, then recomputes the listing's rating/reviewCount from all its reviews. */
    suspend fun postReview(review: Review): Result<Unit> {
        return try {
            reviewsRef.add(review).await()
            val allReviews = reviewsRef.whereEqualTo("listingId", review.listingId).get().await()
                .toObjects(Review::class.java)
            val average = if (allReviews.isEmpty()) 0.0 else allReviews.map { it.overallRating }.average()
            listingsRef.document(review.listingId)
                .update(mapOf("rating" to average, "reviewCount" to allReviews.size))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
