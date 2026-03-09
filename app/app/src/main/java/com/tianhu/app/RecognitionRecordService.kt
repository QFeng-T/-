package com.tianhu.app

import android.content.Context
import android.graphics.Bitmap
import com.tianhu.app.database.DatabaseManager
import com.tianhu.app.database.entities.RecognitionRecord
import com.tianhu.app.database.enums.RecognitionType
import com.tianhu.app.database.enums.SyncStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object RecognitionRecordService {

    suspend fun saveRecognitionRecord(
        context: Context,
        fruitVegName: String,
        confidence: Float,
        imageBitmap: Bitmap,
        nutritionData: String? = null
    ): Long? = withContext(Dispatchers.IO) {
        try {
            val userId = UserIdManager.getCurrentUserId(context)
            val imagePath = ImageUtil.compressAndSaveImage(context, imageBitmap) ?: return@withContext null
            
            val record = RecognitionRecord(
                id = 0,
                fruit_veg_name = fruitVegName,
                confidence = confidence,
                image_uri = imagePath,
                nutrition_data = nutritionData,
                create_time = System.currentTimeMillis(),
                is_collected = false,
                user_id = userId,
                recognition_type = RecognitionType.LOCAL,
                sync_status = SyncStatus.NOT_SYNCED
            )
            
            val db = DatabaseManager.getInstance(context)
            db.recognitionRecordDao().insert(record)
            
            record.id
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun saveRecognitionRecord(
        context: Context,
        fruitVegName: String,
        confidence: Float,
        imagePath: String,
        nutritionData: String? = null
    ): Long? = withContext(Dispatchers.IO) {
        try {
            val userId = UserIdManager.getCurrentUserId(context)
            
            val record = RecognitionRecord(
                id = 0,
                fruit_veg_name = fruitVegName,
                confidence = confidence,
                image_uri = imagePath,
                nutrition_data = nutritionData,
                create_time = System.currentTimeMillis(),
                is_collected = false,
                user_id = userId,
                recognition_type = RecognitionType.LOCAL,
                sync_status = SyncStatus.NOT_SYNCED
            )
            
            val db = DatabaseManager.getInstance(context)
            db.recognitionRecordDao().insert(record)
            
            record.id
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun toggleCollection(context: Context, recordId: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            val db = DatabaseManager.getInstance(context)
            val record = db.recognitionRecordDao().getById(recordId) ?: return@withContext false
            
            val updatedRecord = record.copy(is_collected = !record.is_collected)
            db.recognitionRecordDao().update(updatedRecord)
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun deleteRecord(context: Context, recordId: Long): Boolean = withContext(Dispatchers.IO) {
        try {
            val db = DatabaseManager.getInstance(context)
            val record = db.recognitionRecordDao().getById(recordId)
            
            record?.image_uri?.let {
                ImageUtil.deleteImage(it)
            }
            
            db.recognitionRecordDao().deleteById(recordId)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getAllRecords(context: Context): List<RecognitionRecord> = withContext(Dispatchers.IO) {
        try {
            val userId = UserIdManager.getCurrentUserId(context)
            val db = DatabaseManager.getInstance(context)
            db.recognitionRecordDao().getAllByUserId(userId)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getCollectedRecords(context: Context): List<RecognitionRecord> = withContext(Dispatchers.IO) {
        try {
            val userId = UserIdManager.getCurrentUserId(context)
            val db = DatabaseManager.getInstance(context)
            db.recognitionRecordDao().getCollectedByUserId(userId)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun deleteAllRecords(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val userId = UserIdManager.getCurrentUserId(context)
            val db = DatabaseManager.getInstance(context)
            val records = db.recognitionRecordDao().getAllByUserId(userId)
            
            records.forEach { record ->
                record.image_uri?.let {
                    ImageUtil.deleteImage(it)
                }
            }
            
            db.recognitionRecordDao().deleteByUserId(userId)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
