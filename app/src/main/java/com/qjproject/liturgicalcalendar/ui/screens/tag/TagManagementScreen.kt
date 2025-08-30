package com.qjproject.liturgicalcalendar.ui.screens.tag

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.qjproject.liturgicalcalendar.ui.components.AutoResizingText
import com.qjproject.liturgicalcalendar.ui.theme.SaturatedNavy
import com.qjproject.liturgicalcalendar.ui.theme.VeryDarkNavy
import com.qjproject.liturgicalcalendar.ui.theme.VividRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagManagementScreen(
    viewModel: TagManagementViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    when (val dialogState = uiState.dialogState) {
        is TagDialogState.AddEdit -> {
            AddEditTagDialog(
                tag = dialogState.tag,
                error = uiState.error,
                onDismiss = { viewModel.dismissDialog() },
                onConfirm = { name ->
                    if (dialogState.tag == null) {
                        viewModel.addTag(name)
                    } else {
                        viewModel.updateTag(dialogState.tag, name)
                    }
                },
                onValidate = { name -> viewModel.validateTag(name, dialogState.tag) },
                onAssignToSongs = if (dialogState.tag != null) {
                    { viewModel.showAssignSongsDialog(dialogState.tag) }
                } else null
            )
        }
        is TagDialogState.AssignSongs -> {
            AssignSongsToTagDialog(
                tag = dialogState.tag,
                onDismiss = { viewModel.dismissDialog() }
            )
        }
        is TagDialogState.Delete -> {
            DeleteTagDialog(
                tag = dialogState.tag,
                onDismiss = { viewModel.dismissDialog() },
                onConfirm = { viewModel.deleteTag(dialogState.tag) }
            )
        }
        TagDialogState.None -> {}
    }

    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        AutoResizingText(
                            text = "Zarządzaj tagami",
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Wróć")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                        navigationIconContentColor = MaterialTheme.colorScheme.primary
                    ),
                    windowInsets = TopAppBarDefaults.windowInsets
                )
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.showAddDialog() }) {
                Icon(Icons.Default.Add, contentDescription = "Dodaj tag")
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.tags.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Brak tagów.\nDodaj pierwszy tag używając przycisku +",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 8.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.tags) { tag ->
                    TagItem(
                        tag = tag,
                        onEdit = { viewModel.showEditDialog(tag) },
                        onDelete = { viewModel.showDeleteDialog(tag) }
                    )
                }
            }
        }
    }
}

@Composable
fun TagItem(
    tag: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = VeryDarkNavy)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = tag,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Usuń",
                    tint = VividRed
                )
            }
        }
    }
}

@Composable
fun AddEditTagDialog(
    tag: String?,
    error: String?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    onValidate: (String) -> Unit,
    onAssignToSongs: (() -> Unit)? = null
) {
    var name by remember { mutableStateOf(tag ?: "") }
    val isEditing = tag != null

    LaunchedEffect(name) {
        if (name.isNotEmpty()) {
            onValidate(name)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = VeryDarkNavy)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEditing) "Edytuj tag" else "Dodaj nowy tag",
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                        color = SaturatedNavy
                    )
                    
                    if (onAssignToSongs != null) {
                        IconButton(onClick = onAssignToSongs) {
                            Icon(
                                Icons.Outlined.MusicNote,
                                contentDescription = "Przypisz do pieśni",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
                
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nazwa tagu") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = error != null,
                    supportingText = error?.let { { Text(it, color = VividRed) } }
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Anuluj")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(name) },
                        enabled = error == null && name.trim().isNotEmpty()
                    ) {
                        Text(if (isEditing) "Zapisz" else "Dodaj")
                    }
                }
            }
        }
    }
}

@Composable
fun DeleteTagDialog(
    tag: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = VeryDarkNavy)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Usuń tag",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                    color = SaturatedNavy
                )
                
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                
                Text(
                    text = buildAnnotatedString {
                        append("Czy na pewno chcesz usunąć tag ")
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append("\"$tag\"")
                        }
                        append("?")
                    }
                )
                
                Text(
                    text = "Ta operacja jest nieodwracalna.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Anuluj")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(containerColor = VividRed)
                    ) {
                        Text("Usuń")
                    }
                }
            }
        }
    }
}
