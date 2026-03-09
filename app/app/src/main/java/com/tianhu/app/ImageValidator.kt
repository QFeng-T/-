package com.tianhu.app

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.abs

object ImageValidator {

    private const val MIN_WIDTH = 480
    private const val MIN_HEIGHT = 480
    private const val MAX_WIDTH = 2048
    private const val MAX_HEIGHT = 2048
    private const val BLUR_THRESHOLD = 100.0

    sealed class ValidationResult {
        object Success : ValidationResult()
        data class Failure(val errorType: ErrorType, val message: String) : ValidationResult()
    }

    enum class ErrorType {
        SIZE_TOO_SMALL,
        SIZE_TOO_LARGE,
        TOO_BLURRY,
        INVALID_CONTENT
    }

    fun validateImage(bitmap: Bitmap): ValidationResult {
        val sizeResult = validateSize(bitmap)
        if (sizeResult is ValidationResult.Failure) {
            return sizeResult
        }

        val blurResult = validateBlur(bitmap)
        if (blurResult is ValidationResult.Failure) {
            return blurResult
        }

        return ValidationResult.Success
    }

    private fun validateSize(bitmap: Bitmap): ValidationResult {
        val width = bitmap.width
        val height = bitmap.height

        if (width < MIN_WIDTH || height < MIN_HEIGHT) {
            return ValidationResult.Failure(
                ErrorType.SIZE_TOO_SMALL,
                "图片尺寸过小，请调整角度，确保图片至少 ${MIN_WIDTH}×${MIN_HEIGHT} 像素"
            )
        }

        if (width > MAX_WIDTH || height > MAX_HEIGHT) {
            return ValidationResult.Failure(
                ErrorType.SIZE_TOO_LARGE,
                "图片尺寸过大，请调整图片，确保图片不超过 ${MAX_WIDTH}×${MAX_HEIGHT} 像素"
            )
        }

        return ValidationResult.Success
    }

    private fun validateBlur(bitmap: Bitmap): ValidationResult {
        val grayBitmap = toGrayscale(bitmap)
        val laplacian = calculateLaplacian(grayBitmap)
        val variance = calculateVariance(laplacian)

        if (variance < BLUR_THRESHOLD) {
            return ValidationResult.Failure(
                ErrorType.TOO_BLURRY,
                "图片模糊，请重新拍摄，确保光线充足、对焦清晰"
            )
        }

        return ValidationResult.Success
    }

    private fun toGrayscale(bitmap: Bitmap): Bitmap {
        val grayBitmap = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(grayBitmap)
        val paint = android.graphics.Paint()
        val colorMatrix = android.graphics.ColorMatrix()
        colorMatrix.setSaturation(0f)
        val colorMatrixFilter = android.graphics.ColorMatrixColorFilter(colorMatrix)
        paint.colorFilter = colorMatrixFilter
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        return grayBitmap
    }

    private fun calculateLaplacian(bitmap: Bitmap): Array<IntArray> {
        val width = bitmap.width
        val height = bitmap.height
        val laplacian = Array(height) { IntArray(width) }

        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                val pixel = bitmap.getPixel(x, y)
                val gray = Color.red(pixel)
                
                val top = Color.red(bitmap.getPixel(x, y - 1))
                val bottom = Color.red(bitmap.getPixel(x, y + 1))
                val left = Color.red(bitmap.getPixel(x - 1, y))
                val right = Color.red(bitmap.getPixel(x + 1, y))
                
                val laplacianValue = (top + bottom + left + right - 4 * gray)
                laplacian[y][x] = laplacianValue
            }
        }

        return laplacian
    }

    private fun calculateVariance(laplacian: Array<IntArray>): Double {
        var sum = 0.0
        var count = 0

        for (y in laplacian.indices) {
            for (x in laplacian[y].indices) {
                sum += laplacian[y][x]
                count++
            }
        }

        val mean = sum / count
        var varianceSum = 0.0

        for (y in laplacian.indices) {
            for (x in laplacian[y].indices) {
                val diff = laplacian[y][x] - mean
                varianceSum += diff * diff
            }
        }

        return varianceSum / count
    }
}
