Oczywiście, rozumiem. Pokazuję dokładnie, w których miejscach w kodzie zdefiniowany jest kolor tła dla wspomnianych komponentów i upewniam się, że używają one teraz koloru `CardBackground`.

Poniżej znajdują się precyzyjne fragmenty kodu oraz całe, poprawione pliki.

### Lokalizacja zmian w kodzie

1.  **Ekran "Przeglądaj" (Wszystkie dni i foldery)**
    *   **Plik:** `app/src/main/java/com/qjproject/liturgicalcalendar/ui/screens/browse/BrowseScreen.kt`
    *   **Komponent:** `BrowseItemView`
    *   **Dokładny kod:** Kolor tła jest ustawiany w parametrze `colors` komponentu `Card`.

    ```kotlin
    @Composable
    fun BrowseItemView(item: FileSystemItem, onClick: () -> Unit) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clickable(onClick = onClick),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            // ---> TUTAJ JEST DEFINICJA KOLORU TŁA <---
            colors = CardDefaults.cardColors(containerColor = CardBackground)
        ) {
            // ... reszta komponentu
        }
    }
    ```

2.  **Ekran "Szczegóły Dnia" (Sugerowane pieśni)**
    *   **Plik:** `app/src/main/java/com/qjproject/liturgicalcalendar/ui/screens/daydetails/DayDetailsScreen/DayDetailsViewMode.kt`
    *   **Komponent:** `SongItemView`
    *   **Dokładny kod:** Kolor tła jest ustawiany w parametrze `colors` komponentu `Card`.

    ```kotlin
    @Composable
    private fun SongItemView(displayableSong: DisplayableSuggestedSong, onClick: () -> Unit) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            // ---> TUTAJ JEST DEFINICJA KOLORU TŁA <---
            colors = CardDefaults.cardColors(containerColor = CardBackground)
        ) {
            // ... reszta komponentu
        }
    }
    ```

3.  **Ekran "Szukaj" (Wyniki wyszukiwania pieśni)**
    *   **Plik:** `app/src/main/java/com/qjproject/liturgicalcalendar/ui/screens/search/SearchScreen/SearchScreenComponents.kt`
    *   **Komponent:** `SongResultItem`
    *   **Dokładny kod:** Kolor tła jest ustawiany w parametrze `colors` komponentu `Card`.

    ```kotlin
    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun SongResultItem(
        song: Song,
        onClick: () -> Unit,
        onLongClick: () -> Unit
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            // ---> TUTAJ JEST DEFINICJA KOLORU TŁA <---
            colors = CardDefaults.cardColors(containerColor = CardBackground)
        ) {
            // ... reszta komponentu
        }
    }
    ```

### Zaktualizowane pliki

Poniżej znajdują się pełne pliki z wprowadzonymi poprawkami, abyś mógł je podmienić.

