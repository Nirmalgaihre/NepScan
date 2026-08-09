package com.example.data.local

import android.content.Context
import android.graphics.Bitmap
import com.example.domain.CropCorners
import com.example.domain.EdgeDetector
import com.example.domain.FilterType
import com.example.domain.ImageProcessor
import com.example.domain.PdfGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class DocumentRepository(
    private val context: Context,
    private val db: NepScanDatabase,
    val fileManager: LocalFileManager
) {
    val thumbnailCacheService = ThumbnailCacheService.getInstance(context, fileManager)

    private val docDao = db.documentDao()
    private val pageDao = db.documentPageDao()
    private val folderDao = db.folderDao()

    val activeDocuments: Flow<List<DocumentEntity>> = docDao.getAllActiveDocuments()
    val favoriteDocuments: Flow<List<DocumentEntity>> = docDao.getFavoriteDocuments()
    val trashDocuments: Flow<List<DocumentEntity>> = docDao.getTrashDocuments()
    val folders: Flow<List<FolderEntity>> = folderDao.getAllFolders()

    fun getDocumentsByFolder(folderId: String): Flow<List<DocumentEntity>> = docDao.getDocumentsByFolder(folderId)

    fun observeDocument(id: String): Flow<DocumentEntity?> = docDao.observeDocumentById(id)

    fun getPagesForDocument(documentId: String): Flow<List<DocumentPageEntity>> = pageDao.getPagesForDocument(documentId)

    suspend fun getDocument(id: String): DocumentEntity? = docDao.getDocumentById(id)

    fun searchDocuments(query: String): Flow<List<DocumentEntity>> = docDao.searchDocuments(query)

    suspend fun createDocumentWithPages(
        title: String,
        bitmaps: List<Bitmap>,
        folderId: String? = null,
        filterType: FilterType = FilterType.AUTO
    ): String = withContext(Dispatchers.IO) {
        val docId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        var firstPageProcPath = ""

        val pages = mutableListOf<DocumentPageEntity>()
        bitmaps.forEachIndexed { index, rawBitmap ->
            val pageId = UUID.randomUUID().toString()
            val origFile = fileManager.createPageOriginalFile(docId, pageId)
            fileManager.saveBitmap(rawBitmap, origFile)

            // Auto detect corners
            val corners = EdgeDetector.detectDocumentCorners(rawBitmap)

            // Perspective warp & filter
            val warped = ImageProcessor.perspectiveWarp(rawBitmap, corners)
            val filtered = ImageProcessor.applyFilter(warped, filterType)

            val procFile = fileManager.createPageProcessedFile(docId, pageId)
            fileManager.saveBitmap(filtered, procFile)

            var generatedThumbPath = ""
            if (index == 0) {
                firstPageProcPath = procFile.absolutePath
                // Generate and cache low-res thumbnail helper service
                generatedThumbPath = thumbnailCacheService.generateAndCacheThumbnail(docId, filtered)
            }

            val pageEntity = DocumentPageEntity(
                id = pageId,
                documentId = docId,
                pageNumber = index + 1,
                originalPath = origFile.absolutePath,
                processedPath = procFile.absolutePath,
                rotationDegrees = 0,
                filterType = filterType.name,
                cropTopLeftX = corners.topLeft.x,
                cropTopLeftY = corners.topLeft.y,
                cropTopRightX = corners.topRight.x,
                cropTopRightY = corners.topRight.y,
                cropBottomRightX = corners.bottomRight.x,
                cropBottomRightY = corners.bottomRight.y,
                cropBottomLeftX = corners.bottomLeft.x,
                cropBottomLeftY = corners.bottomLeft.y,
                createdAt = now
            )
            pages.add(pageEntity)
        }

        val thumbPath = File(fileManager.thumbnailsDir, "$docId.jpg").absolutePath

        val document = DocumentEntity(
            id = docId,
            title = title.ifBlank { "Document ${now % 10000}" },
            folderId = folderId,
            thumbnailPath = thumbPath,
            pdfPath = null,
            pageCount = pages.size,
            isFavorite = false,
            isInTrash = false,
            createdAt = now,
            updatedAt = now
        )

        docDao.insertDocument(document)
        pageDao.insertPages(pages)

        // Generate initial PDF
        val pdfFile = PdfGenerator.generatePdf(document.title, docId, pages, fileManager)
        if (pdfFile != null) {
            docDao.updateDocument(document.copy(pdfPath = pdfFile.absolutePath))
        }

        docId
    }

    suspend fun addPagesToDocument(
        documentId: String,
        bitmaps: List<Bitmap>,
        filterType: FilterType = FilterType.AUTO
    ) = withContext(Dispatchers.IO) {
        val existingDoc = docDao.getDocumentById(documentId) ?: return@withContext
        val existingPages = pageDao.getPagesForDocumentSync(documentId)
        val startPageNum = existingPages.size + 1
        val now = System.currentTimeMillis()

        val newPages = mutableListOf<DocumentPageEntity>()
        bitmaps.forEachIndexed { index, rawBitmap ->
            val pageId = UUID.randomUUID().toString()
            val origFile = fileManager.createPageOriginalFile(documentId, pageId)
            fileManager.saveBitmap(rawBitmap, origFile)

            val corners = EdgeDetector.detectDocumentCorners(rawBitmap)
            val warped = ImageProcessor.perspectiveWarp(rawBitmap, corners)
            val filtered = ImageProcessor.applyFilter(warped, filterType)

            val procFile = fileManager.createPageProcessedFile(documentId, pageId)
            fileManager.saveBitmap(filtered, procFile)

            val pageEntity = DocumentPageEntity(
                id = pageId,
                documentId = documentId,
                pageNumber = startPageNum + index,
                originalPath = origFile.absolutePath,
                processedPath = procFile.absolutePath,
                rotationDegrees = 0,
                filterType = filterType.name,
                cropTopLeftX = corners.topLeft.x,
                cropTopLeftY = corners.topLeft.y,
                cropTopRightX = corners.topRight.x,
                cropTopRightY = corners.topRight.y,
                cropBottomRightX = corners.bottomRight.x,
                cropBottomRightY = corners.bottomRight.y,
                cropBottomLeftX = corners.bottomLeft.x,
                cropBottomLeftY = corners.bottomLeft.y,
                createdAt = now
            )
            newPages.add(pageEntity)
        }

        pageDao.insertPages(newPages)
        val updatedPages = pageDao.getPagesForDocumentSync(documentId)

        // Regenerate PDF
        val pdfFile = PdfGenerator.generatePdf(existingDoc.title, documentId, updatedPages, fileManager)
        docDao.updateDocument(
            existingDoc.copy(
                pageCount = updatedPages.size,
                pdfPath = pdfFile?.absolutePath ?: existingDoc.pdfPath,
                updatedAt = now
            )
        )
    }

    suspend fun updatePageTransform(
        pageId: String,
        corners: CropCorners,
        filterType: FilterType,
        rotationDegrees: Int
    ) = withContext(Dispatchers.IO) {
        val allPages = mutableListOf<DocumentPageEntity>()
        var targetDocId: String? = null

        // Get page info
        // Simple scan from pages or direct flow
        val pagesInDoc = pageDao.getPagesForDocumentSync(pageId)
        // search matching page
    }

    suspend fun updatePageInDocument(
        page: DocumentPageEntity,
        rawBitmap: Bitmap,
        corners: CropCorners,
        filterType: FilterType,
        rotationDegrees: Int
    ) = withContext(Dispatchers.IO) {
        val warped = ImageProcessor.perspectiveWarp(rawBitmap, corners, rotationDegrees)
        val filtered = ImageProcessor.applyFilter(warped, filterType)

        val procFile = File(page.processedPath)
        fileManager.saveBitmap(filtered, procFile)

        val updatedPage = page.copy(
            rotationDegrees = rotationDegrees,
            filterType = filterType.name,
            cropTopLeftX = corners.topLeft.x,
            cropTopLeftY = corners.topLeft.y,
            cropTopRightX = corners.topRight.x,
            cropTopRightY = corners.topRight.y,
            cropBottomRightX = corners.bottomRight.x,
            cropBottomRightY = corners.bottomRight.y,
            cropBottomLeftX = corners.bottomLeft.x,
            cropBottomLeftY = corners.bottomLeft.y
        )

        pageDao.updatePage(updatedPage)

        // Update document thumbnail if first page
        if (page.pageNumber == 1) {
            val thumbFile = File(fileManager.thumbnailsDir, "${page.documentId}.jpg")
            fileManager.saveBitmap(filtered, thumbFile, quality = 70)
        }

        // Regenerate PDF
        val doc = docDao.getDocumentById(page.documentId)
        if (doc != null) {
            val allPages = pageDao.getPagesForDocumentSync(page.documentId)
            val pdfFile = PdfGenerator.generatePdf(doc.title, doc.id, allPages, fileManager)
            docDao.updateDocument(doc.copy(pdfPath = pdfFile?.absolutePath, updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun deletePage(page: DocumentPageEntity) = withContext(Dispatchers.IO) {
        pageDao.deletePage(page.id)
        File(page.originalPath).delete()
        File(page.processedPath).delete()

        val doc = docDao.getDocumentById(page.documentId) ?: return@withContext
        val remainingPages = pageDao.getPagesForDocumentSync(page.documentId)

        if (remainingPages.isEmpty()) {
            // Delete entire doc if no pages remain
            deleteDocumentPermanently(doc.id)
        } else {
            // Re-index page numbers
            val reindexed = remainingPages.mapIndexed { idx, p -> p.copy(pageNumber = idx + 1) }
            pageDao.insertPages(reindexed)

            val pdfFile = PdfGenerator.generatePdf(doc.title, doc.id, reindexed, fileManager)
            docDao.updateDocument(
                doc.copy(
                    pageCount = reindexed.size,
                    pdfPath = pdfFile?.absolutePath,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun reorderPages(documentId: String, reorderedPages: List<DocumentPageEntity>) = withContext(Dispatchers.IO) {
        val updated = reorderedPages.mapIndexed { idx, p -> p.copy(pageNumber = idx + 1) }
        pageDao.insertPages(updated)

        val doc = docDao.getDocumentById(documentId)
        if (doc != null) {
            val pdfFile = PdfGenerator.generatePdf(doc.title, doc.id, updated, fileManager)
            docDao.updateDocument(doc.copy(pdfPath = pdfFile?.absolutePath, updatedAt = System.currentTimeMillis()))
        }
    }

    suspend fun renameDocument(id: String, newTitle: String) = withContext(Dispatchers.IO) {
        val doc = docDao.getDocumentById(id) ?: return@withContext
        val updated = doc.copy(title = newTitle.ifBlank { "Untitled Document" }, updatedAt = System.currentTimeMillis())
        docDao.updateDocument(updated)

        val pages = pageDao.getPagesForDocumentSync(id)
        val pdfFile = PdfGenerator.generatePdf(updated.title, id, pages, fileManager)
        if (pdfFile != null) {
            docDao.updateDocument(updated.copy(pdfPath = pdfFile.absolutePath))
        }
    }

    suspend fun toggleFavorite(id: String, isFav: Boolean) {
        docDao.setFavorite(id, isFav)
    }

    suspend fun moveToFolder(id: String, folderId: String?) {
        docDao.moveToFolder(id, folderId)
    }

    suspend fun moveToTrash(id: String) {
        docDao.moveToTrash(id)
    }

    suspend fun restoreFromTrash(id: String) {
        docDao.restoreFromTrash(id)
    }

    suspend fun deleteDocumentPermanently(id: String) = withContext(Dispatchers.IO) {
        docDao.deletePermanently(id)
        fileManager.deleteDocumentFolder(id)
    }

    suspend fun emptyTrash() = withContext(Dispatchers.IO) {
        val trashedDocs = docDao.getTrashDocuments()
        docDao.emptyTrash()
    }

    suspend fun createFolder(name: String) = withContext(Dispatchers.IO) {
        val folder = FolderEntity(
            id = UUID.randomUUID().toString(),
            name = name.ifBlank { "New Folder" }
        )
        folderDao.insertFolder(folder)
    }

    suspend fun deleteFolder(folderId: String) = withContext(Dispatchers.IO) {
        folderDao.deleteFolder(folderId)
    }

    suspend fun seedInitialSampleDocumentsIfNeeded() = withContext(Dispatchers.IO) {
        // If empty, create starter folders and sample Nepali citizenship & certificate document for immediate user experience!
        val existingDocs = docDao.getDocumentById("sample_citizenship")
        if (existingDocs == null) {
            val collegeFolder = FolderEntity(id = "folder_college", name = "College")
            val personalFolder = FolderEntity(id = "folder_personal", name = "Personal")
            val certsFolder = FolderEntity(id = "folder_certs", name = "Certificates")

            folderDao.insertFolder(collegeFolder)
            folderDao.insertFolder(personalFolder)
            folderDao.insertFolder(certsFolder)

            // Generate sample documents
            createSampleDoc("Citizenship Card", "folder_personal", "sample_citizenship")
            createSampleDoc("Academic Certificate", "folder_certs", "sample_certificate")
        }
    }

    private suspend fun createSampleDoc(title: String, folderId: String, docId: String) {
        val now = System.currentTimeMillis()
        // Create 2 pages per sample doc
        val sampleBitmap1 = createSampleDocumentBitmap(title, 1)
        val sampleBitmap2 = createSampleDocumentBitmap(title, 2)

        val pages = mutableListOf<DocumentPageEntity>()

        listOf(sampleBitmap1 to 1, sampleBitmap2 to 2).forEach { (bmp, pageNum) ->
            val pageId = "${docId}_p$pageNum"
            val origFile = fileManager.createPageOriginalFile(docId, pageId)
            val procFile = fileManager.createPageProcessedFile(docId, pageId)

            fileManager.saveBitmap(bmp, origFile)
            val filtered = ImageProcessor.applyFilter(bmp, FilterType.AUTO)
            fileManager.saveBitmap(filtered, procFile)

            if (pageNum == 1) {
                val thumbFile = File(fileManager.thumbnailsDir, "$docId.jpg")
                fileManager.saveBitmap(filtered, thumbFile, quality = 70)
            }

            pages.add(
                DocumentPageEntity(
                    id = pageId,
                    documentId = docId,
                    pageNumber = pageNum,
                    originalPath = origFile.absolutePath,
                    processedPath = procFile.absolutePath,
                    rotationDegrees = 0,
                    filterType = "AUTO",
                    createdAt = now
                )
            )
        }

        val thumbPath = File(fileManager.thumbnailsDir, "$docId.jpg").absolutePath

        val doc = DocumentEntity(
            id = docId,
            title = title,
            folderId = folderId,
            thumbnailPath = thumbPath,
            pdfPath = null,
            pageCount = 2,
            isFavorite = true,
            isInTrash = false,
            createdAt = now,
            updatedAt = now
        )

        docDao.insertDocument(doc)
        pageDao.insertPages(pages)

        val pdf = PdfGenerator.generatePdf(title, docId, pages, fileManager)
        docDao.updateDocument(doc.copy(pdfPath = pdf?.absolutePath))
    }

    private fun createSampleDocumentBitmap(title: String, pageNumber: Int): Bitmap {
        val w = 1080
        val h = 1528
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.WHITE)

        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)

        // Header banner accent
        paint.color = android.graphics.Color.parseColor("#0B132B")
        canvas.drawRect(0f, 0f, w.toFloat(), 180f, paint)

        // Header Title
        paint.color = android.graphics.Color.WHITE
        paint.textSize = 54f
        paint.isFakeBoldText = true
        canvas.drawText("NEPSCAN OFFICIAL DOCUMENT", 60f, 110f, paint)

        // Document Details
        paint.color = android.graphics.Color.parseColor("#1C2541")
        paint.textSize = 48f
        canvas.drawText("Title: $title", 80f, 300f, paint)

        paint.textSize = 36f
        paint.isFakeBoldText = false
        paint.color = android.graphics.Color.DKGRAY
        canvas.drawText("Document ID: NEP-2026-${(1000..9999).random()}", 80f, 380f, paint)
        canvas.drawText("Page: $pageNumber of 2", 80f, 440f, paint)
        canvas.drawText("Format: High Definition Scan", 80f, 500f, paint)

        // Decorative document lines mimicking scanned paper content
        paint.color = android.graphics.Color.LTGRAY
        paint.strokeWidth = 4f
        var yPos = 600f
        while (yPos < h - 200f) {
            canvas.drawLine(80f, yPos, w - 80f, yPos, paint)
            yPos += 70f
        }

        // Seal / Stamp badge
        paint.color = android.graphics.Color.parseColor("#1A5276")
        paint.style = android.graphics.Paint.Style.STROKE
        paint.strokeWidth = 8f
        canvas.drawCircle(w - 220f, h - 220f, 110f, paint)

        paint.style = android.graphics.Paint.Style.FILL
        paint.textSize = 28f
        paint.isFakeBoldText = true
        canvas.drawText("NEPSCAN", w - 280f, h - 230f, paint)
        canvas.drawText("VERIFIED", w - 280f, h - 190f, paint)

        return bitmap
    }
}
