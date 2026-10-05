package com.example.saf2.data.repository

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FavoritesRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    // One doc per student: /favorites/{studentId} -> { listingIds: [...] }
    private val favoritesRef = firestore.collection("favorites")

    fun observeFavoriteListingIds(studentId: String): Flow<List<String>> = callbackFlow {
        val subscription = favoritesRef.document(studentId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                @Suppress("UNCHECKED_CAST")
                val ids = snapshot?.get("listingIds") as? List<String> ?: emptyList()
                trySend(ids)
            }
        awaitClose { subscription.remove() }
    }

    suspend fun isFavorite(studentId: String, listingId: String): Boolean {
        return try {
            val snapshot = favoritesRef.document(studentId).get().await()
            @Suppress("UNCHECKED_CAST")
            val ids = snapshot.get("listingIds") as? List<String> ?: emptyList()
            listingId in ids
        } catch (e: Exception) {
            false
        }
    }

    suspend fun toggleFavorite(studentId: String, listingId: String): Result<Unit> {
        return try {
            val isFav = isFavorite(studentId, listingId)
            val update = mapOf(
                "listingIds" to if (isFav) FieldValue.arrayRemove(listingId) else FieldValue.arrayUnion(listingId)
            )
            favoritesRef.document(studentId).set(update, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