---
nazwa pliku: `C:\Users\blzej\Desktop\Aplikacja dla studenta\Aplikacja-dla-Konrada\app\src\main\java\com\qjproject\liturgicalcalendar\ui\screens\browse\BrowseScreen.kt`
treść:
```kotlin
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
import com.qjproject.liturgicalcalendar.ui.theme.CardBackground
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
        colors = CardDefaults.cardColors(containerColor = CardBackground)
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
```---
nazwa pliku: `C:\Users\blzej\Desktop\Aplikacja dla studenta\Aplikacja-dla-Konrada\app\src\main\java\com\qjproject\liturgicalcalendar\ui\screens\daydetails\DayDetailsScreen\DayDetailsViewMode.kt`
treść:
```kotlin
package com.qjproject.liturgicalcalendar.ui.screens.daydetails.daydetailsscreen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.qjproject.liturgicalcalendar.data.Reading
import com.qjproject.liturgicalcalendar.data.Song
import com.qjproject.liturgicalcalendar.data.SuggestedSong
import com.qjproject.liturgicalcalendar.ui.screens.daydetails.daydetailsviewmodel.DayDetailsViewModel
import com.qjproject.liturgicalcalendar.ui.screens.daydetails.daydetailsviewmodel.DisplayableSuggestedSong
import com.qjproject.liturgicalcalendar.ui.screens.daydetails.daydetailsviewmodel.songMomentOrderMap
import com.qjproject.liturgicalcalendar.ui.theme.CardBackground
import com.qjproject.liturgicalcalendar.ui.theme.SaturatedNavy
import com.qjproject.liturgicalcalendar.ui.theme.VeryDarkNavy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
internal fun DayDetailsViewModeContent(
    modifier: Modifier = Modifier,
    viewModel: DayDetailsViewModel,
    onNavigateToSongContent: (song: Song, startInEdit: Boolean) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSongModal by remember { mutableStateOf(false) }
    var selectedSong by remember { mutableStateOf<SuggestedSong?>(null) }
    var fullSelectedSong by remember { mutableStateOf<Song?>(null) }

    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val readingOffsetsY = remember { mutableStateMapOf<Int, Int>() }
    val interactionSources = remember { mutableStateMapOf<Int, MutableInteractionSource>() }
    var contentStartY by remember { mutableStateOf(0f) }
    var scrollAnchorCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    if (showSongModal && selectedSong != null && fullSelectedSong != null) {
        SongDetailsModal(
            suggestedSong = selectedSong!!,
            fullSong = fullSelectedSong!!,
            onDismiss = { showSongModal = false },
            onShowContent = onNavigateToSongContent
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState)
            .onGloballyPositioned { contentStartY = it.positionInRoot().y }
    ) {
        Spacer(
            modifier = Modifier
                .height(0.dp)
                .onGloballyPositioned { scrollAnchorCoordinates = it }
        )

        Spacer(modifier = Modifier.height(16.dp))
        HierarchicalCollapsibleSection(
            title = "Czytania",
            isExpanded = uiState.isReadingsSectionExpanded,
            onToggle = { viewModel.toggleReadingsSection() }
        ) {
            uiState.dayData?.czytania?.forEachIndexed { index, reading ->
                ReadingItemView(
                    reading = reading,
                    isExpanded = uiState.expandedReadings.contains(index),
                    onToggle = { viewModel.toggleReading(index) },
                    onContentDoubleTap = {
                        handleReadingCollapse(index, readingOffsetsY, viewModel, coroutineScope, scrollState)
                    },
                    onGloballyPositioned = { itemCoordinates ->
                        scrollAnchorCoordinates?.let { anchor ->
                            val offsetY = (itemCoordinates.positionInRoot().y - anchor.positionInRoot().y).toInt()
                            readingOffsetsY[index] = offsetY
                        }
                    },
                    interactionSource = interactionSources.getOrPut(index) { MutableInteractionSource() }
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        HierarchicalCollapsibleSection(
            title = "Sugerowane pieśni",
            isExpanded = uiState.isSongsSectionExpanded,
            onToggle = { viewModel.toggleSongsSection() }
        ) {
            val groupedSongs by viewModel.groupedSongs.collectAsState()
            songMomentOrderMap.forEach { (momentKey, momentName) ->
                SongGroupView(
                    momentName = momentName,
                    songs = groupedSongs[momentKey].orEmpty(),
                    isExpanded = uiState.expandedSongMoments.contains(momentKey),
                    onToggle = { viewModel.toggleSongMoment(momentKey) },
                    onSongClick = { suggested ->
                        viewModel.getFullSong(suggested.suggestedSong) { fullSong ->
                            if (fullSong != null) {
                                selectedSong = suggested.suggestedSong
                                fullSelectedSong = fullSong
                                showSongModal = true
                            }
                        }
                    }
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

private fun handleReadingCollapse(
    index: Int,
    readingOffsetsY: Map<Int, Int>,
    viewModel: DayDetailsViewModel,
    coroutineScope: CoroutineScope,
    scrollState: ScrollState
) {
    val currentOffset = readingOffsetsY[index] ?: 0
    val isExpanded = viewModel.uiState.value.expandedReadings.contains(index)
    if (isExpanded) {
        coroutineScope.launch {
            scrollState.animateScrollTo(currentOffset)
        }
    }
    viewModel.collapseReading(index)
}

@Composable
fun ReadingItemView(
    reading: Reading,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onContentDoubleTap: () -> Unit,
    onGloballyPositioned: (LayoutCoordinates) -> Unit,
    interactionSource: MutableInteractionSource
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned(onGloballyPositioned)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onToggle
                ),
            colors = CardDefaults.cardColors(containerColor = VeryDarkNavy)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = reading.typ,
                    style = MaterialTheme.typography.titleMedium,
                    color = SaturatedNavy
                )
                if (reading.sigla?.isNotBlank() == true) {
                    Text(
                        text = reading.sigla,
                        style = MaterialTheme.typography.bodySmall,
                        color = SaturatedNavy.copy(alpha = 0.8f)
                    )
                }
                if (reading.opis?.isNotBlank() == true) {
                    Text(
                        text = reading.opis,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Light,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
        AnimatedVisibility(visible = isExpanded) {
            Text(
                text = reading.tekst,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(onDoubleTap = { onContentDoubleTap() })
                    },
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun SongGroupView(
    momentName: String,
    songs: List<DisplayableSuggestedSong>,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onSongClick: (DisplayableSuggestedSong) -> Unit
) {
    if (songs.isNotEmpty()) {
        Column {
            val rotationAngle by animateFloatAsState(targetValue = if (isExpanded) 0f else -90f, label = "songArrowRotation")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(momentName, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Zwiń" else "Rozwiń",
                    modifier = Modifier.rotate(rotationAngle)
                )
            }
            AnimatedVisibility(visible = isExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    songs.forEach { song ->
                        SongItemView(displayableSong = song, onClick = { onSongClick(song) })
                    }
                }
            }
        }
    }
}

@Composable
private fun SongItemView(displayableSong: DisplayableSuggestedSong, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val iconColor = if (displayableSong.hasText) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            }
            Icon(Icons.Outlined.MusicNote, contentDescription = "Pieśń", tint = iconColor)
            Spacer(Modifier.width(16.dp))
            Text(displayableSong.suggestedSong.piesn, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
```
---
nazwa pliku: `C:\Users\blzej\Desktop\Aplikacja dla studenta\Aplikacja-dla-Konrada\app\src\main\java\com\qjproject\liturgicalcalendar\ui\screens\search\SearchScreen\SearchScreenComponents.kt`
treść:```kotlin
package com.qjproject.liturgicalcalendar.ui.screens.search

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.qjproject.liturgicalcalendar.data.Category
import com.qjproject.liturgicalcalendar.data.Song
import com.qjproject.liturgicalcalendar.ui.theme.CardBackground
import com.qjproject.liturgicalcalendar.ui.theme.VeryDarkNavy

@Composable
fun SearchBar(query: String, onQueryChange: (String) -> Unit, placeholder: String) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Ikona wyszukiwania") },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Wyczyść")
                }
            }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.extraLarge,
        textStyle = MaterialTheme.typography.bodyLarge
    )
}

@Composable
fun NoResults() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Brak wyników.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun SearchResultsContent(
    categories: List<Category>,
    songs: List<Song>,
    isGlobalSearch: Boolean,
    onCategoryClick: (Category) -> Unit,
    onNoCategoryClick: () -> Unit,
    onSongClick: (Song) -> Unit,
    onSongLongClick: (Song) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(
            start = 8.dp,
            end = 8.dp,
            top = 8.dp,
            bottom = 80.dp
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories, key = { "category_${it.nazwa}" }) { category ->
            CategoryItem(
                category = category,
                onClick = { onCategoryClick(category) }
            )
        }

        if (isGlobalSearch && categories.isNotEmpty()) {
            item { Divider(modifier = Modifier.padding(vertical = 4.dp)) }
            item {
                CategoryItem(
                    category = Category("Brak kategorii", ""),
                    onClick = onNoCategoryClick
                )
            }
        }

        if (songs.isNotEmpty()) {
            items(songs, key = { song ->
                "song_${song.tytul}_${song.numerSiedl}_${song.numerSAK}_${song.numerDN}"
            }) { song ->
                SongResultItem(
                    song = song,
                    onClick = { onSongClick(song) },
                    onLongClick = { onSongLongClick(song) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongResultItem(
    song: Song,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val iconColor = if (song.tekst.isNullOrBlank()) {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            } else {
                MaterialTheme.colorScheme.primary
            }
            Icon(Icons.Outlined.MusicNote, contentDescription = "Pieśń", tint = iconColor)
            Spacer(Modifier.width(16.dp))
            Text(song.tytul, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.width(8.dp))
            Text(song.kategoriaSkr, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun CategoryItem(
    category: Category,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = VeryDarkNavy)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Category,
                contentDescription = "Kategoria",
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = category.nazwa, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
```