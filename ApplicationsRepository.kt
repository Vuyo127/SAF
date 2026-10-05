package com.example.saf2.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.example.saf2.data.model.Application
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ApplicationsRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val applicationsRef = firestore.collection("applications")
    private val listingsRef = firestore.collection("listings")

    suspend fun applyForListing(application: Application): Result<Unit> {
        return try {
            firestore.runTransaction { txn ->
                val listingSnap = txn.get(listingsRef.document(application.listingId))
                val availableBeds = listingSnap.getLong("availableBeds") ?: 0L
                if (availableBeds <= 0) {
                    throw IllegalStateException("No beds available for this listing")
                }
                val newDocRef = applicationsRef.document()
                txn.set(newDocRef, application)
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun acceptApplication(applicationId: String, listingId: String): Result<Unit> {
        return try {
            firestore.runTransaction { txn ->
                val listingDoc = listingsRef.document(listingId)
                val listingSnap = txn.get(listingDoc)
                val availableBeds = listingSnap.getLong("availableBeds") ?: 0L

                if (availableBeds <= 0) {
                    throw IllegalStateException("No beds left to accept this application")
                }

                txn.update(applicationsRef.document(applicationId), "status", "Accepted")
                txn.update(listingDoc, "availableBeds", availableBeds - 1)
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun rejectApplication(applicationId: String): Result<Unit> {
        return try {
            applicationsRef.document(applicationId).update("status", "Rejected").await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeStudentApplications(studentId: String): Flow<List<Application>> = callbackFlow {
        val subscription = applicationsRef
            .whereEqualTo("studentId", studentId)
            .orderBy("appliedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObjects(Application::class.java) ?: emptyList())
            }
        awaitClose { subscription.remove() }
    }

    fun observeOwnerApplications(ownerId: String): Flow<List<Application>> = callbackFlow {
        val subscription = applicationsRef
            .whereEqualTo("ownerId", ownerId)
            .orderBy("appliedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObjects(Application::class.java) ?: emptyList())
            }
        awaitClose { subscription.remove() }
    }
}
