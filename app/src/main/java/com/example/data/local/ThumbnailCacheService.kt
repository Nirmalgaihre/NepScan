package com.example.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

class ThumbnailCacheService private constructor(
    private val context: Context,
    private val fileManager: LocalFileManager
) {
    // Memory cache in kilobytes (e.g., 1/8th of available memory)
    private val maxMemoryKb = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSizeKb = maxMemoryKb / 8

    private val memoryCache = object : LruCache<String, Bitmap>(cacheSizeKb) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    /**
     * Generates a low-resolution thumbnail (max 300px width/height) from a full bitmap,
     * saves it to the disk thumbnail directory, and puts it in the LRU memory cache.
     */
    fun generateAndCacheThumbnail(documentId: String, firstPageBitmap: Bitmap): String {
        val lowResBitmap = createLowResBitmap(firstPageBitmap, maxDimension = 300)
        
        // Put into memory cache
        memoryCache.put(documentId, lowResBitmap)

        // Save to disk
        val thumbFile = File(fileManager.thumbnailsDir, "$documentId.jpg")
        try {
            thumbFile.parentFile?.mkdirs()
            FileOutputStream(thumbFile).use { out ->
                lowResBitmap.compress(Bitmap.CompressFormat.JPEG, 75, out)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return thumbFile.absolutePath
    }

    /**
     * Retrieves thumbnail bitmap from memory cache, or loads from disk if available.
     */
    fun getThumbnailBitmap(documentId: String, thumbnailPath: String?): Bitmap? {
        val cached = memoryCache.get(documentId)
        if (cached != null && !cached.isRecycled) {
            return cached
        }

        if (thumbnailPath.isNullOrBlank()) return null
        val file = File(thumbnailPath)
        if (!file.exists()) return null

        return try {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            if (bitmap != null) {
                memoryCache.put(documentId, bitmap)
            }
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Removes thumbnail from memory cache and disk.
     */
    fun evictThumbnail(documentId: String) {
        memoryCache.remove(documentId)
        val file = File(fileManager.thumbnailsDir, "$documentId.jpg")
        if (file.exists()) {
            file.delete()
        }
    }

    fun clearMemoryCache() {
        memoryCache.evictAll()
    }

    private fun createLowResBitmap(source: Bitmap, maxDimension: Int): Bitmap {
        val width = source.width
        val height = source.height
        if (width <= maxDimension && height <= maxDimension) {
            return source
        }

        val scale = maxDimension.toFloat() / max(width, height)
        val newWidth = (width * scale).toInt().coerceAtLeast(1)
        val newHeight = (height * scale).toInt().coerceAtLeast(1)

        return Bitmap.createScaledBitmap(source, newWidth, newHeight, true)
    }

    companion object {
        @Volatile
        private var INSTANCE: ThumbnailCacheService? = null

        fun getInstance(context: Context, fileManager: LocalFileManager): ThumbnailCacheService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ThumbnailCacheService(context.applicationContext, fileManager).also {
                    INSTANCE = it
                }
            }
        }
    }
}
