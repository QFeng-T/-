package com.tianhu.app.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tianhu.app.database.enums.RecognitionType
import com.tianhu.app.database.enums.SyncStatus

@Entity(
    tableName = "recognition_record",
    indices = [
        Index(name = "idx_user_time", value = ["user_id", "create_time"]),
        Index(name = "idx_user_collect", value = ["user_id", "is_collected"]),
        Index(name = "idx_sync_status", value = ["user_id", "sync_status"])
    ]
)
data class RecognitionRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val fruit_veg_name: String,
    val confidence: Float,
    val image_uri: String,
    val nutrition_data: String?,
    val create_time: Long,
    val is_collected: Boolean,
    val user_id: String,
    val recognition_type: RecognitionType,
    val sync_status: SyncStatus,
    val freshness_score: Float? = null,
    val is_fresh: Boolean? = null
)