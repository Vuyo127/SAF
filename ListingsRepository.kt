package com.example.saf2.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.example.saf2.data.model.Listing
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ListingsRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val listingsRef = firestore.collection("listings")

    fun observeApprovedListings(): Flow<List<Listing>> = callbackFlow {
        val subscription = listingsRef
            .whereEqualTo("status", "Approved")
            .whereGreaterThan("availableBeds", 0)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObjects(Listing::class.java) ?: emptyList())
            }
        awaitClose { subscription.remove() }
    }

    suspend fun getListing(listingId: String): Listing? {
        return try {
            listingsRef.document(listingId).get().await().toObject(Listing::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun incrementViews(listingId: String) {
        try {
            listingsRef.document(listingId).update("viewsCount", FieldValue.increment(1)).await()
        } catch (_: Exception) { /* non-critical */ }
    }

    suspend fun createListing(listing: Listing): Result<String> {
        return try {
            val docRef = listingsRef.add(listing.copy(status = "Pending")).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateListing(listingId: String, updates: Map<String, Any?>): Result<Unit> {
        return try {
            listingsRef.document(listingId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteListing(listingId: String): Result<Unit> {
        return try {
            listingsRef.document(listingId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeOwnerListings(ownerId: String): Flow<List<Listing>> = callbackFlow {
        val subscription = listingsRef
            .whereEqualTo("ownerId", ownerId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObjects(Listing::class.java) ?: emptyList())
            }
        awaitClose { subscription.remove() }
    }
}
