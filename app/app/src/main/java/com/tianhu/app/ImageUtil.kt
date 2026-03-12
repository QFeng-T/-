package com.tianhu.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream

object ImageUtil {

    private const val MAX_IMAGE_SIZE = 200 * 1024 
    private const val MAX_WIDTH = 800
    private const val MAX_HEIGHT = 800
    private const val IMAGE_QUALITY = 80

    fun compressAndSaveImage(context: Context, bitmap: Bitmap): String? {
        return try {
            val compressedBitmap = compressBitmap(bitmap)
            
            val fileName = "recognition_${System.currentTimeMillis()}.jpg"
            val file = File(context.filesDir, fileName)
            
            FileOutputStream(file).use { out ->
                compressedBitmap.compress(Bitmap.CompressFormat.JPEG, IMAGE_QUALITY, out)
            }
            
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun compressAndSaveImage(context: Context, uri: Uri): String? {
        return try {
            val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            compressAndSaveImage(context, bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun compressImage(bitmap: Bitmap, maxWidth: Int = MAX_WIDTH, maxHeight: Int = MAX_HEIGHT, quality: Int = IMAGE_QUALITY): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        if (width <= maxWidth && height <= maxHeight) {
            return bitmap
        }
        
        val ratio = Math.min(
            maxWidth.toFloat() / width,
            maxHeight.toFloat() / height
        )
        
        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun compressBitmap(bitmap: Bitmap): Bitmap {
        return compressImage(bitmap)
    }

    fun getBitmapFromPath(imagePath: String): Bitmap? {
        return try {
            BitmapFactory.decodeFile(imagePath)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun deleteImage(imagePath: String): Boolean {
        return try {
            val file = File(imagePath)
            file.delete()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
