package com.tianhu.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object ImageUtil {
    private const val TAG = "ImageUtil"

    private const val MAX_IMAGE_SIZE = 200 * 1024
    private const val MAX_WIDTH = 800
    private const val MAX_HEIGHT = 800
    private const val IMAGE_QUALITY = 80

    suspend fun compressAndSaveImage(context: Context, bitmap: Bitmap): String? = withContext(Dispatchers.IO) {
        try {
            val compressedBitmap = compressImage(bitmap)

            val fileName = "recognition_${System.currentTimeMillis()}.jpg"
            val file = File(context.filesDir, fileName)

            FileOutputStream(file).use { out ->
                if (!compressedBitmap.compress(Bitmap.CompressFormat.JPEG, IMAGE_QUALITY, out)) {
                    Log.e(TAG, "Bitmap压缩失败")
                    return@withContext null
                }
            }

            if (compressedBitmap != bitmap) {
                compressedBitmap.recycle()
            }

            file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "压缩并保存图片失败", e)
            null
        }
    }

    suspend fun compressAndSaveImage(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            compressAndSaveImage(context, bitmap)
        } catch (e: Exception) {
            Log.e(TAG, "从Uri压缩并保存图片失败", e)
            null
        }
    }

    fun compressImage(
        bitmap: Bitmap,
        maxWidth: Int = MAX_WIDTH,
        maxHeight: Int = MAX_HEIGHT,
        quality: Int = IMAGE_QUALITY
    ): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxWidth && height <= maxHeight) {
            return bitmap
        }

        val ratio = Math.min(
            maxWidth.toFloat() / width,
            maxHeight.toFloat() / height
        )

        val newWidth = (width * ratio).toInt().coerceAtLeast(1)
        val newHeight = (height * ratio).toInt().coerceAtLeast(1)

        return try {
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } catch (e: Exception) {
            Log.e(TAG, "图片缩放失败", e)
            bitmap
        }
    }

    fun bitmapToBase64(bitmap: Bitmap, quality: Int = IMAGE_QUALITY): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    fun getBitmapFromPath(imagePath: String): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            BitmapFactory.decodeFile(imagePath, options)
        } catch (e: Exception) {
            Log.e(TAG, "从路径获取Bitmap失败", e)
            null
        }
    }

    fun deleteImage(imagePath: String): Boolean {
        return try {
            val file = File(imagePath)
            if (file.exists()) {
                file.delete()
            } else {
                Log.w(TAG, "文件不存在，无法删除: $imagePath")
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "删除图片失败", e)
            false
        }
    }

    suspend fun copyImageToPrivateDir(context: Context, sourcePath: String): String? = withContext(Dispatchers.IO) {
        try {
            val sourceFile = File(sourcePath)
            if (!sourceFile.exists()) {
                Log.e(TAG, "源文件不存在: $sourcePath")
                return@withContext null
            }

            val fileName = "record_${System.currentTimeMillis()}.jpg"
            val destFile = File(context.filesDir, fileName)

            sourceFile.copyTo(destFile, overwrite = true)
            destFile.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "复制图片到私有目录失败", e)
            null
        }
    }

    fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height, width) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }
}
