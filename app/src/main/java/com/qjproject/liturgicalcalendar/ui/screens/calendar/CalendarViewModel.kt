package com.qjproject.liturgicalcalendar.ui.screens.calendar

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.qjproject.liturgicalcalendar.data.FileSystemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

data class CalendarDay(
    val dayOfMonth: Int,
    val isToday: Boolean,
    val hasData: Boolean,
    val files: List<String>
)

data class CalendarUiState(
    val selectedMonth: YearMonth = YearMonth.now(),
    val daysInMonth: List<CalendarDay?> = emptyList(),
    val yearList: List<Int> = emptyList()
)

class CalendarViewModel(private val repository: FileSystemRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState = _uiState.asStateFlow()

    init {
        val currentYear = YearMonth.now().year
        // --- POPRAWKA: Poprawna składnia konwersji zakresu na listę ---
        _uiState.update { it.copy(yearList = (currentYear - 2..currentYear + 2).toList()) }
        loadDataForMonth(YearMonth.now())
    }

    fun changeMonth(amount: Long) {
        val newMonth = _uiState.value.selectedMonth.plusMonths(amount)
        loadDataForMonth(newMonth)
    }

    fun setYear(year: Int) {
        val newMonth = _uiState.value.selectedMonth.withYear(year)
        loadDataForMonth(newMonth)
    }

    fun setMonth(monthIndex: Int) {
        val newMonth = _uiState.value.selectedMonth.withMonth(monthIndex + 1)
        loadDataForMonth(newMonth)
    }

    fun resetToCurrentMonth() {
        loadDataForMonth(YearMonth.now())
    }

    private fun loadDataForMonth(yearMonth: YearMonth) {
        val today = LocalDate.now()

        val firstDayOfMonth = yearMonth.atDay(1).dayOfWeek
        val firstDayOfMonthOffset = (firstDayOfMonth.value - DayOfWeek.MONDAY.value + 7) % 7
        val daysInMonthCount = yearMonth.lengthOfMonth()

        val calendarDays = mutableListOf<CalendarDay?>()
        repeat(firstDayOfMonthOffset) { calendarDays.add(null) }

        for (day in 1..daysInMonthCount) {
            calendarDays.add(
                CalendarDay(
                    dayOfMonth = day,
                    isToday = yearMonth.month == today.month && yearMonth.year == today.year && day == today.dayOfMonth,
                    hasData = true,
                    files = emptyList()
                )
            )
        }

        _uiState.update {
            it.copy(
                selectedMonth = yearMonth,
                daysInMonth = calendarDays,
            )
        }
    }
}

class CalendarViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CalendarViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CalendarViewModel(FileSystemRepository(context.applicationContext)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}