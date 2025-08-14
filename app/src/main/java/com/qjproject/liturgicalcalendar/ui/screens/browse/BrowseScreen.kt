package com.qjproject.liturgicalcalendar.ui.screens.browse

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.qjproject.liturgicalcalendar.data.FileSystemItem
import com.qjproject.liturgicalcalendar.ui.theme.SaturatedNavy
import com.qjproject.liturgicalcalendar.ui.theme.VeryDarkNavy
import org.burnoutcrew.reorderable.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BrowseScreen(
    viewModel: BrowseViewModel,
    onNavigateToDay: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    HandleDialogs(viewModel, uiState)

    val reorderState = rememberReorderableLazyListState(onMove = { from, to -> viewModel.reorderItems(from, to) })

    LazyColumn(
        state = if (uiState.canReorder) reorderState.listState else rememberLazyListState(),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp)
            .then(if (uiState.canReorder) Modifier.reorderable(reorderState) else Modifier),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        itemsIndexed(uiState.items, key = { _, item -> item.path }) { index, item ->
            if (uiState.isEditMode) {
                ReorderableItem(reorderState, key = item.path) { isDragging ->
                    BrowseItemEditable(
                        item = item,
                        isDragging = isDragging,
                        canReorder = uiState.canReorder,
                        onRenameClick = { viewModel.showDialog(BrowseDialogState.RenameItem(item, index)) },
                        onDeleteClick = { viewModel.showDialog(BrowseDialogState.ConfirmDelete(item, index)) },
                        reorderModifier = Modifier.detectReorder(reorderState)
                    )
                }
            } else {
                BrowseItemView(
                    item = item,
                    onClick = {
                        if (item.isDirectory) {
                            viewModel.onDirectoryClick(item.name)
                        } else {
                            onNavigateToDay(item.path)
                        }
                    }
                )
            }
        }

        if (uiState.isEditMode) {
            item {
                AddItemTile(onClick = { viewModel.showDialog(BrowseDialogState.AddOptions) })
            }
        }
    }
}

@Composable
private fun HandleDialogs(viewModel: BrowseViewModel, uiState: BrowseUiState) {
    when (val dialog = uiState.activeDialog) {
        is BrowseDialogState.AddOptions -> AddOptionsDialog(
            onDismiss = { viewModel.dismissDialog() },
            onCreateFolder = { viewModel.showDialog(BrowseDialogState.CreateFolder) },
            onCreateDay = { viewModel.showDialog(BrowseDialogState.CreateDay) }
        )
        is BrowseDialogState.CreateFolder -> CreateItemDialog(
            title = "Utwórz nowy folder",
            label = "Nazwa folderu",
            error = uiState.operationError,
            onDismiss = { viewModel.dismissDialog() },
            onValueChange = { newName -> viewModel.onNewItemNameChange(newName) },
            onConfirm = { name -> viewModel.createFolder(name) }
        )
        is BrowseDialogState.CreateDay -> CreateItemDialog(
            title = "Utwórz nowy dzień",
            label = "Nazwa dnia",
            error = uiState.operationError,
            onDismiss = { viewModel.dismissDialog() },
            onValueChange = { newName -> viewModel.onNewItemNameChange(newName) },
            onConfirm = { name -> viewModel.createDay(name, null) }
        )
        is BrowseDialogState.RenameItem -> RenameItemDialog(
            item = dialog.item,
            error = uiState.operationError,
            onDismiss = { viewModel.dismissDialog() },
            onValueChange = { newName -> viewModel.onNewItemNameChange(newName) },
            onConfirm = { newName -> viewModel.renameItem(dialog.index, newName) }
        )
        is BrowseDialogState.ConfirmDelete -> ConfirmDeleteDialog(
            item = dialog.item,
            onDismiss = { viewModel.dismissDialog() },
            onConfirm = { viewModel.deleteItem(dialog.index) }
        )
        is BrowseDialogState.None -> {}
    }

    if (uiState.showConfirmExitDialog) {
        ConfirmExitEditModeDialog(
            onDismiss = { viewModel.dismissConfirmExitDialog() },
            onDiscard = { viewModel.onCancelEditMode(isFromDialog = true) }
        )
    }
}

