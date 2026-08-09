package com.example.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

class LocalFileManager(private val context: Context) {

    private val baseDir: File
        get() {
            val dir = File(context.filesDir, "NepScan")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    private val documentsDir: File
        get() {
            val dir = File(baseDir, "Documents")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    val thumbnailsDir: File
        get() {
            val dir = File(baseDir, "Thumbnails")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    val tempDir: File
        get() {
            val dir = File(baseDir, "Temp")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    val exportsDir: File
        get() {
            val dir = File(context.getExternalFilesDir(null) ?: context.filesDir, "Exports")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    fun getDocumentDirectory(documentId: String): File {
        val dir = File(documentsDir, documentId)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun saveBitmap(bitmap: Bitmap, file: File, format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG, quality: Int = 90): File {
        file.parentFile?.mkdirs()
        FileOutputStream(file).use { out ->
            bitmap.compress(format, quality, out)
        }
        return file
    }

    fun saveInputStreamToFile(inputStream: InputStream, outputFile: File): File {
        outputFile.parentFile?.mkdirs()
        FileOutputStream(outputFile).use { out ->
            inputStream.copyTo(out)
        }
        return outputFile
    }

    fun createTempImageFile(): File {
        return File(tempDir, "temp_scan_${UUID.randomUUID()}.jpg")
    }

    fun createPageOriginalFile(documentId: String, pageId: String): File {
        val docDir = getDocumentDirectory(documentId)
        return File(docDir, "page_${pageId}_orig.jpg")
    }

    fun createPageProcessedFile(documentId: String, pageId: String): File {
        val docDir = getDocumentDirectory(documentId)
        return File(docDir, "page_${pageId}_proc.jpg")
    }

    fun createPdfFile(documentId: String, title: String): File {
        val safeTitle = title.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        return File(exportsDir, "${safeTitle}_$documentId.pdf")
    }

    fun createExportImageFile(title: String, pageIndex: Int, extension: String = "jpg"): File {
        val safeTitle = title.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
        return File(exportsDir, "${safeTitle}_page_${pageIndex + 1}_${System.currentTimeMillis()}.$extension")
    }

    fun loadBitmapFromFile(filePath: String): Bitmap? {
        val file = File(filePath)
        if (!file.exists()) return null
        return try {
            BitmapFactory.decodeFile(file.absolutePath)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun deleteDocumentFolder(documentId: String) {
        val docDir = File(documentsDir, documentId)
        if (docDir.exists()) {
            docDir.deleteRecursively()
        }
        val thumbFile = File(thumbnailsDir, "$documentId.jpg")
        if (thumbFile.exists()) {
            thumbFile.delete()
        }
    }

    fun clearTempFiles() {
        if (tempDir.exists()) {
            tempDir.listFiles()?.forEach { it.delete() }
        }
    }

    fun getStorageUsageBytes(): Long {
        fun dirSize(dir: File): Long {
            var result: Long = 0
            val fileList = dir.listFiles() ?: return 0
            for (file in fileList) {
                result += if (file.isDirectory) dirSize(file) else file.length()
            }
            return result
        }
        return dirSize(baseDir)
    }
}
