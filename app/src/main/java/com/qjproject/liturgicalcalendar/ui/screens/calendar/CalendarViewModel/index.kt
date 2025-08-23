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
import com.qjproject.liturgicalcalendar.ui.screens.calendar.CalendarRepository.model.LiturgicalEventDetails
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
            val isCurrentYearAvailable = calendarRepo.isYearAvailable(currentYear)
            val isPreviousYearAvailable = calendarRepo.isYearAvailable(currentYear - 1)

            if (!isCurrentYearAvailable || !isPreviousYearAvailable) {
                _uiState.update { it.copy(isDataMissing = true, isLoading = true) }
                forceRefreshData()
            } else {
                val availableYears = calendarRepo.getAvailableYears()
                _uiState.update { it.copy(isDataMissing = false, availableYears = availableYears, isLoading = false) }
                loadDataForMonth(YearMonth.now())
                viewModelScope.launch { calendarRepo.downloadAndSaveYearIfNeeded(currentYear + 1) }
            }
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
                    val result = fileSystemRepo.createDayFile("Datowane/nieznane", event.name, null)
                    result.onSuccess { newFileName ->
                        val newPath = "Datowane/nieznane/$newFileName"
                        onResult(NavigationAction.NavigateToDay(newPath))
                    }
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
    // Zastąpiono poprzednią, błędną logikę wyszukiwania nową, w pełni rekursywną funkcją.
    private fun findFilePathsForEvent(name: String): List<String> {
        val searchName = name.replace(":", "").trim()
        val results = mutableListOf<String>()
        val directoriesToSearch = listOf(
            File(fileSystemRepo.context.filesDir, "data"),
            File(fileSystemRepo.context.filesDir, "Datowane")
        )

        directoriesToSearch.forEach { startDir ->
            if (startDir.exists() && startDir.isDirectory) {
                startDir.walkTopDown().forEach { file ->
                    val itemName = if (file.isDirectory) file.name else file.nameWithoutExtension

                    if (itemName.equals(searchName, ignoreCase = true)) {
                        if (file.isDirectory) {
                            file.walkTopDown()
                                .filter { it.isFile && it.extension.equals("json", ignoreCase = true) }
                                .forEach { jsonFile ->
                                    val relativePath = jsonFile.absolutePath.removePrefix(fileSystemRepo.context.filesDir.absolutePath + "/")
                                    results.add(relativePath)
                                }
                        } else if (file.isFile && file.extension.equals("json", ignoreCase = true)) {
                            val relativePath = file.absolutePath.removePrefix(fileSystemRepo.context.filesDir.absolutePath + "/")
                            results.add(relativePath)
                        }
                    }
                }
            }
        }
        return results.distinct().sorted()
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