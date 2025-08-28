// Ścieżka: app/src/main/java/com/qjproject/liturgicalcalendar/ui/screens/calendar/CalendarViewModel/index.kt
// Opis: ViewModel dla ekranu kalendarza. Zarządza stanem UI, logiką pobierania i przetwarzania danych liturgicznych na dany miesiąc i rok oraz obsługuje nawigację do szczegółów dnia.
package com.qjproject.liturgicalcalendar.ui.screens.calendar.CalendarViewModel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.qjproject.liturgicalcalendar.data.repository.FileSystemRepository.FileSystemRepository
import com.qjproject.liturgicalcalendar.ui.screens.calendar.CalendarRepository.CalendarRepository
import com.qjproject.liturgicalcalendar.ui.screens.calendar.CalendarRepository.model.CalendarDay
import com.qjproject.liturgicalcalendar.ui.screens.calendar.CalendarRepository.model.LiturgicalYear
import com.qjproject.liturgicalcalendar.ui.screens.calendar.CalendarRepository.model.LiturgicalEventDetails
import com.qjproject.liturgicalcalendar.ui.screens.calendar.CalendarRepository.model.translationMap
import com.qjproject.liturgicalcalendar.ui.screens.calendar.CalendarViewModel.model.CalendarUiState
import com.qjproject.liturgicalcalendar.ui.screens.calendar.CalendarViewModel.model.NavigationAction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

