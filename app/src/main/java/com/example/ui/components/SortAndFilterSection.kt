package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.FolderEntity
import com.example.ui.screens.DocumentSortField
import com.example.ui.screens.DocumentSortOrder

@Composable
fun SortAndFilterSection(
    sortField: DocumentSortField,
    onSortFieldChange: (DocumentSortField) -> Unit,
    sortOrder: DocumentSortOrder,
    onSortOrderToggle: () -> Unit,
    onlyFavorites: Boolean,
    onOnlyFavoritesToggle: (Boolean) -> Unit,
    selectedFolderId: String?,
    onFolderSelect: (String?) -> Unit,
    folders: List<FolderEntity>,
    isGridView: Boolean,
    onGridViewToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // TOP ROW: Primary Filter Chips (All, Starred, Folders)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item {
                FilterChip(
                    selected = selectedFolderId == null && !onlyFavorites,
                    onClick = {
                        onFolderSelect(null)
                        onOnlyFavoritesToggle(false)
                    },
                    label = { Text("All Docs", fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF0F5231),
                        selectedLabelColor = Color.White
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
            }

            item {
                FilterChip(
                    selected = onlyFavorites,
                    onClick = {
                        onOnlyFavoritesToggle(!onlyFavorites)
                        if (!onlyFavorites) onFolderSelect(null)
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (onlyFavorites) Icons.Default.Star else Icons.Outlined.StarBorder,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text("Starred", fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFEAB308),
                        selectedLabelColor = Color.Black,
                        selectedLeadingIconColor = Color.Black
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
            }

            items(folders, key = { it.id }) { folder ->
                val isSelected = selectedFolderId == folder.id
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        if (isSelected) {
                            onFolderSelect(null)
                        } else {
                            onFolderSelect(folder.id)
                            onOnlyFavoritesToggle(false)
                        }
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text(folder.name, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF065F46),
                        selectedLabelColor = Color.White,
                        selectedLeadingIconColor = Color.White
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
            }
        }

        // BOTTOM ROW: Interactive Sort Controls + View Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sort Option Chips Segment
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Date Sort Pill
                SortPill(
                    label = "Date",
                    icon = Icons.Default.CalendarMonth,
                    isSelected = sortField == DocumentSortField.DATE_CREATED,
                    onClick = { onSortFieldChange(DocumentSortField.DATE_CREATED) }
                )

                // Name Sort Pill
                SortPill(
                    label = "Name",
                    icon = Icons.Default.SortByAlpha,
                    isSelected = sortField == DocumentSortField.NAME,
                    onClick = { onSortFieldChange(DocumentSortField.NAME) }
                )

                // Pages Sort Pill
                SortPill(
                    label = "Pages",
                    icon = Icons.Default.Description,
                    isSelected = sortField == DocumentSortField.PAGES,
                    onClick = { onSortFieldChange(DocumentSortField.PAGES) }
                )
            }

            // Direction Toggle & Grid/List Toggle Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Direction Toggle Chip (Newest / Oldest, A-Z / Z-A)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFE2F1E8),
                    border = BorderStroke(1.dp, Color(0xFFB8E0CB)),
                    modifier = Modifier.clickable(onClick = onSortOrderToggle)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = if (sortOrder == DocumentSortOrder.DESCENDING) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                            contentDescription = "Order Direction",
                            tint = Color(0xFF0F5231),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = when (sortField) {
                                DocumentSortField.DATE_CREATED -> if (sortOrder == DocumentSortOrder.DESCENDING) "Newest" else "Oldest"
                                DocumentSortField.NAME -> if (sortOrder == DocumentSortOrder.ASCENDING) "A-Z" else "Z-A"
                                DocumentSortField.PAGES -> if (sortOrder == DocumentSortOrder.DESCENDING) "Most" else "Least"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F5231)
                        )
                    }
                }

                // Grid / List View Toggle
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.clickable(onClick = onGridViewToggle)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = if (isGridView) Icons.Default.List else Icons.Default.GridView,
                            contentDescription = "Toggle Grid or List View",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SortPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Color(0xFF0F5231) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        border = BorderStroke(
            1.dp,
            if (isSelected) Color(0xFF0F5231) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
        ),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
