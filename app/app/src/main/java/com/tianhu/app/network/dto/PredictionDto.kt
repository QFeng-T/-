package com.tianhu.app.network.dto

data class PredictionRequest(
    val image_base64: String? = null
)

data class PredictionResponse(
    val prediction_id: Long,
    val prediction: PredictionData,
    val file_path: String,
    val file_url: String?
)

data class PredictionData(
    val class_id: Int,
    val confidence: Float,
    val label: String,
    val nutrition_data: Map<String, Any>?
)

data class RecognitionRecordDto(
    val id: Long,
    val user_id: Int,
    val fruit_veg_name: String,
    val confidence: Float,
    val class_id: Int,
    val image_path: String,
    val image_url: String?,
    val nutrition_data: Map<String, Any>?,
    val is_collected: Boolean,
    val recognition_type: String,
    val sync_status: String,
    val deleted: Boolean,
    val created_at: String,
    val updated_at: String
)

data class SyncRecordRequest(
    val records: List<RecognitionRecordDto>
)

data class SyncRecordResponse(
    val synced_ids: List<Long>,
    val failed_ids: List<Long>
)
