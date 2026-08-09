package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.DocumentEntity
import com.example.data.local.DocumentPageEntity
import com.example.data.local.DocumentRepository
import com.example.data.local.FolderEntity
import com.example.data.local.LocalFileManager
import com.example.data.local.NepScanDatabase
import com.example.data.local.SettingsRepository
import com.example.data.local.UserPreferences
import com.example.domain.CropCorners
import com.example.domain.EdgeDetector
import com.example.domain.FilterType
import com.example.domain.ImageProcessor
import com.example.domain.PdfGenerator
import com.example.domain.ShareManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

sealed interface ScannerStage {
    object Idle : ScannerStage
    object CameraPreview : ScannerStage
    data class CropAdjustment(val pageIndex: Int, val rawBitmap: Bitmap, val corners: CropCorners) : ScannerStage
    data class Enhancement(val pageIndex: Int, val croppedBitmap: Bitmap, val filterType: FilterType) : ScannerStage
    object MultiPageReview : ScannerStage
    data class Processing(val progressMessage: String, val percent: Int = 0) : ScannerStage
}

data class ScanSessionPage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val rawBitmap: Bitmap,
    var corners: CropCorners = CropCorners(),
    var rotationDegrees: Int = 0,
    var filterType: FilterType = FilterType.AUTO,
    var processedBitmap: Bitmap? = null
)

class NepScanViewModel(application: Application) : AndroidViewModel(application) {

    val fileManager = LocalFileManager(application)
    val database = NepScanDatabase.getDatabase(application)
    val repository = DocumentRepository(application, database, fileManager)
    val settingsRepository = SettingsRepository(application)

    val userPreferences: StateFlow<UserPreferences> = settingsRepository.preferences

