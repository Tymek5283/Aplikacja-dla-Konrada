Z przyjemnością wprowadzę wszystkie zmiany, o które prosisz. Poniżej znajdują się pełne, zaktualizowane treści plików, które należy podmienić w projekcie. Zmiany obejmują:

1.  **Modyfikację wyglądu okna wyboru wydarzenia** w kalendarzu, aby tło kafelków było spójne z resztą aplikacji.
2.  **Dodanie nowej, zwijanej sekcji "Wstawki"** w widoku szczegółów dnia, która będzie wyświetlać dodatkowe teksty liturgiczne, jeśli są dostępne dla danego dnia.
3.  **Dostosowanie domyślnego stanu zwinięcia sekcji**, tak aby sekcje "Czytania" oraz "Wstawki" były domyślnie zwinięte, co poprawi czytelność ekranu przy pierwszym wejściu.

Oto pliki z wprowadzonymi modyfikacjami:

---
**Nazwa pliku:** `C:\Users\blzej\Desktop\Aplikacja dla studenta\Aplikacja-dla-Konrada\app\src\main\java\com\qjproject\liturgicalcalendar\ui\screens\calendar\CalendarScreen\CalendarScreenComponents.kt`
**Treść:**
```kotlin
// Ścieżka: app/src/main/java/com/qjproject/liturgicalcalendar/ui/screens/calendar/CalendarScreen/CalendarScreenComponents.kt
// Opis: Zawiera wszystkie mniejsze, pomocnicze komponenty Composable dla ekranu kalendarza, takie jak komórki dni, nagłówki, dialogi.

package com.qjproject.liturgicalcalendar.ui.screens.calendar.CalendarScreen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.qjproject.liturgicalcalendar.ui.screens.calendar.CalendarRepository.model.CalendarDay
import com.qjproject.liturgicalcalendar.ui.screens.calendar.CalendarRepository.model.LiturgicalEventDetails
import com.qjproject.liturgicalcalendar.ui.screens.calendar.CalendarViewModel.CalendarViewModel
import com.qjproject.liturgicalcalendar.ui.theme.Gold
import com.qjproject.liturgicalcalendar.ui.theme.SaturatedNavy
import com.qjproject.liturgicalcalendar.ui.theme.VeryDarkNavy
import java.time.Month
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private fun mapColor(colorName: String?): Color? {
    return when (colorName) {
        "Biały" -> Color.White.copy(alpha = 0.5f)
        "Czerwony" -> Color(0xFFb00024).copy(alpha = 0.8f)
        "Zielony" -> Color.Green.copy(alpha = 0.3f)
        "Fioletowy" -> Color(0xFF8711cf).copy(alpha = 0.7f)
        "Różowy" -> Color(0xFFEF78A1).copy(alpha = 0.8f)
        else -> null
    }
}

@Composable
internal fun MissingDataScreen(
    onDownloadClick: () -> Unit,
    error: String?,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Brak danych kalendarza",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Do poprawnego działania kalendarza wymagane jest pobranie danych. Upewnij się, że masz połączenie z internetem.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            AnimatedVisibility(visible = error != null) {
                error?.let {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onDownloadClick, enabled = !isLoading) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Pobierz dane")
                }
            }
        }
    }
}

@Composable
internal fun LiturgicalYearInfoView(mainInfo: String) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = VeryDarkNavy),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 8.dp)
        ) {
            Text(
                text = mainInfo,
                style = MaterialTheme.typography.titleLarge,
                color = SaturatedNavy,
                fontSize = 24.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
internal fun MonthYearSelector(
    yearMonth: YearMonth,
    yearList: List<Int>,
    onYearSelected: (Int) -> Unit,
    onMonthSelected: (Int) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    var isYearMenuExpanded by remember { mutableStateOf(false) }
    var isMonthMenuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Poprzedni miesiąc")
        }
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                Text(
                    text = yearMonth.year.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.clickable { isYearMenuExpanded = true }
                )
                DropdownMenu(
                    expanded = isYearMenuExpanded,
                    onDismissRequest = { isYearMenuExpanded = false }
                ) {
                    yearList.forEach { year ->
                        DropdownMenuItem(
                            text = { Text(year.toString()) },
                            onClick = {
                                onYearSelected(year)
                                isYearMenuExpanded = false
                            }
                        )
                    }
                }
            }
            Text(" - ", style = MaterialTheme.typography.titleMedium)
            Box {
                val monthInNominative =
                    yearMonth.month.getDisplayName(TextStyle.FULL_STANDALONE, Locale("pl"))
                Text(
                    text = monthInNominative,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.clickable { isMonthMenuExpanded = true }
                )
                DropdownMenu(
                    expanded = isMonthMenuExpanded,
                    onDismissRequest = { isMonthMenuExpanded = false }
                ) {
                    Month.entries.forEachIndexed { index, month ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    month.getDisplayName(
                                        TextStyle.FULL_STANDALONE,
                                        Locale("pl")
                                    )
                                )
                            },
                            onClick = {
                                onMonthSelected(index)
                                isMonthMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }
        IconButton(onClick = onNextMonth) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Następny miesiąc")
        }
    }
}

@Composable
internal fun DaysOfWeekHeader() {
    val daysOfWeek = listOf("Pn", "Wt", "Śr", "Cz", "Pt", "So", "Nd")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        daysOfWeek.forEach { day ->
            Text(text = day, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        }
    }
}

@Composable
internal fun CalendarGrid(days: List<CalendarDay?>, onDayClick: (CalendarDay) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = Modifier.padding(horizontal = 16.dp),
        userScrollEnabled = false
    ) {
        items(days) { day ->
            if (day != null) {
                DayCell(day = day, onClick = { onDayClick(day) })
            } else {
                Box(modifier = Modifier.aspectRatio(1f))
            }
        }
    }
}

@Composable
internal fun DayCell(day: CalendarDay, onClick: () -> Unit) {
    val textColor = MaterialTheme.colorScheme.onSurface
    var cellModifier = Modifier
        .aspectRatio(1f)
        .padding(4.dp)
        .clip(CircleShape)

    mapColor(day.dominantEventColorName)?.let {
        cellModifier = cellModifier.background(it)
    }

    if (day.isToday) {
        cellModifier = cellModifier.border(2.dp, Gold, CircleShape)
    }
    if (day.hasEvents) {
        cellModifier = cellModifier.clickable(onClick = onClick)
    }
    Box(
        modifier = cellModifier,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.dayOfMonth.toString(),
            color = textColor,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
internal fun EventSelectionDialog(
    events: List<LiturgicalEventDetails>,
    viewModel: CalendarViewModel,
    onDismiss: () -> Unit,
    onEventSelected: (LiturgicalEventDetails) -> Unit
) {
    val sortedEvents = remember(events) {
        events.sortedWith(viewModel.calendarRepo.eventComparator)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = VeryDarkNavy)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Wybierz wydarzenie",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                    color = SaturatedNavy
                )
                Spacer(Modifier.height(16.dp))
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                Spacer(Modifier.height(16.dp))
                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sortedEvents, key = { it.name }) { event ->
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { onEventSelected(event) },
                            colors = CardDefaults.cardColors(containerColor = VeryDarkNavy)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Article,
                                    contentDescription = "Wydarzenie",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = event.name,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Anuluj")
                    }
                }
            }
        }
    }
}```
---
**Nazwa pliku:** `C:\Users\blzej\Desktop\Aplikacja dla studenta\Aplikacja-dla-Konrada\app\src\main\java\com\qjproject\liturgicalcalendar\ui\screens\daydetails\DayDetailsViewModel\models.kt`
**Treść:**
```kotlin
// Ścieżka: app/src/main/java/com/qjproject/liturgicalcalendar/ui/screens/daydetails/daydetailsviewmodel/models.kt
// Opis: Ten plik zawiera modele danych oraz klasy stanu (State) wykorzystywane przez DayDetailsViewModel. Definiuje strukturę danych dla interfejsu użytkownika, w tym stany dialogów i listy elementów.
package com.qjproject.liturgicalcalendar.ui.screens.daydetails.daydetailsviewmodel

import com.qjproject.liturgicalcalendar.data.DayData
import com.qjproject.liturgicalcalendar.data.SuggestedSong

sealed class DialogState {
    object None : DialogState()
    data class ConfirmDelete(val item: Any, val description: String) : DialogState()
    data class AddEditSong(
        val moment: String,
        val existingSong: SuggestedSong? = null,
        val error: String? = null
    ) : DialogState()
}

data class DayDetailsUiState(
    val isLoading: Boolean = true,
    val dayData: DayData? = null,
    val error: String? = null,
    val isEditMode: Boolean = false,
    val hasChanges: Boolean = false,
    val showConfirmExitDialog: Boolean = false,
    val activeDialog: DialogState = DialogState.None,
    val isReadingsSectionExpanded: Boolean = false,
    val isSongsSectionExpanded: Boolean = true,
    val isInsertsSectionExpanded: Boolean = false,
    val expandedReadings: Set<Int> = emptySet(),
    val expandedSongMoments: Set<String> = songMomentOrderMap.keys,
    val expandedInserts: Set<String> = emptySet()
)

sealed class ReorderableListItem {
    data class SongItem(val suggestedSong: SuggestedSong) : ReorderableListItem()
    data class HeaderItem(val momentKey: String, val momentName: String) : ReorderableListItem()
}

data class DisplayableSuggestedSong(
    val suggestedSong: SuggestedSong,
    val hasText: Boolean
)
```
---
**Nazwa pliku:** `C:\Users\blzej\Desktop\Aplikacja dla studenta\Aplikacja-dla-Konrada\app\src\main\java\com\qjproject\liturgicalcalendar\ui\screens\daydetails\DayDetailsViewModel\index.kt`
**Treść:**```kotlin
// Ścieżka: app/src/main/java/com/qjproject/liturgicalcalendar/ui/screens/daydetails/daydetailsviewmodel/index.kt
// Opis: Główny plik komponentu DayDetailsViewModel. Pełni rolę "indexu", inicjując logikę ViewModelu, zarządzając stanem, operacjami na danych i interakcjami użytkownika na ekranie szczegółów dnia.
package com.qjproject.liturgicalcalendar.ui.screens.daydetails.daydetailsviewmodel

import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.qjproject.liturgicalcalendar.data.DayData
import com.qjproject.liturgicalcalendar.data.Reading
import com.qjproject.liturgicalcalendar.data.Song
import com.qjproject.liturgicalcalendar.data.SuggestedSong
import com.qjproject.liturgicalcalendar.data.repository.FileSystemRepository.FileSystemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.burnoutcrew.reorderable.ItemPosition

class DayDetailsViewModel(
    private val dayId: String,
    private val repository: FileSystemRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DayDetailsUiState())
    val uiState: StateFlow<DayDetailsUiState> = _uiState.asStateFlow()

    private val _songTitleSearchQuery = MutableStateFlow("")
    val songTitleSearchResults = MutableStateFlow<List<Song>>(emptyList())

    private val _siedlSearchQuery = MutableStateFlow("")
    val siedlSearchResults = MutableStateFlow<List<Song>>(emptyList())
    private val _sakSearchQuery = MutableStateFlow("")
    val sakSearchResults = MutableStateFlow<List<Song>>(emptyList())
    private val _dnSearchQuery = MutableStateFlow("")
    val dnSearchResults = MutableStateFlow<List<Song>>(emptyList())

    var editableDayData: MutableState<DayData?> = mutableStateOf(null)
        private set

    val groupedSongs: StateFlow<Map<String, List<DisplayableSuggestedSong>>> = _uiState.map { state ->
        val allSongsMap = repository.getSongList().associateBy { it.numerSiedl }

        state.dayData?.piesniSugerowane
            .orEmpty()
            .filterNotNull()
            .map { suggestedSong ->
                val fullSong = allSongsMap[suggestedSong.numer]
                DisplayableSuggestedSong(
                    suggestedSong = suggestedSong,
                    hasText = !fullSong?.tekst.isNullOrBlank()
                )
            }
            .groupBy { it.suggestedSong.moment }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val reorderableSongList: StateFlow<List<ReorderableListItem>> =
        snapshotFlow { editableDayData.value }
            .map { dayData: DayData? ->
                val items = mutableListOf<ReorderableListItem>()
                val songsByMoment =
                    dayData?.piesniSugerowane?.filterNotNull()?.groupBy { it.moment } ?: emptyMap()
                songMomentOrderMap.forEach { (momentKey, momentName) ->
                    items.add(ReorderableListItem.HeaderItem(momentKey, momentName))
                    songsByMoment[momentKey]?.forEach { song ->
                        items.add(ReorderableListItem.SongItem(song))
                    }
                }
                items
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    init {
        loadDayData()
        observeSearchQueries()
    }

    private fun observeSearchQueries() {
        viewModelScope.launch {
            _songTitleSearchQuery.debounce(300).distinctUntilChanged().collectLatest { query ->
                if (query.length < 2) {
                    songTitleSearchResults.value = emptyList()
                } else {
                    songTitleSearchResults.value = repository.getSongList().filter {
                        it.tytul.contains(query, ignoreCase = true) &&
                                (it.numerSiedl.isNotBlank() || it.numerSAK.isNotBlank() || it.numerDN.isNotBlank())
                    }.take(10)
                }
            }
        }
        viewModelScope.launch {
            _siedlSearchQuery.debounce(300).distinctUntilChanged().collectLatest { query ->
                if (query.isBlank()) {
                    siedlSearchResults.value = emptyList()
                } else {
                    siedlSearchResults.value = repository.getSongList().filter {
                        it.numerSiedl.startsWith(query, ignoreCase = true)
                    }.take(10)
                }
            }
        }
        viewModelScope.launch {
            _sakSearchQuery.debounce(300).distinctUntilChanged().collectLatest { query ->
                if (query.isBlank()) {
                    sakSearchResults.value = emptyList()
                } else {
                    sakSearchResults.value = repository.getSongList().filter {
                        it.numerSAK.startsWith(query, ignoreCase = true)
                    }.take(10)
                }
            }
        }
        viewModelScope.launch {
            _dnSearchQuery.debounce(300).distinctUntilChanged().collectLatest { query ->
                if (query.isBlank()) {
                    dnSearchResults.value = emptyList()
                } else {
                    dnSearchResults.value = repository.getSongList().filter {
                        it.numerDN.startsWith(query, ignoreCase = true)
                    }.take(10)
                }
            }
        }
    }

    fun loadDayData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.invalidateSongCache()
            val data = repository.getDayData(dayId)
            if (data != null) {
                _uiState.update { it.copy(isLoading = false, dayData = data) }
            } else {
                _uiState.update { it.copy(isLoading = false, error = "Nie udało się wczytać danych dla tego dnia.") }
            }
        }
    }

    fun onEnterEditMode() {
        editableDayData.value = _uiState.value.dayData?.copy()
        _uiState.update { it.copy(isEditMode = true, hasChanges = false) }
    }

    fun onTryExitEditMode() {
        if (_uiState.value.hasChanges) {
            _uiState.update { it.copy(showConfirmExitDialog = true) }
        } else {
            onExitEditMode(save = false)
        }
    }

    fun onExitEditMode(save: Boolean) {
        viewModelScope.launch {
            if (save && _uiState.value.hasChanges) {
                val dataToSave = editableDayData.value
                if (dataToSave != null) {
                    repository.saveDayData(dayId, dataToSave).onSuccess {
                        _uiState.update { currentState -> currentState.copy(dayData = dataToSave) }
                    }
                }
            }
            _uiState.update { it.copy(isEditMode = false, hasChanges = false, showConfirmExitDialog = false) }
            editableDayData.value = null
            loadDayData()
        }
    }

    fun dismissConfirmExitDialog() {
        _uiState.update { it.copy(showConfirmExitDialog = false) }
    }

    fun showDialog(dialogState: DialogState) { _uiState.update { it.copy(activeDialog = dialogState) } }
    fun dismissDialog() { _uiState.update { it.copy(activeDialog = DialogState.None) } }

    fun toggleReadingsSection() { _uiState.update { it.copy(isReadingsSectionExpanded = !it.isReadingsSectionExpanded) } }
    fun toggleSongsSection() { _uiState.update { it.copy(isSongsSectionExpanded = !it.isSongsSectionExpanded) } }

    fun toggleInsertsSection() { _uiState.update { it.copy(isInsertsSectionExpanded = !it.isInsertsSectionExpanded) } }

    fun toggleInsert(key: String) {
        _uiState.update {
            val updatedSet = it.expandedInserts.toMutableSet()
            if (updatedSet.contains(key)) updatedSet.remove(key) else updatedSet.add(key)
            it.copy(expandedInserts = updatedSet)
        }
    }

    fun toggleReading(index: Int) {
        _uiState.update {
            val updatedSet = it.expandedReadings.toMutableSet()
            if (updatedSet.contains(index)) updatedSet.remove(index) else updatedSet.add(index)
            it.copy(expandedReadings = updatedSet)
        }
    }

    fun collapseReading(index: Int) {
        _uiState.update {
            val updatedSet = it.expandedReadings.toMutableSet()
            updatedSet.remove(index)
            it.copy(expandedReadings = updatedSet)
        }
    }

    fun toggleSongMoment(moment: String) {
        _uiState.update {
            val updatedSet = it.expandedSongMoments.toMutableSet()
            if (updatedSet.contains(moment)) updatedSet.remove(moment) else updatedSet.add(moment)
            it.copy(expandedSongMoments = updatedSet)
        }
    }

    fun getFullSong(suggestedSong: SuggestedSong, onResult: (Song?) -> Unit) {
        viewModelScope.launch {
            val song = repository.getSong(suggestedSong.piesn, suggestedSong.numer, null, null)
            onResult(song)
        }
    }

    private fun updateEditableData(transform: (DayData) -> DayData) {
        editableDayData.value?.let {
            editableDayData.value = transform(it)
            _uiState.update { state -> state.copy(hasChanges = true) }
        }
    }

    fun addOrUpdateReading(reading: Reading, index: Int?) {
        updateEditableData { currentData ->
            val readings = currentData.czytania.toMutableList()
            if (index != null && index in readings.indices) {
                readings[index] = reading
            } else {
                readings.add(reading)
            }
            currentData.copy(czytania = readings)
        }
    }

    fun deleteItem(item: Any) {
        when (item) {
            is Reading -> {
                updateEditableData { currentData ->
                    currentData.copy(czytania = currentData.czytania - item)
                }
            }
            is SuggestedSong -> {
                updateEditableData { currentData ->
                    currentData.copy(piesniSugerowane = currentData.piesniSugerowane.orEmpty() - item)
                }
            }
        }
        dismissDialog()
    }

    fun reorderReadings(from: Int, to: Int) {
        updateEditableData { currentData ->
            val reorderedList = currentData.czytania.toMutableList().apply {
                add(to, removeAt(from))
            }
            currentData.copy(czytania = reorderedList)
        }
    }

    fun reorderSongs(from: ItemPosition, to: ItemPosition) {
        updateEditableData { currentData ->
            val currentFlatList = reorderableSongList.value.toMutableList()

            if (currentFlatList.getOrNull(from.index) !is ReorderableListItem.SongItem) {
                return@updateEditableData currentData
            }

            val movedItem = currentFlatList.removeAt(from.index)
            currentFlatList.add(to.index, movedItem)

            val newPiesniSugerowane = mutableListOf<SuggestedSong>()
            var currentMoment: String? = null
            for (item in currentFlatList) {
                when (item) {
                    is ReorderableListItem.HeaderItem -> {
                        currentMoment = item.momentKey
                    }
                    is ReorderableListItem.SongItem -> {
                        currentMoment?.let { moment ->
                            newPiesniSugerowane.add(item.suggestedSong.copy(moment = moment))
                        }
                    }
                }
            }

            currentData.copy(piesniSugerowane = newPiesniSugerowane)
        }
    }

    fun addOrUpdateSong(
        title: String,
        siedl: String,
        sak: String,
        dn: String,
        opis: String,
        moment: String,
        originalSong: SuggestedSong?
    ) {
        viewModelScope.launch {
            val trimmedTitle = title.trim()
            val trimmedSiedl = siedl.trim()
            val trimmedSak = sak.trim()
            val trimmedDn = dn.trim()

            val songMatches = repository.getSongList().filter { it.tytul.equals(trimmedTitle, ignoreCase = true) }

            val perfectMatch = songMatches.find {
                it.numerSiedl.equals(trimmedSiedl, ignoreCase = true) &&
                        it.numerSAK.equals(trimmedSak, ignoreCase = true) &&
                        it.numerDN.equals(trimmedDn, ignoreCase = true)
            }

            if (perfectMatch == null) {
                val error = when {
                    songMatches.isEmpty() -> "Nie znaleziono pieśni o podanym tytule."
                    else -> "Numery nie pasują do pieśni o tym tytule. Wybierz pieśń z sugestii, aby automatycznie uzupełnić pola."
                }
                _uiState.update {
                    val currentDialog = it.activeDialog as? DialogState.AddEditSong
                    it.copy(activeDialog = currentDialog?.copy(error = error) ?: DialogState.None)
                }
                return@launch
            }

            val newSuggestedSong = SuggestedSong(
                numer = trimmedSiedl,
                piesn = trimmedTitle,
                opis = opis.trim(),
                moment = moment
            )

            updateEditableData { currentData ->
                val songs = currentData.piesniSugerowane.orEmpty().filterNotNull().toMutableList()
                if (originalSong != null) {
                    val index = songs.indexOf(originalSong)
                    if (index != -1) {
                        songs[index] = newSuggestedSong
                    } else {
                        songs.add(newSuggestedSong)
                    }
                } else {
                    songs.add(newSuggestedSong)
                }
                currentData.copy(piesniSugerowane = songs)
            }
            dismissDialog()
        }
    }

    fun searchSongsByTitle(query: String) {
        _songTitleSearchQuery.value = query
        if (query.length < 2) songTitleSearchResults.value = emptyList()
    }

    fun searchSongsBySiedl(query: String) {
        _siedlSearchQuery.value = query
        if (query.isBlank()) siedlSearchResults.value = emptyList()
    }

    fun searchSongsBySak(query: String) {
        _sakSearchQuery.value = query
        if (query.isBlank()) sakSearchResults.value = emptyList()
    }

    fun searchSongsByDn(query: String) {
        _dnSearchQuery.value = query
        if (query.isBlank()) dnSearchResults.value = emptyList()
    }

    fun formatSongSuggestion(song: Song): String {
        val numberInfo = buildList {
            if (song.numerSiedl.isNotBlank()) add("Siedl: ${song.numerSiedl}")
            if (song.numerSAK.isNotBlank()) add("ŚAK: ${song.numerSAK}")
            if (song.numerDN.isNotBlank()) add("DN: ${song.numerDN}")
        }.joinToString(", ")

        return if (numberInfo.isNotEmpty()) {
            "${song.tytul} ($numberInfo)"
        } else {
            song.tytul
        }
    }

    fun clearAllSearchResults() {
        songTitleSearchResults.value = emptyList()
        siedlSearchResults.value = emptyList()
        sakSearchResults.value = emptyList()
        dnSearchResults.value = emptyList()
        _songTitleSearchQuery.value = ""
        _siedlSearchQuery.value = ""
        _sakSearchQuery.value = ""
        _dnSearchQuery.value = ""
    }
}

class DayDetailsViewModelFactory(
    private val context: Context,
    private val dayId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DayDetailsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DayDetailsViewModel(dayId, FileSystemRepository(context.applicationContext)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
```
---
**Nazwa pliku:** `C:\Users\blzej\Desktop\Aplikacja dla studenta\Aplikacja-dla-Konrada\app\src\main\java\com\qjproject\liturgicalcalendar\ui\screens\daydetails\DayDetailsScreen\DayDetailsViewMode.kt`
**Treść:**
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
import androidx.compose.ui.graphics.Color
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
        InsertsSectionView(viewModel = viewModel)
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

