package com.gcuf.craftoria.data.model

import com.google.firebase.firestore.PropertyName

data class Report(
    val id: String = "",
    @PropertyName("type")
    val type: ReportType = ReportType.SELLER,
    @PropertyName("reporter_id")
    val reporterId: String = "",
    @PropertyName("reporter_name")
    val reporterName: String = "",
    @PropertyName("reported_entity_id")
    val reportedEntityId: String = "",
    @PropertyName("reported_entity_name")
    val reportedEntityName: String = "",
    @PropertyName("reason")
    val reason: String = "",
    @PropertyName("description")
    val description: String = "",
    @PropertyName("status")
    val status: ReportStatus = ReportStatus.NEW,
    @PropertyName("created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @PropertyName("updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)

enum class ReportType {
    @PropertyName("product")
    PRODUCT,
    @PropertyName("seller")
    SELLER,
    @PropertyName("buyer")
    BUYER,
    @PropertyName("technical")
    TECHNICAL
}

enum class ReportStatus {
    @PropertyName("New")
    NEW,
    @PropertyName("Under Review")
    UNDER_REVIEW,
    @PropertyName("Resolved")
    RESOLVED
}

fun Report.toMap(): Map<String, Any> = mapOf(
    "type" to when (type) {
        ReportType.PRODUCT -> "product"
        ReportType.SELLER -> "seller"
        ReportType.BUYER -> "buyer"
        ReportType.TECHNICAL -> "technical"
    },
    "reporter_id" to reporterId,
    "reporter_name" to reporterName,
    "reported_entity_id" to reportedEntityId,
    "reported_entity_name" to reportedEntityName,
    "reason" to reason,
    "description" to description,
    "status" to when (status) {
        ReportStatus.NEW -> "New"
        ReportStatus.UNDER_REVIEW -> "Under Review"
        ReportStatus.RESOLVED -> "Resolved"
    },
    "created_at" to createdAt,
    "updated_at" to updatedAt
)
