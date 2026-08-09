package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.DocumentEntity
import com.example.ui.NepScanViewModel
import com.example.ui.components.DocumentCard
import java.util.Locale

import com.example.ui.components.SortAndFilterSection

enum class DocumentSortField(val label: String) {
    DATE_CREATED("Date Created"),
    NAME("Name"),
    PAGES("Pages")
}

enum class DocumentSortOrder(val label: String) {
    ASCENDING("Ascending"),
    DESCENDING("Descending")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoldersScreen(
    viewModel: NepScanViewModel,
    onBack: () -> Unit,
    onOpenDocument: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val folders by viewModel.folders.collectAsState()
    val documents by viewModel.activeDocuments.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var sortField by remember { mutableStateOf(DocumentSortField.DATE_CREATED) }
    var sortOrder by remember { mutableStateOf(DocumentSortOrder.DESCENDING) }
    var selectedFolderFilter by remember { mutableStateOf<String?>(null) }
    var onlyFavorites by remember { mutableStateOf(false) }
    var isGridView by remember { mutableStateOf(false) }

    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var folderNameInput by remember { mutableStateOf("") }

    var renameDialogDoc by remember { mutableStateOf<DocumentEntity?>(null) }
    var renameInputText by remember { mutableStateOf("") }

    val filteredAndSortedDocuments = remember(documents, searchQuery, sortField, sortOrder, selectedFolderFilter, onlyFavorites) {
        var list = documents

        if (onlyFavorites) {
            list = list.filter { it.isFavorite }
        }

        // Folder filter
        if (selectedFolderFilter != null) {
            list = list.filter { it.folderId == selectedFolderFilter }
        }

        // Real-time title search filter
        if (searchQuery.isNotBlank()) {
            list = list.filter { it.title.contains(searchQuery, ignoreCase = true) }
        }

        // Sorting
        when (sortField) {
            DocumentSortField.DATE_CREATED -> {
                if (sortOrder == DocumentSortOrder.ASCENDING) {
                    list.sortedBy { it.createdAt }
                } else {
                    list.sortedByDescending { it.createdAt }
                }
            }
            DocumentSortField.NAME -> {
                if (sortOrder == DocumentSortOrder.ASCENDING) {
                    list.sortedBy { it.title.lowercase(Locale.getDefault()) }
                } else {
                    list.sortedByDescending { it.title.lowercase(Locale.getDefault()) }
                }
            }
            DocumentSortField.PAGES -> {
                if (sortOrder == DocumentSortOrder.ASCENDING) {
                    list.sortedBy { it.pageCount }
                } else {
                    list.sortedByDescending { it.pageCount }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Document Library", fontWeight = FontWeight.Bold)
                        Text(
                            text = "${filteredAndSortedDocuments.size} of ${documents.size} documents",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        folderNameInput = ""
                        showCreateFolderDialog = true
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "New Folder", tint = Color(0xFF0F5231))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    folderNameInput = ""
                    showCreateFolderDialog = true
                },
                containerColor = Color(0xFF0F5231),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Folder, contentDescription = "New Folder")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Real-time Search TextField
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search documents by title...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search Icon",
                        tint = Color(0xFF0F5231)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search query")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF0F5231),
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )

            // Interactive Sort & Filter Section Bar
            SortAndFilterSection(
                sortField = sortField,
                onSortFieldChange = { sortField = it },
                sortOrder = sortOrder,
                onSortOrderToggle = {
                    sortOrder = if (sortOrder == DocumentSortOrder.ASCENDING) DocumentSortOrder.DESCENDING else DocumentSortOrder.ASCENDING
                },
                onlyFavorites = onlyFavorites,
                onOnlyFavoritesToggle = { onlyFavorites = it },
                selectedFolderId = selectedFolderFilter,
                onFolderSelect = { selectedFolderFilter = it },
                folders = folders,
                isGridView = isGridView,
                onGridViewToggle = { isGridView = !isGridView }
            )

            // Documents List / Grid
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (filteredAndSortedDocuments.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = Color(0xFF0F5231),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = if (searchQuery.isNotBlank()) "No matching documents" else "No documents found",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (searchQuery.isNotBlank()) {
                                        "No title matches '$searchQuery'. Try a different keyword."
                                    } else {
                                        "Scan or import a document to see it in your library."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (searchQuery.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = { searchQuery = "" },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Clear Search")
                                    }
                                }
                            }
                        }
                    }
                } else if (isGridView) {
                    val chunkedDocs = filteredAndSortedDocuments.chunked(2)
                    items(chunkedDocs, key = { row -> row.first().id }) { rowDocs ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            for (doc in rowDocs) {
                                DocumentCard(
                                    document = doc,
                                    onClick = { onOpenDocument(doc.id) },
                                    onFavoriteToggle = { isFav -> viewModel.toggleFavorite(doc.id, isFav) },
                                    onShareClick = { viewModel.shareDocumentPdf(doc) },
                                    onRenameClick = {
                                        renameDialogDoc = doc
                                        renameInputText = doc.title
                                    },
                                    onDeleteClick = { viewModel.moveToTrash(doc.id) },
                                    isGridView = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (rowDocs.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                } else {
                    items(filteredAndSortedDocuments, key = { it.id }) { doc ->
                        DocumentCard(
                            document = doc,
                            onClick = { onOpenDocument(doc.id) },
                            onFavoriteToggle = { isFav -> viewModel.toggleFavorite(doc.id, isFav) },
                            onShareClick = { viewModel.shareDocumentPdf(doc) },
                            onRenameClick = {
                                renameDialogDoc = doc
                                renameInputText = doc.title
                            },
                            onDeleteClick = { viewModel.moveToTrash(doc.id) },
                            isGridView = false
                        )
                    }
                }
            }
        }
    }

    // Create Folder Dialog
    if (showCreateFolderDialog) {
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = { Text("Create New Folder") },
            text = {
                OutlinedTextField(
                    value = folderNameInput,
                    onValueChange = { folderNameInput = it },
                    label = { Text("Folder Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (folderNameInput.isNotBlank()) {
                            viewModel.createFolder(folderNameInput.trim())
                            showCreateFolderDialog = false
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Rename Document Dialog
    if (renameDialogDoc != null) {
        AlertDialog(
            onDismissRequest = { renameDialogDoc = null },
            title = { Text("Rename Document") },
            text = {
                OutlinedTextField(
                    value = renameInputText,
                    onValueChange = { renameInputText = it },
                    label = { Text("Document Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val doc = renameDialogDoc ?: return@Button
                        if (renameInputText.isNotBlank()) {
                            viewModel.renameDocument(doc.id, renameInputText.trim())
                        }
                        renameDialogDoc = null
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { renameDialogDoc = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
