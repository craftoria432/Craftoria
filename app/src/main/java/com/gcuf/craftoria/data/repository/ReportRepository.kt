package com.gcuf.craftoria.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.gcuf.craftoria.data.model.Report
import com.gcuf.craftoria.data.model.ReportType
import com.gcuf.craftoria.data.model.toMap
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ReportRepository {
    private val db = FirebaseFirestore.getInstance()
    private val reportsCollection = db.collection("reports")

    companion object {
        private const val TAG = "ReportRepository"
    }

    /**
     * Submit a new report
     */
    suspend fun submitReport(
        reportType: ReportType,
        reporterId: String,
        reporterName: String,
        reportedEntityId: String,
        reportedEntityName: String,
        reason: String,
        description: String
    ): Result<String> {
        return try {
            val report = Report(
                type = reportType,
                reporterId = reporterId,
                reporterName = reporterName,
                reportedEntityId = reportedEntityId,
                reportedEntityName = reportedEntityName,
                reason = reason,
                description = description
            )

            val docRef = reportsCollection.add(report.toMap()).await()
            Log.d(TAG, "Report submitted successfully: ${docRef.id}")
            Result.success(docRef.id)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to submit report", e)
            Result.failure(e)
        }
    }

    /**
     * Get user's submitted reports
     */
    suspend fun getUserReports(userId: String): Result<List<Report>> {
        return try {
            val snapshot = reportsCollection
                .whereEqualTo("reporter_id", userId)
                .orderBy("created_at", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()

            val reports = snapshot.documents.mapNotNull { doc ->
                try {
                    parseReport(doc.id, doc.data ?: emptyMap())
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing report", e)
                    null
                }
            }

            Log.d(TAG, "Fetched ${reports.size} reports for user")
            Result.success(reports)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch user reports", e)
            Result.failure(e)
        }
    }

    /**
     * Observe user's submitted reports in real-time (recommended for production UI)
     */
    fun observeUserReports(userId: String): Flow<Result<List<Report>>> = callbackFlow {
        Log.d(TAG, "Subscribing to real-time reports for user: $userId")

        val registration: ListenerRegistration = reportsCollection
            .whereEqualTo("reporter_id", userId)
            .orderBy("created_at", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e(TAG, "Reports snapshot listener error", e)
                    trySend(Result.failure(e))
                    return@addSnapshotListener
                }

                if (snapshot == null) {
                    trySend(Result.success(emptyList()))
                    return@addSnapshotListener
                }

                val reports = snapshot.documents.mapNotNull { doc ->
                    try {
                        parseReport(doc.id, doc.data ?: emptyMap())
                    } catch (ex: Exception) {
                        Log.e(TAG, "Error parsing report (realtime)", ex)
                        null
                    }
                }

                trySend(Result.success(reports))
            }

        awaitClose {
            Log.d(TAG, "Unsubscribing from real-time reports for user: $userId")
            registration.remove()
        }
    }

    private fun parseReport(id: String, data: Map<String, Any>): Report {
        val typeStr = data["type"] as? String ?: "seller"
        val statusStr = data["status"] as? String ?: "New"

        return Report(
            id = id,
            type = when (typeStr) {
                "product" -> ReportType.PRODUCT
                "seller" -> ReportType.SELLER
                "buyer" -> ReportType.BUYER
                "technical" -> ReportType.TECHNICAL
                else -> ReportType.SELLER
            },
            reporterId = data["reporter_id"] as? String ?: "",
            reporterName = data["reporter_name"] as? String ?: "",
            reportedEntityId = data["reported_entity_id"] as? String ?: "",
            reportedEntityName = data["reported_entity_name"] as? String ?: "",
            reason = data["reason"] as? String ?: "",
            description = data["description"] as? String ?: "",
            status = when (statusStr) {
                "New" -> com.gcuf.craftoria.data.model.ReportStatus.NEW
                "Under Review" -> com.gcuf.craftoria.data.model.ReportStatus.UNDER_REVIEW
                "Resolved" -> com.gcuf.craftoria.data.model.ReportStatus.RESOLVED
                else -> com.gcuf.craftoria.data.model.ReportStatus.NEW
            },
            createdAt = (data["created_at"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: System.currentTimeMillis(),
            updatedAt = (data["updated_at"] as? com.google.firebase.Timestamp)?.toDate()?.time ?: System.currentTimeMillis()
        )
    }
}
