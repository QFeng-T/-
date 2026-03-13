package com.tianhu.app.service

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.google.gson.Gson
import com.tianhu.app.util.ImageUtil
import com.tianhu.app.util.UserIdManager
import com.tianhu.app.database.DatabaseManager
import com.tianhu.app.database.entities.RecognitionRecord
import com.tianhu.app.database.enums.RecognitionType
import com.tianhu.app.database.enums.SyncStatus
import com.tianhu.app.network.ApiClient
import com.tianhu.app.network.ApiService
import com.tianhu.app.network.dto.PredictionResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

object CloudRecognitionService {
    private const val TAG = "CloudRecognitionService"
    private val gson = Gson()

    sealed class RecognitionResult {
        data class Success(
            val recordId: Long,
            val fruitName: String,
            val confidence: Float,
            val nutritionData: String?
        ) : RecognitionResult()

        data class Failure(val errorMessage: String) : RecognitionResult()
    }

    suspend fun recognizeWithCloud(
        context: Context,
        imageBitmap: Bitmap
    ): RecognitionResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "开始云端识别...")

            val imageFile = bitmapToFile(context, imageBitmap)
            if (imageFile == null) {
                return@withContext RecognitionResult.Failure("图片处理失败")
            }

            val requestFile = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", imageFile.name, requestFile)

            val apiService = ApiClient.getApiService(context)
            val response = apiService.uploadAndPredict(body)

            if (response.isSuccessful &amp;&amp; response.body()?.success == true) {
                val predictionResponse = response.body()?.data
                if (predictionResponse != null) {
                    val recordId = saveCloudRecognitionResult(
                        context = context,
                        predictionResponse = predictionResponse,
                        imageFile = imageFile
                    )

                    RecognitionResult.Success(
                        recordId = recordId,
                        fruitName = predictionResponse.prediction.label,
                        confidence = predictionResponse.prediction.confidence,
                        nutritionData = predictionResponse.prediction.nutrition_data?.let { gson.toJson(it) }
                    )
                } else {
                    RecognitionResult.Failure("识别结果为空")
                }
            } else {
                val errorMsg = response.errorBody()?.string() ?: "识别失败"
                Log.e(TAG, "云端识别失败: $errorMsg")
                RecognitionResult.Failure(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "云端识别异常", e)
            RecognitionResult.Failure("识别异常: ${e.message}")
        }
    }

    private fun bitmapToFile(context: Context, bitmap: Bitmap): File? {
        return try {
            val file = File(context.cacheDir, "cloud_recognize_${System.currentTimeMillis()}.jpg")
            val fos = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
            fos.flush()
            fos.close()
            file
        } catch (e: Exception) {
            Log.e(TAG, "Bitmap转文件失败", e)
            null
        }
    }

    private suspend fun saveCloudRecognitionResult(
        context: Context,
        predictionResponse: PredictionResponse,
        imageFile: File
    ): Long = withContext(Dispatchers.IO) {
        try {
            val userId = UserIdManager.getCurrentUserId(context)
            val db = DatabaseManager.getInstance(context)

            val savedImagePath = ImageUtil.copyImageToPrivateDir(context, imageFile.absolutePath)
                ?: imageFile.absolutePath

            val record = RecognitionRecord(
                id = 0,
                fruit_veg_name = predictionResponse.prediction.label,
                confidence = predictionResponse.prediction.confidence,
                image_uri = savedImagePath,
                nutrition_data = predictionResponse.prediction.nutrition_data?.let { gson.toJson(it) },
                create_time = System.currentTimeMillis(),
                is_collected = false,
                user_id = userId,
                recognition_type = RecognitionType.CLOUD,
                sync_status = SyncStatus.SYNCED,
                freshness_score = null,
                is_fresh = null
            )

            db.recognitionRecordDao().insert(record)
            record.id
        } catch (e: Exception) {
            Log.e(TAG, "保存云端识别结果失败", e)
            throw e
        }
    }
}
