package com.tianhu.app.network.dto

data class SyncUploadRequest(
    val local_records: List<RecognitionRecordDto>,
    val last_sync_time: Long
)

data class SyncDownloadRequest(
    val last_sync_time: Long
)

data class SyncResponse(
    val server_records: List<RecognitionRecordDto>,
    val latest_sync_time: Long,
    val conflicts: List<ConflictRecord>?
)

data class ConflictRecord(
    val local_record: RecognitionRecordDto,
    val server_record: RecognitionRecordDto
)

data class SyncStatusResponse(
    val pending_count: Int,
    val last_sync_time: Long?,
    val sync_status: String
)