@Composable
private fun InsertsSectionView(viewModel: DayDetailsViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val inserts = uiState.dayData?.wstawki
    val hasInserts = inserts != null && (inserts.`1`?.isNotBlank() == true || inserts.`2`?.isNotBlank() == true || inserts.`3`?.isNotBlank() == true)

    if (!hasInserts) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Wstawki",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.Gray
            )
        }
    } else {
        HierarchicalCollapsibleSection(
            title = "Wstawki",
            isExpanded = uiState.isInsertsSectionExpanded,
            onToggle = { viewModel.toggleInsertsSection() }
        ) {
            inserts?.let {
                val insertMap = linkedMapOf(
                    "1" to it.`1`,
                    "2" to it.`2`,
                    "3" to it.`3`
                ).filterValues { value -> !value.isNullOrBlank() }

                insertMap.forEach { (key, text) ->
                    InsertItemView(
                        insertKey = key,
                        insertText = text!!,
                        isExpanded = uiState.expandedInserts.contains(key),
                        onToggle = { viewModel.toggleInsert(key) }
                    )
                }
            }
        }
    }
}

@Composable
fun InsertItemView(
    insertKey: String,
    insertText: String,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle),
            colors = CardDefaults.cardColors(containerColor = VeryDarkNavy)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Wstawka $insertKey",
                    style = MaterialTheme.typography.titleMedium,
                    color = SaturatedNavy
                )
            }
        }
        AnimatedVisibility(visible = isExpanded) {
            Text(
                text = insertText.replace("*", "\n"),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
```