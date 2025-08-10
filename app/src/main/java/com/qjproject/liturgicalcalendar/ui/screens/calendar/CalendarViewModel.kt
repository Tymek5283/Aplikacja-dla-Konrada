package com.qjproject.liturgicalcalendar.ui.screens.calendar

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.qjproject.liturgicalcalendar.data.FileSystemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

data class CalendarUiState(
    val selectedMonth: YearMonth = YearMonth.now(),
    val daysInMonth: List<CalendarDay?> = emptyList(),
    val availableYears: List<Int> = emptyList(),
    val isDataMissing: Boolean = true,
    val isLoading: Boolean = false,
    val liturgicalYearInfo: String = "",
    val liturgicalYearTransitionInfo: String? = null,
    val downloadError: String? = null
)

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

            if (!isCurrentYearAvailable) {
                _uiState.update { it.copy(isDataMissing = true, isLoading = true) }
                downloadCalendarFiles() // Usunięto 'silent', ponieważ teraz UI zawsze reaguje
            } else {
                val availableYears = calendarRepo.getAvailableYears()
                _uiState.update { it.copy(isDataMissing = false, availableYears = availableYears, isLoading = false) }
                loadDataForMonth(YearMonth.now())
                // Sprawdź w tle przyszły rok bez blokowania UI
                viewModelScope.launch { calendarRepo.downloadAndSaveYearIfNeeded(currentYear + 1) }
            }
        }
    }

    fun downloadCalendarFiles() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, downloadError = null) }

            val currentYear = YearMonth.now().year
            val nextYear = currentYear + 1

            var firstError: Throwable? = null

            calendarRepo.downloadAndSaveYearIfNeeded(currentYear).onFailure { error ->
                if (firstError == null) firstError = error
            }
            calendarRepo.downloadAndSaveYearIfNeeded(nextYear).onFailure { error ->
                if (firstError == null) firstError = error
            }

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
        val liturgicalYearData = calendarRepo.getLiturgicalYear(yearMonth.year)
        if (liturgicalYearData == null) {
            _uiState.update { it.copy(daysInMonth = emptyList()) }
            return
        }

        val today = LocalDate.now()
        val firstDayOfMonth = yearMonth.atDay(1).dayOfWeek
        val firstDayOfMonthOffset = (firstDayOfMonth.value - DayOfWeek.MONDAY.value + 7) % 7
        val daysInMonthCount = yearMonth.lengthOfMonth()

        val calendarDays = mutableListOf<CalendarDay?>()
        repeat(firstDayOfMonthOffset) { calendarDays.add(null) }

        for (day in 1..daysInMonthCount) {
            val date = yearMonth.atDay(day)
            val eventsForDay = liturgicalYearData.eventsForDate(date)
            val dominantEvent = calendarRepo.getDominantEvent(eventsForDay)

            calendarDays.add(
                CalendarDay(
                    dayOfMonth = day,
                    month = yearMonth,
                    isToday = date == today,
                    events = eventsForDay,
                    dominantEventColorName = dominantEvent?.kolor
                )
            )
        }

        val yearInfo = calendarRepo.getLiturgicalYearInfoForMonth(yearMonth, liturgicalYearData)

        _uiState.update {
            it.copy(
                selectedMonth = yearMonth,
                daysInMonth = calendarDays,
                liturgicalYearInfo = yearInfo.mainInfo,
                liturgicalYearTransitionInfo = yearInfo.transitionInfo
            )
        }
    }

    fun handleEventSelection(event: LiturgicalEventDetails, onResult: (NavigationAction) -> Unit) {
        viewModelScope.launch {
            val foundItem = findItemRecursively("data", event.name)
                ?: findItemRecursively("Datowane", event.name)

            if (foundItem != null) {
                val (foundPath, isFolder) = foundItem
                val action = if (isFolder) NavigationAction.ShowFolderSelection(foundPath) else NavigationAction.NavigateToDay(foundPath)
                onResult(action)
            } else {
                val result = fileSystemRepo.createDayFile("Datowane/nieznane", event.name, null)
                result.onSuccess { newFileName ->
                    val newPath = "Datowane/nieznane/$newFileName"
                    onResult(NavigationAction.NavigateToDay(newPath))
                }
            }
        }
    }

    private fun findItemRecursively(basePath: String, name: String): Pair<String, Boolean>? {
        val items = fileSystemRepo.getItems(basePath)
        for (item in items) {
            if (item.name.equals(name, ignoreCase = true)) {
                return item.path to item.isDirectory
            }
            if (item.isDirectory) {
                findItemRecursively(item.path, name)?.let { return it }
            }
        }
        return null
    }

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

class CalendarViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CalendarViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CalendarViewModel(
                CalendarRepository(context.applicationContext),
                FileSystemRepository(context.applicationContext)
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}