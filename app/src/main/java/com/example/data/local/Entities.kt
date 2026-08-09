package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "documents",
    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("folderId")]
)
data class DocumentEntity(
    @PrimaryKey val id: String,
    val title: String,
    val folderId: String? = null,
    val thumbnailPath: String = "",
    val pdfPath: String? = null,
    val pageCount: Int = 0,
    val isFavorite: Boolean = false,
    val isInTrash: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "document_pages",
    foreignKeys = [
        ForeignKey(
            entity = DocumentEntity::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("documentId")]
)
data class DocumentPageEntity(
    @PrimaryKey val id: String,
    val documentId: String,
    val pageNumber: Int,
    val originalPath: String,
    val processedPath: String,
    val rotationDegrees: Int = 0,
    val filterType: String = "AUTO",
    val cropTopLeftX: Float = 0.05f,
    val cropTopLeftY: Float = 0.05f,
    val cropTopRightX: Float = 0.95f,
    val cropTopRightY: Float = 0.05f,
    val cropBottomRightX: Float = 0.95f,
    val cropBottomRightY: Float = 0.95f,
    val cropBottomLeftX: Float = 0.05f,
    val cropBottomLeftY: Float = 0.95f,
    val createdAt: Long = System.currentTimeMillis()
)
