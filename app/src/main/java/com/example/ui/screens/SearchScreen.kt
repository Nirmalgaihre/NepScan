package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.example.ui.NepScanViewModel
import com.example.ui.components.DocumentCard
import java.util.Locale

import com.example.data.local.FolderEntity
import com.example.ui.components.SortAndFilterSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: NepScanViewModel,
    onOpenDocument: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val rawResults by viewModel.searchResults.collectAsState()
    val folders by viewModel.folders.collectAsState()

    var sortField by remember { mutableStateOf(DocumentSortField.DATE_CREATED) }
    var sortOrder by remember { mutableStateOf(DocumentSortOrder.DESCENDING) }
    var selectedFolderFilter by remember { mutableStateOf<String?>(null) }
    var onlyFavorites by remember { mutableStateOf(false) }
    var isGridView by remember { mutableStateOf(false) }

    val sortedResults = remember(rawResults, sortField, sortOrder, selectedFolderFilter, onlyFavorites) {
        var list = rawResults
        if (onlyFavorites) {
            list = list.filter { it.isFavorite }
        }
        if (selectedFolderFilter != null) {
            list = list.filter { it.folderId == selectedFolderFilter }
        }
        when (sortField) {
            DocumentSortField.DATE_CREATED -> {
                if (sortOrder == DocumentSortOrder.ASCENDING) list.sortedBy { it.createdAt }
                else list.sortedByDescending { it.createdAt }
            }
            DocumentSortField.NAME -> {
                if (sortOrder == DocumentSortOrder.ASCENDING) list.sortedBy { it.title.lowercase(Locale.getDefault()) }
                else list.sortedByDescending { it.title.lowercase(Locale.getDefault()) }
            }
            DocumentSortField.PAGES -> {
                if (sortOrder == DocumentSortOrder.ASCENDING) list.sortedBy { it.pageCount }
                else list.sortedByDescending { it.pageCount }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Search Documents", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                placeholder = { Text("Search by document title in real-time...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF0F5231)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
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
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Interactive Sort & Filter Controls Bar
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

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isGridView) {
                    val chunkedDocs = sortedResults.chunked(2)
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
                                    onRenameClick = { },
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
                    items(sortedResults, key = { it.id }) { doc ->
                        DocumentCard(
                            document = doc,
                            onClick = { onOpenDocument(doc.id) },
                            onFavoriteToggle = { isFav -> viewModel.toggleFavorite(doc.id, isFav) },
                            onShareClick = { viewModel.shareDocumentPdf(doc) },
                            onRenameClick = { },
                            onDeleteClick = { viewModel.moveToTrash(doc.id) },
                            isGridView = false
                        )
                    }
                }
            }
        }
    }
}
