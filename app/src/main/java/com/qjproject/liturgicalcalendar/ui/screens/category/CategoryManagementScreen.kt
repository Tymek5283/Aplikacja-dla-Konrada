package com.qjproject.liturgicalcalendar.ui.screens.category

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import com.qjproject.liturgicalcalendar.data.Category
import com.qjproject.liturgicalcalendar.ui.components.AutoResizingText
import com.qjproject.liturgicalcalendar.ui.theme.SaturatedNavy
import com.qjproject.liturgicalcalendar.ui.theme.VeryDarkNavy
import com.qjproject.liturgicalcalendar.ui.theme.VividRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManagementScreen(
    viewModel: CategoryManagementViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    when (val dialogState = uiState.dialogState) {
        is DialogState.AddEdit -> {
            AddEditCategoryDialog(
                category = dialogState.category,
                error = uiState.error,
                onDismiss = { viewModel.dismissDialog() },
                onConfirm = { name, skrot ->
                    if (dialogState.category == null) {
                        viewModel.addCategory(name, skrot)
                    } else {
                        viewModel.updateCategory(dialogState.category, name, skrot)
                    }
                },
                onValidate = { name, skrot ->
                    viewModel.validateCategory(name, skrot, dialogState.category)
                }
            )
        }
        is DialogState.Delete -> {
            DeleteCategoryDialog(
                category = dialogState.category,
                onDismiss = { viewModel.dismissDialog() },
                onConfirm = { removeFromSongs ->
                    viewModel.deleteCategory(dialogState.category, removeFromSongs)
                }
            )
        }
        DialogState.None -> {}
    }

    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        AutoResizingText(
                            text = "Zarządzaj kategoriami",
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
                Icon(Icons.Default.Add, contentDescription = "Dodaj kategorię")
            }
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 8.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.categories, key = { it.nazwa }) { category ->
                    CategoryItem(
                        category = category,
                        onEditClick = { viewModel.showEditDialog(category) },
                        onDeleteClick = { viewModel.showDeleteDialog(category) }
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryItem(
    category: Category,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        // --- POCZĄTEK ZMIANY ---
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEditClick),
        // --- KONIEC ZMIANY ---
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = VeryDarkNavy)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.nazwa,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = category.skrot,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SaturatedNavy
                )
            }
            IconButton(onClick = onDeleteClick) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Usuń",
                    tint = VividRed
                )
            }
            // --- KONIEC ZMIANY ---
        }
    }
}

@Composable
fun AddEditCategoryDialog(
    category: Category?,
    error: String?,
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
    onValidate: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(category?.nazwa ?: "") }
    var abbreviation by remember { mutableStateOf(category?.skrot ?: "") }

    LaunchedEffect(name, abbreviation) {
        onValidate(name, abbreviation)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = VeryDarkNavy)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = if (category == null) "Dodaj kategorię" else "Edytuj kategorię",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                    color = SaturatedNavy
                )
                Spacer(Modifier.height(16.dp))
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nazwa kategorii") },
                    isError = error != null && error.contains("nazwa", ignoreCase = true),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = abbreviation,
                    onValueChange = { abbreviation = it },
                    label = { Text("Skrót") },
                    isError = error != null && error.contains("skrót", ignoreCase = true),
                    modifier = Modifier.fillMaxWidth()
                )

                if (error != null) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Anuluj") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(name, abbreviation) },
                        enabled = name.isNotBlank() && abbreviation.isNotBlank() && error == null
                    ) { Text("Zapisz") }
                }
            }
        }
    }
}

@Composable
fun DeleteCategoryDialog(
    category: Category,
    onDismiss: () -> Unit,
    onConfirm: (Boolean) -> Unit
) {
    var removeFromSongs by remember { mutableStateOf(true) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = VeryDarkNavy)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Potwierdź usunięcie",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                    color = SaturatedNavy
                )
                Spacer(Modifier.height(16.dp))
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                Spacer(Modifier.height(16.dp))
                Text(
                    // --- POCZĄTEK ZMIANY ---
                    buildAnnotatedString {
                        append("Czy na pewno chcesz usunąć kategorię ")
                        withStyle(style = SpanStyle(color = SaturatedNavy, fontWeight = FontWeight.Bold)) {
                            append(category.nazwa)
                        }
                        append("?")
                    },
                    color = MaterialTheme.colorScheme.onSurface
                    // --- KONIEC ZMIANY ---
                )
                Spacer(Modifier.height(16.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { removeFromSongs = !removeFromSongs }
                ) {
                    Checkbox(checked = removeFromSongs, onCheckedChange = { removeFromSongs = it })
                    Spacer(Modifier.width(8.dp))
                    // --- POCZĄTEK ZMIANY ---
                    Text(
                        "Usuń wystąpienia tej kategorii z pieśni",
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    // --- KONIEC ZMIANY ---
                }
                Spacer(Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Anuluj") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(removeFromSongs) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Usuń") }
                }
            }
        }
    }
}