@Composable
fun BrowseItemView(item: FileSystemItem, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
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
fun BrowseItemEditable(
    item: FileSystemItem,
    isDragging: Boolean,
    canReorder: Boolean,
    onRenameClick: () -> Unit,
    onDeleteClick: () -> Unit,
    reorderModifier: Modifier
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .shadow(if (isDragging) 4.dp else 0.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (canReorder) {
                Icon(
                    imageVector = Icons.Default.DragHandle,
                    contentDescription = "Zmień kolejność",
                    modifier = reorderModifier.padding(8.dp)
                )
            } else {
                Spacer(Modifier.width(40.dp)) // Placeholder
            }

            Icon(
                imageVector = if (item.isDirectory) Icons.Outlined.Folder else Icons.Outlined.Article,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = item.name.replace("_", " "),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            IconButton(onClick = onRenameClick) {
                Icon(Icons.Default.Edit, "Zmień nazwę")
            }
            IconButton(onClick = onDeleteClick) {
                Icon(Icons.Default.Delete, "Usuń")
            }
        }
    }
}

@Composable
fun AddItemTile(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Box(
            modifier = Modifier
                .height(64.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Dodaj nowy element",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// --- DIALOGS ---

@Composable
private fun ConfirmExitEditModeDialog(onDismiss: () -> Unit, onDiscard: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = VeryDarkNavy)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    "Anulować zmiany?",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                    color = SaturatedNavy
                )
                Spacer(Modifier.height(16.dp))
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                Spacer(Modifier.height(16.dp))
                Text("Masz niezapisane zmiany. Czy na pewno chcesz wyjść i je odrzucić?")
                Spacer(Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Zostań", fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = onDiscard) {
                        Text("Odrzuć", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfirmDeleteDialog(item: FileSystemItem, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = VeryDarkNavy)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    "Potwierdź usunięcie",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                    color = SaturatedNavy
                )
                Spacer(Modifier.height(16.dp))
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                Spacer(Modifier.height(16.dp))
                Text("Czy na pewno chcesz usunąć '${item.name}'? Ta operacja jest nieodwracalna.")
                Spacer(Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Anuluj") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Usuń") }
                }
            }
        }
    }
}

@Composable
private fun RenameItemDialog(
    item: FileSystemItem,
    error: String?,
    onDismiss: () -> Unit,
    onValueChange: (String) -> Unit,
    onConfirm: (String) -> Unit
) {
    CreateItemDialog(
        title = "Zmień nazwę",
        label = "Nowa nazwa",
        initialValue = item.name,
        error = error,
        onDismiss = onDismiss,
        onValueChange = onValueChange,
        onConfirm = onConfirm
    )
}

@Composable
private fun AddOptionsDialog(onDismiss: () -> Unit, onCreateFolder: () -> Unit, onCreateDay: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = VeryDarkNavy)
        ) {
            Column(Modifier.padding(24.dp)) {
                Text(
                    "Co chcesz utworzyć?",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                    color = SaturatedNavy
                )
                Spacer(Modifier.height(16.dp))
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                Spacer(Modifier.height(24.dp))
                Button(onClick = onCreateFolder, modifier = Modifier.fillMaxWidth()) {
                    Text("Nowy folder")
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = onCreateDay, modifier = Modifier.fillMaxWidth()) {
                    Text("Nowy dzień")
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
private fun CreateItemDialog(
    title: String,
    label: String,
    initialValue: String = "",
    error: String?,
    onDismiss: () -> Unit,
    onValueChange: (String) -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(initialValue) }
    LaunchedEffect(Unit) { onValueChange(initialValue) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = VeryDarkNavy)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                    color = SaturatedNavy
                )
                Spacer(Modifier.height(16.dp))
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                Spacer(Modifier.height(16.dp))
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            onValueChange(it)
                        },
                        label = { Text(label) },
                        singleLine = true,
                        isError = error != null,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            if (name.isNotBlank() && error == null) onConfirm(name)
                        })
                    )
                    if (error != null) {
                        Text(
                            error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Anuluj") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(name) },
                        enabled = name.isNotBlank() && error == null
                    ) { Text("Zatwierdź") }
                }
            }
        }
    }
}