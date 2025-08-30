package com.qjproject.liturgicalcalendar.ui.screens.songdetails

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.qjproject.liturgicalcalendar.data.Song
import com.qjproject.liturgicalcalendar.ui.theme.SaturatedNavy
import com.qjproject.liturgicalcalendar.ui.theme.TileBackground
import com.qjproject.liturgicalcalendar.ui.theme.VeryDarkNavy

@Composable
fun AssignTagsToSongDialog(
    song: Song,
    onDismiss: () -> Unit,
    onTagsUpdated: (Song) -> Unit
) {
    val context = LocalContext.current
    val viewModel: AssignTagsToSongViewModel = viewModel(
        factory = AssignTagsToSongViewModelFactory(context)
    )
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(song) {
        viewModel.initializeForSong(song)
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = VeryDarkNavy)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                // Nagłówek
                Text(
                    text = "Dodaj tagi do pieśni",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                    color = SaturatedNavy,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Wyszukiwarka
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    placeholder = { Text("Wyszukaj tag") },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { 
                        Icon(Icons.Default.Search, contentDescription = "Wyszukaj") 
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Wyczyść")
                            }
                        }
                    },
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Lista tagów
                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (uiState.filteredTags.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (uiState.searchQuery.isEmpty()) "Brak tagów" else "Brak wyników wyszukiwania",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.filteredTags, key = { it }) { tag ->
                            TagSelectionItem(
                                tag = tag,
                                isSelected = uiState.selectedTags.contains(tag),
                                onToggleSelection = { viewModel.toggleTagSelection(tag) }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Przyciski
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Anuluj")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { 
                            viewModel.saveChanges { updatedSong ->
                                onTagsUpdated(updatedSong)
                                onDismiss()
                            }
                        },
                        enabled = !uiState.isSaving
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Zapisz")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TagSelectionItem(
    tag: String,
    isSelected: Boolean,
    onToggleSelection: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onToggleSelection),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = TileBackground)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val iconColor = if (isSelected) {
                androidx.compose.ui.graphics.Color(0xFF4CAF50) // Zielony kolor dla zaznaczonych
            } else {
                MaterialTheme.colorScheme.primary // Niebieski dla niezaznaczonych
            }
            
            Icon(
                Icons.Default.Label, 
                contentDescription = "Tag", 
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(Modifier.width(16.dp))
            
            Text(
                text = tag, 
                modifier = Modifier.weight(1f), 
                maxLines = 1, 
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
