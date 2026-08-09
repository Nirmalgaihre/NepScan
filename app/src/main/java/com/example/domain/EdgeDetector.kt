package com.example.domain

import android.graphics.Bitmap
import android.graphics.PointF

object EdgeDetector {

    /**
     * Estimates document corners on a bitmap.
     * Returns normalized coordinates (0.0 .. 1.0) for TopLeft, TopRight, BottomRight, BottomLeft.
     */
    fun detectDocumentCorners(bitmap: Bitmap): CropCorners {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= 0 || height <= 0) {
            return CropCorners()
        }

        // Downsample to small thumbnail for fast pixel scanning
        val sampleSize = 100
        val aspect = height.toFloat() / width.toFloat()
        val sw = sampleSize
        val sh = (sampleSize * aspect).toInt().coerceAtLeast(10)

        val small = try {
            Bitmap.createScaledBitmap(bitmap, sw, sh, false)
        } catch (e: Exception) {
            return CropCorners()
        }

        // Scan row/column brightness to locate document margins
        var topMargin = 0
        var bottomMargin = sh - 1
        var leftMargin = 0
        var rightMargin = sw - 1

        val pixels = IntArray(sw * sh)
        small.getPixels(pixels, 0, sw, 0, 0, sw, sh)

        // Find average brightness of center vs edges
        fun brightness(color: Int): Float {
            val r = (color shr 16) and 0xFF
            val g = (color shr 8) and 0xFF
            val b = color and 0xFF
            return (0.299f * r + 0.587f * g + 0.114f * b)
        }

        // Scan from top
        for (y in 0 until sh / 3) {
            var rowSum = 0f
            for (x in 0 until sw) {
                rowSum += brightness(pixels[y * sw + x])
            }
            val avg = rowSum / sw
            if (avg > 80f) {
                topMargin = y
                break
            }
        }

        // Scan from bottom
        for (y in sh - 1 downTo (sh * 2 / 3)) {
            var rowSum = 0f
            for (x in 0 until sw) {
                rowSum += brightness(pixels[y * sw + x])
            }
            val avg = rowSum / sw
            if (avg > 80f) {
                bottomMargin = y
                break
            }
        }

        // Scan from left
        for (x in 0 until sw / 3) {
            var colSum = 0f
            for (y in 0 until sh) {
                colSum += brightness(pixels[y * sw + x])
            }
            val avg = colSum / sh
            if (avg > 80f) {
                leftMargin = x
                break
            }
        }

        // Scan from right
        for (x in sw - 1 downTo (sw * 2 / 3)) {
            var colSum = 0f
            for (y in 0 until sh) {
                colSum += brightness(pixels[y * sw + x])
            }
            val avg = colSum / sh
            if (avg > 80f) {
                rightMargin = x
                break
            }
        }

        // Convert to normalized floats with safety margins
        val normTop = (topMargin.toFloat() / sh).coerceIn(0.02f, 0.25f)
        val normBottom = (bottomMargin.toFloat() / sh).coerceIn(0.75f, 0.98f)
        val normLeft = (leftMargin.toFloat() / sw).coerceIn(0.02f, 0.25f)
        val normRight = (rightMargin.toFloat() / sw).coerceIn(0.75f, 0.98f)

        return CropCorners(
            topLeft = PointF(normLeft, normTop),
            topRight = PointF(normRight, normTop),
            bottomRight = PointF(normRight, normBottom),
            bottomLeft = PointF(normLeft, normBottom)
        )
    }
}