    val activeDocuments: StateFlow<List<DocumentEntity>> = repository.activeDocuments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteDocuments: StateFlow<List<DocumentEntity>> = repository.favoriteDocuments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trashDocuments: StateFlow<List<DocumentEntity>> = repository.trashDocuments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val folders: StateFlow<List<FolderEntity>> = repository.folders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val searchResults: StateFlow<List<DocumentEntity>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) repository.activeDocuments
            else repository.searchDocuments(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Currently active scan session state
    private val _scannerStage = MutableStateFlow<ScannerStage>(ScannerStage.Idle)
    val scannerStage: StateFlow<ScannerStage> = _scannerStage.asStateFlow()

    private val _scanSessionPages = MutableStateFlow<List<ScanSessionPage>>(emptyList())
    val scanSessionPages: StateFlow<List<ScanSessionPage>> = _scanSessionPages.asStateFlow()

    private val _documentTitleInput = MutableStateFlow("")
    val documentTitleInput: StateFlow<String> = _documentTitleInput.asStateFlow()

    private val _selectedFolderId = MutableStateFlow<String?>(null)
    val selectedFolderId: StateFlow<String?> = _selectedFolderId.asStateFlow()

    // Selected document detail view state
    private val _selectedDocumentId = MutableStateFlow<String?>(null)
    val selectedDocumentId = _selectedDocumentId.asStateFlow()

    val selectedDocument: StateFlow<DocumentEntity?> = _selectedDocumentId
        .flatMapLatest { id ->
            if (id == null) MutableStateFlow(null)
            else repository.observeDocument(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedDocumentPages: StateFlow<List<DocumentPageEntity>> = _selectedDocumentId
        .flatMapLatest { id ->
            if (id == null) MutableStateFlow(emptyList())
            else repository.getPagesForDocument(id)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.seedInitialSampleDocumentsIfNeeded()
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun startNewScanSession() {
        _scanSessionPages.value = emptyList()
        _documentTitleInput.value = "Document ${System.currentTimeMillis() % 10000}"
        _selectedFolderId.value = null
        _scannerStage.value = ScannerStage.CameraPreview
    }

    fun handleCapturedBitmap(bitmap: Bitmap) {
        viewModelScope.launch(Dispatchers.Default) {
            _scannerStage.value = ScannerStage.Processing("Detecting document edges...", 30)
            val corners = EdgeDetector.detectDocumentCorners(bitmap)
            val defaultFilter = try {
                FilterType.valueOf(userPreferences.value.defaultFilter)
            } catch (e: Exception) {
                FilterType.AUTO
            }

            val newPage = ScanSessionPage(
                rawBitmap = bitmap,
                corners = corners,
                filterType = defaultFilter
            )

            val currentList = _scanSessionPages.value.toMutableList()
            currentList.add(newPage)
            _scanSessionPages.value = currentList

            val newIndex = currentList.size - 1
            _scannerStage.value = ScannerStage.CropAdjustment(
                pageIndex = newIndex,
                rawBitmap = bitmap,
                corners = corners
            )
        }
    }

    fun importBitmapsFromUris(uris: List<Uri>) {
        viewModelScope.launch(Dispatchers.IO) {
            _scannerStage.value = ScannerStage.Processing("Importing ${uris.size} image(s)...", 10)
            val importedPages = mutableListOf<ScanSessionPage>()
            val defaultFilter = try {
                FilterType.valueOf(userPreferences.value.defaultFilter)
            } catch (e: Exception) {
                FilterType.AUTO
            }

            uris.forEachIndexed { idx, uri ->
                try {
                    getApplication<Application>().contentResolver.openInputStream(uri)?.use { stream ->
                        val bitmap = BitmapFactory.decodeStream(stream)
                        if (bitmap != null) {
                            val corners = EdgeDetector.detectDocumentCorners(bitmap)
                            val warped = ImageProcessor.perspectiveWarp(bitmap, corners)
                            val processed = ImageProcessor.applyFilter(warped, defaultFilter)

                            importedPages.add(
                                ScanSessionPage(
                                    rawBitmap = bitmap,
                                    corners = corners,
                                    filterType = defaultFilter,
                                    processedBitmap = processed
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            if (importedPages.isNotEmpty()) {
                val currentList = _scanSessionPages.value.toMutableList()
                currentList.addAll(importedPages)
                _scanSessionPages.value = currentList

                // Go to Crop review or MultiPage review
                if (importedPages.size == 1 && currentList.size == 1) {
                    val first = importedPages.first()
                    _scannerStage.value = ScannerStage.CropAdjustment(
                        pageIndex = 0,
                        rawBitmap = first.rawBitmap,
                        corners = first.corners
                    )
                } else {
                    _scannerStage.value = ScannerStage.MultiPageReview
                }
            } else {
                _scannerStage.value = ScannerStage.Idle
            }
        }
    }

    fun updateCropCorners(pageIndex: Int, newCorners: CropCorners) {
        val list = _scanSessionPages.value.toMutableList()
        if (pageIndex in list.indices) {
            list[pageIndex].corners = newCorners
            _scanSessionPages.value = list
        }
    }

    fun confirmCropAndProceedToEnhance(pageIndex: Int) {
        viewModelScope.launch(Dispatchers.Default) {
            _scannerStage.value = ScannerStage.Processing("Applying perspective warp...", 60)
            val list = _scanSessionPages.value
            if (pageIndex in list.indices) {
                val page = list[pageIndex]
                val warped = ImageProcessor.perspectiveWarp(page.rawBitmap, page.corners, page.rotationDegrees)
                val filter = page.filterType
                val processed = ImageProcessor.applyFilter(warped, filter)
                page.processedBitmap = processed

                _scannerStage.value = ScannerStage.Enhancement(
                    pageIndex = pageIndex,
                    croppedBitmap = warped,
                    filterType = filter
                )
            }
        }
    }

    fun applyFilterToSessionPage(pageIndex: Int, filterType: FilterType) {
        viewModelScope.launch(Dispatchers.Default) {
            val list = _scanSessionPages.value.toMutableList()
            if (pageIndex in list.indices) {
                val page = list[pageIndex]
                page.filterType = filterType
                val warped = ImageProcessor.perspectiveWarp(page.rawBitmap, page.corners, page.rotationDegrees)
                page.processedBitmap = ImageProcessor.applyFilter(warped, filterType)
                _scanSessionPages.value = list

                _scannerStage.value = ScannerStage.Enhancement(
                    pageIndex = pageIndex,
                    croppedBitmap = warped,
                    filterType = filterType
                )
            }
        }
    }

    fun rotateSessionPage(pageIndex: Int) {
        val list = _scanSessionPages.value.toMutableList()
        if (pageIndex in list.indices) {
            val page = list[pageIndex]
            page.rotationDegrees = (page.rotationDegrees + 90) % 360
            _scanSessionPages.value = list
            confirmCropAndProceedToEnhance(pageIndex)
        }
    }

    fun confirmEnhanceAndGoToMultiPage() {
        _scannerStage.value = ScannerStage.MultiPageReview
    }

    fun setDocumentTitleInput(title: String) {
        _documentTitleInput.value = title
    }

    fun setSelectedFolderInput(folderId: String?) {
        _selectedFolderId.value = folderId
    }

    fun saveCurrentScanSession() {
        val pages = _scanSessionPages.value
        if (pages.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            _scannerStage.value = ScannerStage.Processing("Saving document & generating PDF...", 80)
            val title = _documentTitleInput.value.ifBlank { "Scanned Document" }
            val folderId = _selectedFolderId.value

            val bitmaps = pages.map { page ->
                page.processedBitmap ?: ImageProcessor.applyFilter(
                    ImageProcessor.perspectiveWarp(page.rawBitmap, page.corners, page.rotationDegrees),
                    page.filterType
                )
            }

            val docId = repository.createDocumentWithPages(
                title = title,
                bitmaps = bitmaps,
                folderId = folderId,
                filterType = pages.first().filterType
            )

            _scannerStage.value = ScannerStage.Idle
            _selectedDocumentId.value = docId
        }
    }

    fun removeSessionPage(index: Int) {
        val list = _scanSessionPages.value.toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            _scanSessionPages.value = list
            if (list.isEmpty()) {
                _scannerStage.value = ScannerStage.Idle
            }
        }
    }

    fun reorderSessionPages(fromIndex: Int, toIndex: Int) {
        val list = _scanSessionPages.value.toMutableList()
        if (fromIndex in list.indices && toIndex in list.indices) {
            val item = list.removeAt(fromIndex)
            list.add(toIndex, item)
            _scanSessionPages.value = list
        }
    }

    fun cancelScanSession() {
        _scannerStage.value = ScannerStage.Idle
        _scanSessionPages.value = emptyList()
    }

    // Document Management Actions
    fun selectDocument(id: String?) {
        _selectedDocumentId.value = id
    }

    fun renameDocument(id: String, newName: String) {
        viewModelScope.launch {
            repository.renameDocument(id, newName)
        }
    }

    fun toggleFavorite(id: String, isFav: Boolean) {
        viewModelScope.launch {
            repository.toggleFavorite(id, isFav)
        }
    }

    fun moveToFolder(id: String, folderId: String?) {
        viewModelScope.launch {
            repository.moveToFolder(id, folderId)
        }
    }

    fun moveToTrash(id: String) {
        viewModelScope.launch {
            repository.moveToTrash(id)
            if (_selectedDocumentId.value == id) {
                _selectedDocumentId.value = null
            }
        }
    }

    fun restoreFromTrash(id: String) {
        viewModelScope.launch {
            repository.restoreFromTrash(id)
        }
    }

    fun deleteDocumentPermanently(id: String) {
        viewModelScope.launch {
            repository.deleteDocumentPermanently(id)
            if (_selectedDocumentId.value == id) {
                _selectedDocumentId.value = null
            }
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            repository.emptyTrash()
        }
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            repository.createFolder(name)
        }
    }

    fun deleteFolder(folderId: String) {
        viewModelScope.launch {
            repository.deleteFolder(folderId)
        }
    }

    // Native Sharing Sheet Calls
    fun shareDocumentPdf(document: DocumentEntity) {
        val pdfPath = document.pdfPath ?: return
        val file = File(pdfPath)
        if (file.exists()) {
            ShareManager.shareFile(getApplication(), file, "application/pdf", "Share ${document.title} (PDF)")
        } else {
            // Regenerate PDF if needed
            viewModelScope.launch(Dispatchers.IO) {
                val pages = repository.getPagesForDocument(document.id)
                // regenerate and trigger
            }
        }
    }

    fun shareDocumentWebLink(document: DocumentEntity) {
        val randomHash = kotlin.random.Random.nextInt(100000, 999999)
        val shortId = document.id.take(8)
        val shareableUrl = "https://nirmalgaihre.com.np/Nepscan/doc_${shortId}_$randomHash"
        val shareMessage = "📄 View and download '${document.title}' on NepScan:\n$shareableUrl"
        ShareManager.shareTextLink(getApplication(), shareMessage, "Share NepScan Document Link")
    }

    fun exportAndSharePagesAsImages(document: DocumentEntity, pages: List<DocumentPageEntity>, isPng: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            val exportedFiles = mutableListOf<File>()
            val extension = if (isPng) "png" else "jpg"
            val format = if (isPng) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG

            pages.forEachIndexed { idx, page ->
                val procPath = page.processedPath
                val bitmap = BitmapFactory.decodeFile(procPath)
                if (bitmap != null) {
                    val exportFile = fileManager.createExportImageFile(document.title, idx, extension)
                    fileManager.saveBitmap(bitmap, exportFile, format = format)
                    exportedFiles.add(exportFile)
                }
            }

            if (exportedFiles.isNotEmpty()) {
                val mimeType = if (isPng) "image/png" else "image/jpeg"
                withContext(Dispatchers.Main) {
                    if (exportedFiles.size == 1) {
                        ShareManager.shareFile(getApplication(), exportedFiles.first(), mimeType, "Share Page Image")
                    } else {
                        ShareManager.shareMultipleFiles(getApplication(), exportedFiles, mimeType, "Share Page Images")
                    }
                }
            }
        }
    }

    // Settings
    fun updateAutoCapture(value: Boolean) = settingsRepository.updateAutoCapture(value)
    fun updateDefaultFilter(filter: String) = settingsRepository.updateDefaultFilter(filter)
    fun updateScanQuality(quality: String) = settingsRepository.updateScanQuality(quality)
    fun updateFlashMode(mode: String) = settingsRepository.updateFlashMode(mode)
    fun updatePdfQuality(quality: String) = settingsRepository.updatePdfQuality(quality)
    fun updateThemeMode(theme: String) = settingsRepository.updateThemeMode(theme)

    fun clearTempFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            fileManager.clearTempFiles()
        }
    }
}
