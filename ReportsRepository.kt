package com.example.saf2.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.example.saf2.data.model.Report
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ReportsRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val reportsRef = firestore.collection("reports")

    fun observeReportsForListings(listingIds: List<String>): Flow<List<Report>> = callbackFlow {
        if (listingIds.isEmpty()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val subscription = reportsRef
            .whereIn("listingId", listingIds.take(10))
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObjects(Report::class.java) ?: emptyList())
            }
        awaitClose { subscription.remove() }
    }

    fun observeReportsForReporter(reporterId: String): Flow<List<Report>> = callbackFlow {
        val subscription = reportsRef
            .whereEqualTo("reporterId", reporterId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObjects(Report::class.java) ?: emptyList())
            }
        awaitClose { subscription.remove() }
    }

    suspend fun submitReport(report: Report): Result<Unit> {
        return try {
            reportsRef.add(report).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resolveReport(reportId: String): Result<Unit> {
        return try {
            reportsRef.document(reportId).update("status", "Resolved").await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
