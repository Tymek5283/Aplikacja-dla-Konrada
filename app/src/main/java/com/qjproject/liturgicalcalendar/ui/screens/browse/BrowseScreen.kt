package com.qjproject.liturgicalcalendar.ui.screens.browse

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.qjproject.liturgicalcalendar.data.FileSystemItem

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BrowseScreen(
    viewModel: BrowseViewModel,
    onNavigateToDay: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    BackHandler(enabled = uiState.isBackArrowVisible) {
        viewModel.onBackPress()
    }

    when (uiState.activeDialog) {
        is BrowseDialogState.AddOptions -> {
            AddOptionsDialog(
                onDismiss = { viewModel.dismissDialog() },
                onCreateFolder = { viewModel.showCreateFolderDialog() },
                onCreateDay = { viewModel.showCreateDayDialog() }
            )
        }
        is BrowseDialogState.CreateFolder -> {
            CreateFolderDialog(
                error = uiState.operationError,
                onDismiss = { viewModel.dismissDialog() },
                onConfirm = { name -> viewModel.createFolder(name) }
            )
        }
        is BrowseDialogState.CreateDay -> {
            CreateDayDialog(
                error = uiState.operationError,
                onDismiss = { viewModel.dismissDialog() },
                onConfirm = { name, url -> viewModel.createDay(name, url) }
            )
        }
        is BrowseDialogState.None -> {}
    }

    Column {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(uiState.items, key = { it.name }) { item ->
                BrowseItem(
                    item = item,
                    onClick = {
                        if (item.isDirectory) {
                            viewModel.onDirectoryClick(item.name)
                        } else {
                            val fullPath = (uiState.currentPath + item.name).joinToString("/")
                            onNavigateToDay(fullPath)
                        }
                    },
                    modifier = Modifier.animateItemPlacement(
                        animationSpec = tween(durationMillis = 250)
                    )
                )
            }
        }
    }
}

@Composable
fun BrowseItem(
    item: FileSystemItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (item.isDirectory) Icons.Outlined.Folder else Icons.Outlined.Article,
                contentDescription = if (item.isDirectory) "Folder" else "Dzień",
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = item.name.replace("_", " "),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AddOptionsDialog(
    onDismiss: () -> Unit,
    onCreateFolder: () -> Unit,
    onCreateDay: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(Modifier.padding(24.dp)) {
                Text("Co chcesz utworzyć?", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(24.dp))
                Button(onClick = onCreateFolder, modifier = Modifier.fillMaxWidth()) {
                    Text("Utwórz folder")
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = onCreateDay, modifier = Modifier.fillMaxWidth()) {
                    Text("Utwórz dzień")
                }
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Anuluj")
                }
            }
        }
    }
}

@Composable
private fun CreateFolderDialog(
    error: String?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Utwórz nowy folder") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nazwa folderu") },
                    singleLine = true,
                    isError = error != null,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        onConfirm(name)
                        keyboardController?.hide()
                    })
                )
                if (error != null) {
                    Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(name) }) {
                Text("Utwórz")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )
}

@Composable
private fun CreateDayDialog(
    error: String?,
    onDismiss: () -> Unit,
    onConfirm: (String, String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Utwórz nowy dzień") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nazwa dnia (wymagane)") },
                    singleLine = true,
                    isError = error != null,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL do czytań (opcjonalnie)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onConfirm(name, url.ifBlank { null }) })
                )
                if (error != null) {
                    Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(name, url.ifBlank { null }) }) {
                Text("Utwórz")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )
}