class CalendarViewModel(
    val calendarRepo: CalendarRepository,
    private val fileSystemRepo: FileSystemRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState = _uiState.asStateFlow()

    init {
        checkAndLoadInitialData()
    }

    private fun checkAndLoadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val currentYear = YearMonth.now().year
            val requiredYears = listOf(currentYear - 1, currentYear, currentYear + 1)
            
            val areAllRequiredYearsAvailable = calendarRepo.areRequiredYearsAvailable(requiredYears)
            
            if (areAllRequiredYearsAvailable) {
                // Wszystkie wymagane dane są dostępne lokalnie
                val availableYears = calendarRepo.getAvailableYears()
                _uiState.update { it.copy(isDataMissing = false, availableYears = availableYears, isLoading = false) }
                loadDataForMonth(YearMonth.now())
            } else {
                // Pobierz tylko brakujące dane
                _uiState.update { it.copy(isDataMissing = true, isLoading = true) }
                smartRefreshData(requiredYears)
            }
        }
    }

    private fun smartRefreshData(requiredYears: List<Int>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, downloadError = null) }

            calendarRepo.downloadMissingYearsOnly(requiredYears).fold(
                onSuccess = { downloadedYears ->
                    val availableYears = calendarRepo.getAvailableYears()
                    val currentYear = YearMonth.now().year
                    val isCurrentYearAvailable = availableYears.contains(currentYear)
                    
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isDataMissing = !isCurrentYearAvailable,
                            availableYears = availableYears,
                            downloadError = null
                        )
                    }

                    if (isCurrentYearAvailable) {
                        loadDataForMonth(YearMonth.now())
                    }
                },
                onFailure = { error ->
                    val availableYears = calendarRepo.getAvailableYears()
                    val currentYear = YearMonth.now().year
                    val isCurrentYearAvailable = availableYears.contains(currentYear)
                    
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isDataMissing = !isCurrentYearAvailable,
                            availableYears = availableYears,
                            downloadError = "Błąd: ${error.message}"
                        )
                    }

                    if (isCurrentYearAvailable) {
                        loadDataForMonth(YearMonth.now())
                    }
                }
            )
        }
    }

    fun forceRefreshData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, downloadError = null) }
            calendarRepo.deleteAllCalendarFiles()

            val currentYear = YearMonth.now().year
            var firstError: Throwable? = null

            calendarRepo.downloadAndSaveYearIfNeeded(currentYear - 1).onFailure { error -> if (firstError == null) firstError = error }
            calendarRepo.downloadAndSaveYearIfNeeded(currentYear).onFailure { error -> if (firstError == null) firstError = error }
            calendarRepo.downloadAndSaveYearIfNeeded(currentYear + 1).onFailure { error -> if (firstError == null) firstError = error }

            val availableYears = calendarRepo.getAvailableYears()
            val isCurrentYearNowAvailable = availableYears.contains(currentYear)

            _uiState.update {
                it.copy(
                    isLoading = false,
                    isDataMissing = !isCurrentYearNowAvailable,
                    availableYears = availableYears,
                    downloadError = if (isCurrentYearNowAvailable) null else "Błąd: ${firstError?.message ?: "Nieznany błąd"}"
                )
            }

            if (isCurrentYearNowAvailable) {
                loadDataForMonth(_uiState.value.selectedMonth)
            }
        }
    }

    private suspend fun loadDataForMonth(yearMonth: YearMonth) {
        val liturgicalYearData = calendarRepo.getAugmentedLiturgicalYear(yearMonth.year)

        val today = LocalDate.now()
        val firstDayOfMonth = yearMonth.atDay(1).dayOfWeek
        val firstDayOfMonthOffset = (firstDayOfMonth.value - DayOfWeek.MONDAY.value + 7) % 7
        val daysInMonthCount = yearMonth.lengthOfMonth()

        val calendarDays = mutableListOf<CalendarDay?>()
        repeat(firstDayOfMonthOffset) { calendarDays.add(null) }

        for (day in 1..daysInMonthCount) {
            val date = yearMonth.atDay(day)
            val eventsForDay = liturgicalYearData?.eventsForDate(date) ?: emptyList()
            val dominantEvent = calendarRepo.getDominantEvent(eventsForDay)

            calendarDays.add(
                CalendarDay(
                    dayOfMonth = day,
                    isToday = date == today,
                    events = eventsForDay,
                    dominantEventColorName = dominantEvent?.kolor
                )
            )
        }

        val yearInfo = calendarRepo.getLiturgicalYearInfo(liturgicalYearData, yearMonth.atDay(1))

        _uiState.update {
            it.copy(
                selectedMonth = yearMonth,
                daysInMonth = calendarDays,
                liturgicalYearInfo = yearInfo
            )
        }
    }

    fun handleEventSelection(event: LiturgicalEventDetails, onResult: (NavigationAction) -> Unit) {
        viewModelScope.launch {
            val foundPaths = findFilePathsForEvent(event.name)

            when {
                foundPaths.isEmpty() -> {
                    // POPRAWKA: Zawsze otwórz okno szczegółów, nawet gdy nie ma pliku
                    android.util.Log.w("CalendarViewModel", "Nie znaleziono pliku dla wydarzenia: '${event.name}'. Otwieranie pustego okna szczegółów.")
                    onResult(NavigationAction.NavigateToDay("empty/${event.name}"))
                }
                foundPaths.size == 1 -> {
                    onResult(NavigationAction.NavigateToDay(foundPaths.first()))
                }
                else -> {
                    onResult(NavigationAction.ShowDateEvents(event.name, foundPaths))
                }
            }
        }
    }

    // --- POCZĄTEK ZMIANY ---
    // Poprawiona logika wyszukiwania plików w folderze assets z użyciem translationMap
    private fun findFilePathsForEvent(name: String): List<String> {
        val searchName = name.replace(":", "").trim()
        val results = mutableListOf<String>()
        val assetManager = fileSystemRepo.context.assets
        
        // KLUCZOWA POPRAWKA: Użyj translationMap do znalezienia właściwej nazwy pliku
        val translatedName: String = translationMap[searchName] ?: searchName
        android.util.Log.d("CalendarViewModel", "Szukanie wydarzenia: '$searchName' -> przetłumaczone na: '$translatedName'")
        
        try {
            // Szukaj zarówno oryginalnej nazwy jak i przetłumaczonej
            val namesToSearch = listOf(searchName, translatedName).distinct()
            
            for (nameToSearch in namesToSearch) {
                // Przeszukaj folder "data" w assets
                searchInAssetsDirectory(assetManager, "data", nameToSearch, results)
                
                // Przeszukaj folder "Datowane" w assets
                searchInAssetsDirectory(assetManager, "Datowane", nameToSearch, results)
            }
            
        } catch (e: Exception) {
            android.util.Log.e("CalendarViewModel", "Błąd podczas przeszukiwania assets: ${e.message}")
        }
        
        android.util.Log.d("CalendarViewModel", "Znalezione pliki dla '$searchName': ${results.size} - $results")
        return results.distinct().sorted()
    }
    
    private fun searchInAssetsDirectory(assetManager: android.content.res.AssetManager, basePath: String, searchName: String, results: MutableList<String>) {
        try {
            val items = assetManager.list(basePath) ?: return
            
            for (item in items) {
                val fullPath = "$basePath/$item"
                
                try {
                    // Sprawdź czy to folder (próbując wylistować jego zawartość)
                    val subItems = assetManager.list(fullPath)
                    if (subItems != null && subItems.isNotEmpty()) {
                        // To jest folder - rekursywnie przeszukaj
                        searchInAssetsDirectory(assetManager, fullPath, searchName, results)
                        
                        // Sprawdź czy nazwa folderu pasuje do szukanej nazwy
                        if (item.equals(searchName, ignoreCase = true)) {
                            // Znajdź wszystkie pliki JSON w tym folderze
                            findJsonFilesInDirectory(assetManager, fullPath, results)
                        }
                    } else {
                        // To jest plik
                        val fileName = if (item.endsWith(".json")) {
                            item.substringBeforeLast(".json")
                        } else {
                            item
                        }
                        
                        if (fileName.contains(searchName, ignoreCase = true) && item.endsWith(".json")) {
                            results.add(fullPath)
                        }
                    }
                } catch (e: Exception) {
                    // Jeśli nie można wylistować, to prawdopodobnie jest to plik
                    val fileName = if (item.endsWith(".json")) {
                        item.substringBeforeLast(".json")
                    } else {
                        item
                    }
                    
                    if (fileName.contains(searchName, ignoreCase = true) && item.endsWith(".json")) {
                        results.add(fullPath)
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("CalendarViewModel", "Nie można przeszukać folderu $basePath: ${e.message}")
        }
    }
    
    private fun findJsonFilesInDirectory(assetManager: android.content.res.AssetManager, dirPath: String, results: MutableList<String>) {
        try {
            val items = assetManager.list(dirPath) ?: return
            
            for (item in items) {
                val fullPath = "$dirPath/$item"
                
                try {
                    val subItems = assetManager.list(fullPath)
                    if (subItems != null && subItems.isNotEmpty()) {
                        // To jest podfolder - rekursywnie przeszukaj
                        findJsonFilesInDirectory(assetManager, fullPath, results)
                    } else if (item.endsWith(".json")) {
                        // To jest plik JSON
                        results.add(fullPath)
                    }
                } catch (e: Exception) {
                    // Prawdopodobnie plik
                    if (item.endsWith(".json")) {
                        results.add(fullPath)
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("CalendarViewModel", "Błąd podczas przeszukiwania folderu $dirPath: ${e.message}")
        }
    }
    // --- KONIEC ZMIANY ---


    fun changeMonth(amount: Long) {
        val newMonth = _uiState.value.selectedMonth.plusMonths(amount)
        if (isYearAvailable(newMonth.year)) {
            viewModelScope.launch { loadDataForMonth(newMonth) }
        }
    }

    fun setYear(year: Int) {
        if (isYearAvailable(year)) {
            val newMonth = _uiState.value.selectedMonth.withYear(year)
            viewModelScope.launch { loadDataForMonth(newMonth) }
        }
    }

    fun setMonth(monthIndex: Int) {
        val newMonth = _uiState.value.selectedMonth.withMonth(monthIndex + 1)
        if (isYearAvailable(newMonth.year)) {
            viewModelScope.launch { loadDataForMonth(newMonth) }
        }
    }

    fun resetToCurrentMonth() {
        viewModelScope.launch { loadDataForMonth(YearMonth.now()) }
    }

    private fun isYearAvailable(year: Int): Boolean {
        return _uiState.value.availableYears.contains(year)
    }
}