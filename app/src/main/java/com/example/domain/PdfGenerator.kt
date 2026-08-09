package com.example.domain

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import com.example.data.local.DocumentPageEntity
import com.example.data.local.LocalFileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object PdfGenerator {

    // Standard A4 dimensions in PDF points (1/72 inch)
    private const val A4_WIDTH_PTS = 595
    private const val A4_HEIGHT_PTS = 842

    suspend fun generatePdf(
        documentTitle: String,
        documentId: String,
        pages: List<DocumentPageEntity>,
        fileManager: LocalFileManager,
        qualitySetting: String = "HIGH",
        onProgress: ((current: Int, total: Int) -> Unit)? = null
    ): File? = withContext(Dispatchers.IO) {
        if (pages.isEmpty()) return@withContext null

        val pdfDocument = PdfDocument()
        val outputFile = fileManager.createPdfFile(documentId, documentTitle)

        try {
            pages.forEachIndexed { index, pageEntity ->
                onProgress?.invoke(index + 1, pages.size)

                val imagePath = if (File(pageEntity.processedPath).exists()) {
                    pageEntity.processedPath
                } else {
                    pageEntity.originalPath
                }

                val originalBitmap = BitmapFactory.decodeFile(imagePath) ?: return@forEachIndexed

                // Determine orientation based on image aspect ratio
                val isLandscape = originalBitmap.width > originalBitmap.height
                val pageWidth = if (isLandscape) A4_HEIGHT_PTS else A4_WIDTH_PTS
                val pageHeight = if (isLandscape) A4_WIDTH_PTS else A4_HEIGHT_PTS

                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas: Canvas = page.canvas

                // Scale bitmap preserving aspect ratio into the PDF page with padding
                val padding = 16f
                val availableWidth = pageWidth - (padding * 2)
                val availableHeight = pageHeight - (padding * 2)

                val scale = Math.min(
                    availableWidth / originalBitmap.width,
                    availableHeight / originalBitmap.height
                )

                val scaledWidth = originalBitmap.width * scale
                val scaledHeight = originalBitmap.height * scale

                val left = padding + (availableWidth - scaledWidth) / 2f
                val top = padding + (availableHeight - scaledHeight) / 2f

                val destRect = RectF(left, top, left + scaledWidth, top + scaledHeight)
                canvas.drawBitmap(originalBitmap, null, destRect, null)

                pdfDocument.finishPage(page)
                originalBitmap.recycle()
            }

            FileOutputStream(outputFile).use { out ->
                pdfDocument.writeTo(out)
            }

            outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            pdfDocument.close()
        }
    }
}
