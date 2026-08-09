package com.example.ui.components

import android.graphics.PointF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.example.domain.CropCorners
import kotlin.math.hypot

@Composable
fun CornerAdjusterOverlay(
    corners: CropCorners,
    onCornersChanged: (CropCorners) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeCornerIndex by remember { mutableStateOf<Int?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(corners) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()

                        val pts = listOf(
                            Offset(corners.topLeft.x * w, corners.topLeft.y * h),
                            Offset(corners.topRight.x * w, corners.topRight.y * h),
                            Offset(corners.bottomRight.x * w, corners.bottomRight.y * h),
                            Offset(corners.bottomLeft.x * w, corners.bottomLeft.y * h)
                        )

                        // Find closest corner within touch radius
                        var closestIdx: Int? = null
                        var minDistance = 120f // 120px touch target radius
                        pts.forEachIndexed { index, pt ->
                            val dist = hypot(offset.x - pt.x, offset.y - pt.y)
                            if (dist < minDistance) {
                                minDistance = dist
                                closestIdx = index
                            }
                        }
                        activeCornerIndex = closestIdx
                    },
                    onDragEnd = { activeCornerIndex = null },
                    onDragCancel = { activeCornerIndex = null },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val idx = activeCornerIndex ?: return@detectDragGestures
                        val w = size.width.toFloat().coerceAtLeast(1f)
                        val h = size.height.toFloat().coerceAtLeast(1f)

                        val deltaX = dragAmount.x / w
                        val deltaY = dragAmount.y / h

                        when (idx) {
                            0 -> { // TopLeft
                                val newX = (corners.topLeft.x + deltaX).coerceIn(0f, corners.topRight.x - 0.05f)
                                val newY = (corners.topLeft.y + deltaY).coerceIn(0f, corners.bottomLeft.y - 0.05f)
                                onCornersChanged(corners.copy(topLeft = PointF(newX, newY)))
                            }
                            1 -> { // TopRight
                                val newX = (corners.topRight.x + deltaX).coerceIn(corners.topLeft.x + 0.05f, 1f)
                                val newY = (corners.topRight.y + deltaY).coerceIn(0f, corners.bottomRight.y - 0.05f)
                                onCornersChanged(corners.copy(topRight = PointF(newX, newY)))
                            }
                            2 -> { // BottomRight
                                val newX = (corners.bottomRight.x + deltaX).coerceIn(corners.bottomLeft.x + 0.05f, 1f)
                                val newY = (corners.bottomRight.y + deltaY).coerceIn(corners.topRight.y + 0.05f, 1f)
                                onCornersChanged(corners.copy(bottomRight = PointF(newX, newY)))
                            }
                            3 -> { // BottomLeft
                                val newX = (corners.bottomLeft.x + deltaX).coerceIn(0f, corners.bottomRight.x - 0.05f)
                                val newY = (corners.bottomLeft.y + deltaY).coerceIn(corners.topLeft.y + 0.05f, 1f)
                                onCornersChanged(corners.copy(bottomLeft = PointF(newX, newY)))
                            }
                        }
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val p1 = Offset(corners.topLeft.x * w, corners.topLeft.y * h)
            val p2 = Offset(corners.topRight.x * w, corners.topRight.y * h)
            val p3 = Offset(corners.bottomRight.x * w, corners.bottomRight.y * h)
            val p4 = Offset(corners.bottomLeft.x * w, corners.bottomLeft.y * h)

            // Draw bounding quadrilateral polygon
            val path = Path().apply {
                moveTo(p1.x, p1.y)
                lineTo(p2.x, p2.y)
                lineTo(p3.x, p3.y)
                lineTo(p4.x, p4.y)
                close()
            }

            // Polygon fill
            drawPath(path, color = Color(0x3300B4D8))

            // Polygon border outline
            drawPath(
                path = path,
                color = Color(0xFF00B4D8),
                style = Stroke(
                    width = 6f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 10f), 0f)
                )
            )

            // Draw corner control handles
            val handleRadius = 28f
            val handles = listOf(p1, p2, p3, p4)
            handles.forEachIndexed { index, pt ->
                val isActive = index == activeCornerIndex
                val color = if (isActive) Color(0xFF06D6A0) else Color(0xFF00B4D8)

                // Outer touch circle halo
                drawCircle(
                    color = color.copy(alpha = if (isActive) 0.5f else 0.3f),
                    radius = if (isActive) handleRadius * 1.6f else handleRadius * 1.2f,
                    center = pt
                )
                // Main circle handle
                drawCircle(
                    color = color,
                    radius = handleRadius,
                    center = pt
                )
                // Inner white center dot
                drawCircle(
                    color = Color.White,
                    radius = handleRadius * 0.4f,
                    center = pt
                )
            }
        }
    }
}
