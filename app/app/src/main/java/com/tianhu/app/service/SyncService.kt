package com.tianhu.app.service

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.tianhu.app.util.UserIdManager
import com.tianhu.app.database.DatabaseManager
import com.tianhu.app.database.entities.RecognitionRecord
import com.tianhu.app.database.enums.RecognitionType
import com.tianhu.app.database.enums.SyncStatus
import com.tianhu.app.network.ApiClient
import com.tianhu.app.network.ApiService
import com.tianhu.app.network.dto.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

object SyncService {
    private const val TAG = "SyncService"
    private const val PREFS_NAME = "sync_prefs"
    private const val KEY_LAST_SYNC_TIME = "last_sync_time"

    private val gson = Gson()

    data class SyncResult(
        val success: Boolean,
        val syncedCount: Int = 0,
        val failedCount: Int = 0,
        val message: String = ""
    )

    suspend fun syncData(context: Context): SyncResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "开始数据同步...")

            val uploadResult = uploadLocalRecords(context)
            val downloadResult = downloadCloudRecords(context)

            saveLastSyncTime(context)

            val totalSynced = uploadResult.syncedCount + downloadResult.syncedCount
            val totalFailed = uploadResult.failedCount + downloadResult.failedCount

            SyncResult(
                success = uploadResult.success &amp;&amp; downloadResult.success,
                syncedCount = totalSynced,
                failedCount = totalFailed,
                message = "同步完成：上传 ${uploadResult.syncedCount} 条，下载 ${downloadResult.syncedCount} 条"
            )
        } catch (e: Exception) {
            Log.e(TAG, "数据同步失败", e)
            SyncResult(
                success = false,
                message = "同步失败: ${e.message}"
            )
        }
    }

    private suspend fun uploadLocalRecords(context: Context): SyncResult = withContext(Dispatchers.IO) {
        try {
            val userId = UserIdManager.getCurrentUserId(context)
            val db = DatabaseManager.getInstance(context)
            val recordsToSync = db.recognitionRecordDao().getBySyncStatus(userId, SyncStatus.NOT_SYNCED)

            if (recordsToSync.isEmpty()) {
                return@withContext SyncResult(
                    success = true,
                    message = "没有需要上传的数据"
                )
            }

            Log.d(TAG, "准备上传 ${recordsToSync.size} 条记录到云端")

            val localRecordsDto = recordsToSync.map { recordToDto(it) }
            val lastSyncTime = getLastSyncTime(context)

            val request = SyncUploadRequest(
                local_records = localRecordsDto,
                last_sync_time = lastSyncTime
            )

            val apiService = ApiClient.getApiService(context)
            val response = apiService.uploadSyncData(request)

            if (response.isSuccessful &amp;&amp; response.body()?.success == true) {
                val syncResponse = response.body()?.data
                val syncedIds = syncResponse?.synced_ids ?: emptyList()
                val failedIds = syncResponse?.failed_ids ?: emptyList()

                recordsToSync.forEach { record -&gt;
                    if (syncedIds.contains(record.id)) {
                        db.recognitionRecordDao().updateSyncStatus(record.id, SyncStatus.SYNCED)
                    } else if (failedIds.contains(record.id)) {
                        db.recognitionRecordDao().updateSyncStatus(record.id, SyncStatus.SYNC_FAILED)
                    }
                }

                SyncResult(
                    success = failedIds.isEmpty(),
                    syncedCount = syncedIds.size,
                    failedCount = failedIds.size,
                    message = "上传完成：成功 ${syncedIds.size} 条，失败 ${failedIds.size} 条"
                )
            } else {
                Log.e(TAG, "上传失败: ${response.code()} - ${response.message()}")
                recordsToSync.forEach { record -&gt;
                    db.recognitionRecordDao().updateSyncStatus(record.id, SyncStatus.SYNC_FAILED)
                }
                SyncResult(
                    success = false,
                    failedCount = recordsToSync.size,
                    message = "上传失败: ${response.message()}"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "上传记录到云端失败", e)
            SyncResult(
                success = false,
                message = "上传失败: ${e.message}"
            )
        }
    }

    private suspend fun downloadCloudRecords(context: Context): SyncResult = withContext(Dispatchers.IO) {
        try {
            val lastSyncTime = getLastSyncTime(context)
            val request = SyncDownloadRequest(last_sync_time = lastSyncTime)

            val apiService = ApiClient.getApiService(context)
            val response = apiService.downloadSyncData(request)

            if (response.isSuccessful &amp;&amp; response.body()?.success == true) {
                val syncResponse = response.body()?.data
                val serverRecords = syncResponse?.server_records ?: emptyList()

                if (serverRecords.isEmpty()) {
                    return@withContext SyncResult(
                        success = true,
                        message = "没有需要下载的新数据"
                    )
                }

                val db = DatabaseManager.getInstance(context)
                var savedCount = 0

                serverRecords.forEach { dto -&gt;
                    try {
                        val existingRecord = db.recognitionRecordDao().getById(dto.id)
                        if (existingRecord == null) {
                            val record = dtoToRecord(dto)
                            db.recognitionRecordDao().insert(record)
                            savedCount++
                        } else {
                            if (dto.updated_at &gt; existingRecord.create_time.toString()) {
                                val updatedRecord = dtoToRecord(dto).copy(id = existingRecord.id)
                                db.recognitionRecordDao().update(updatedRecord)
                                savedCount++
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "保存云端记录失败: ${dto.id}", e)
                    }
                }

                SyncResult(
                    success = true,
                    syncedCount = savedCount,
                    message = "下载完成：保存 $savedCount 条记录"
                )
            } else {
                Log.e(TAG, "下载失败: ${response.code()} - ${response.message()}")
                SyncResult(
                    success = false,
                    message = "下载失败: ${response.message()}"
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "从云端下载数据失败", e)
            SyncResult(
                success = false,
                message = "下载失败: ${e.message}"
            )
        }
    }

    fun getLastSyncTime(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_LAST_SYNC_TIME, 0L)
    }

    private fun recordToDto(record: RecognitionRecord): RecognitionRecordDto {
        return RecognitionRecordDto(
            id = record.id,
            user_id = record.user_id.toIntOrNull() ?: 0,
            fruit_veg_name = record.fruit_veg_name,
            confidence = record.confidence,
            class_id = 0,
            image_path = record.image_uri,
            image_url = null,
            nutrition_data = record.nutrition_data?.let { parseNutritionData(it) },
            is_collected = record.is_collected,
            recognition_type = when (record.recognition_type) {
                RecognitionType.LOCAL -&gt; "local"
                RecognitionType.CLOUD -&gt; "cloud"
                RecognitionType.FRUIT_DETECTION -&gt; "fruit_detection"
                RecognitionType.FRESHNESS_DETECTION -&gt; "freshness_detection"
            },
            sync_status = when (record.sync_status) {
                SyncStatus.NOT_SYNCED -&gt; "pending"
                SyncStatus.SYNCED -&gt; "synced"
                SyncStatus.SYNC_FAILED -&gt; "failed"
            },
            deleted = false,
            created_at = record.create_time.toString(),
            updated_at = System.currentTimeMillis().toString()
        )
    }

    private fun dtoToRecord(dto: RecognitionRecordDto): RecognitionRecord {
        return RecognitionRecord(
            id = dto.id,
            fruit_veg_name = dto.fruit_veg_name,
            confidence = dto.confidence,
            image_uri = dto.image_path,
            nutrition_data = dto.nutrition_data?.let { gson.toJson(it) },
            create_time = dto.created_at.toLongOrNull() ?: System.currentTimeMillis(),
            is_collected = dto.is_collected,
            user_id = dto.user_id.toString(),
            recognition_type = when (dto.recognition_type) {
                "local" -&gt; RecognitionType.LOCAL
                "cloud" -&gt; RecognitionType.CLOUD
                "fruit_detection" -&gt; RecognitionType.FRUIT_DETECTION
                "freshness_detection" -&gt; RecognitionType.FRESHNESS_DETECTION
                else -&gt; RecognitionType.LOCAL
            },
            sync_status = when (dto.sync_status) {
                "pending" -&gt; SyncStatus.NOT_SYNCED
                "synced" -&gt; SyncStatus.SYNCED
                "failed" -&gt; SyncStatus.SYNC_FAILED
                else -&gt; SyncStatus.NOT_SYNCED
            },
            freshness_score = null,
            is_fresh = null
        )
    }

    private fun parseNutritionData(jsonString: String): Map&lt;String, Any&gt;? {
        return try {
            val json = JSONObject(jsonString)
            val map = mutableMapOf&lt;String, Any&gt;()
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key] = json.get(key)
            }
            map
        } catch (e: Exception) {
            Log.e(TAG, "解析营养数据失败", e)
            null
        }
    }

    private fun saveLastSyncTime(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_LAST_SYNC_TIME, System.currentTimeMillis()).apply()
    }

    suspend fun retryFailedSync(context: Context): SyncResult = withContext(Dispatchers.IO) {
        try {
            val userId = UserIdManager.getCurrentUserId(context)
            val db = DatabaseManager.getInstance(context)
            val failedRecords = db.recognitionRecordDao().getBySyncStatus(userId, SyncStatus.SYNC_FAILED)

            failedRecords.forEach { record -&gt;
                db.recognitionRecordDao().updateSyncStatus(record.id, SyncStatus.NOT_SYNCED)
            }

            syncData(context)
        } catch (e: Exception) {
            Log.e(TAG, "重试同步失败", e)
            SyncResult(
                success = false,
                message = "重试失败: ${e.message}"
            )
        }
    }

    suspend fun getSyncStats(context: Context): Map&lt;String, Int&gt; = withContext(Dispatchers.IO) {
        val userId = UserIdManager.getCurrentUserId(context)
        val db = DatabaseManager.getInstance(context)

        val notSynced = db.recognitionRecordDao().getBySyncStatus(userId, SyncStatus.NOT_SYNCED).size
        val synced = db.recognitionRecordDao().getBySyncStatus(userId, SyncStatus.SYNCED).size
        val syncFailed = db.recognitionRecordDao().getBySyncStatus(userId, SyncStatus.SYNC_FAILED).size

        mapOf(
            "notSynced" to notSynced,
            "synced" to synced,
            "syncFailed" to syncFailed
        )
    }
}
