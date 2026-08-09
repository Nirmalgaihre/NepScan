package com.example.domain

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max

data class CropCorners(
    val topLeft: PointF = PointF(0.05f, 0.05f),
    val topRight: PointF = PointF(0.95f, 0.05f),
    val bottomRight: PointF = PointF(0.95f, 0.95f),
    val bottomLeft: PointF = PointF(0.05f, 0.95f)
)

enum class FilterType {
    ORIGINAL,
    AUTO,
    COLOR,
    GRAYSCALE,
    BLACK_AND_WHITE
}

object ImageProcessor {

    /**
     * Applies perspective correction using Matrix.setPolyToPoly
     * Map normalized 4 corner coordinates (0..1) of source bitmap to a flat rectangle.
     */
    fun perspectiveWarp(
        source: Bitmap,
        corners: CropCorners,
        rotationDegrees: Int = 0
    ): Bitmap {
        var srcBitmap = source
        if (rotationDegrees != 0) {
            val rotateMatrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            srcBitmap = Bitmap.createBitmap(source, 0, 0, source.width, source.height, rotateMatrix, true)
        }

        val w = srcBitmap.width.toFloat()
        val h = srcBitmap.height.toFloat()

        val tlX = corners.topLeft.x * w
        val tlY = corners.topLeft.y * h
        val trX = corners.topRight.x * w
        val trY = corners.topRight.y * h
        val brX = corners.bottomRight.x * w
        val brY = corners.bottomRight.y * h
        val blX = corners.bottomLeft.x * w
        val blY = corners.bottomLeft.y * h

        // Calculate target dimensions based on corner distances
        val topWidth = hypot(trX - tlX, trY - tlY)
        val bottomWidth = hypot(brX - blX, brY - blY)
        val targetWidth = max(topWidth, bottomWidth).toInt().coerceAtLeast(100)

        val leftHeight = hypot(blX - tlX, blY - tlY)
        val rightHeight = hypot(brX - trX, brY - trY)
        val targetHeight = max(leftHeight, rightHeight).toInt().coerceAtLeast(100)

        val srcPoints = floatArrayOf(
            tlX, tlY,
            trX, trY,
            brX, brY,
            blX, blY
        )

        val dstPoints = floatArrayOf(
            0f, 0f,
            targetWidth.toFloat(), 0f,
            targetWidth.toFloat(), targetHeight.toFloat(),
            0f, targetHeight.toFloat()
        )

        val polyMatrix = Matrix()
        val success = polyMatrix.setPolyToPoly(srcPoints, 0, dstPoints, 0, 4)

        if (!success) {
            return srcBitmap
        }

        val output = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(srcBitmap, polyMatrix, paint)

        return output
    }

    /**
     * Applies filter effects (ORIGINAL, AUTO, COLOR, GRAYSCALE, BLACK_AND_WHITE)
     */
    fun applyFilter(bitmap: Bitmap, filterType: FilterType): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        when (filterType) {
            FilterType.ORIGINAL -> {
                canvas.drawBitmap(bitmap, 0f, 0f, paint)
                return output
            }
            FilterType.AUTO -> {
                // Boost contrast slightly and enhance document crispness
                val cm = ColorMatrix(
                    floatArrayOf(
                        1.2f, 0f, 0f, 0f, -10f,
                        0f, 1.2f, 0f, 0f, -10f,
                        0f, 0f, 1.2f, 0f, -10f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                paint.colorFilter = ColorMatrixColorFilter(cm)
                canvas.drawBitmap(bitmap, 0f, 0f, paint)
                return output
            }
            FilterType.COLOR -> {
                // Enhanced vibrant color scan
                val cm = ColorMatrix()
                cm.setSaturation(1.3f)
                val cmContrast = ColorMatrix(
                    floatArrayOf(
                        1.15f, 0f, 0f, 0f, -5f,
                        0f, 1.15f, 0f, 0f, -5f,
                        0f, 0f, 1.15f, 0f, -5f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                cm.postConcat(cmContrast)
                paint.colorFilter = ColorMatrixColorFilter(cm)
                canvas.drawBitmap(bitmap, 0f, 0f, paint)
                return output
            }
            FilterType.GRAYSCALE -> {
                val cm = ColorMatrix()
                cm.setSaturation(0f)
                val cmContrast = ColorMatrix(
                    floatArrayOf(
                        1.3f, 0f, 0f, 0f, -20f,
                        0f, 1.3f, 0f, 0f, -20f,
                        0f, 0f, 1.3f, 0f, -20f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                cm.postConcat(cmContrast)
                paint.colorFilter = ColorMatrixColorFilter(cm)
                canvas.drawBitmap(bitmap, 0f, 0f, paint)
                return output
            }
            FilterType.BLACK_AND_WHITE -> {
                // First draw grayscale
                val cm = ColorMatrix()
                cm.setSaturation(0f)
                val highContrast = ColorMatrix(
                    floatArrayOf(
                        2.5f, 0f, 0f, 0f, -150f,
                        0f, 2.5f, 0f, 0f, -150f,
                        0f, 0f, 2.5f, 0f, -150f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                cm.postConcat(highContrast)
                paint.colorFilter = ColorMatrixColorFilter(cm)
                canvas.drawBitmap(bitmap, 0f, 0f, paint)
                return output
            }
        }
    }

    /**
     * Rotate bitmap by degrees (0, 90, 180, 270)
     */
    fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        if (degrees % 360f == 0f) return bitmap
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }
}
