// Ścieżka: app/src/main/java/com/qjproject/liturgicalcalendar/ui/screens/calendar/CalendarViewModel/model/models.kt
// Opis: Definiuje modele danych i stany interfejsu użytkownika (UI State) używane wyłącznie przez CalendarViewModel.

package com.qjproject.liturgicalcalendar.ui.screens.calendar.CalendarViewModel.model

import com.qjproject.liturgicalcalendar.ui.screens.calendar.CalendarRepository.model.CalendarDay
import java.time.YearMonth

sealed class NavigationAction {
    data class NavigateToDay(val path: String) : NavigationAction()
    data class ShowDateEvents(val title: String, val paths: List<String>) : NavigationAction()
}

data class CalendarUiState(
    val selectedMonth: YearMonth = YearMonth.now(),
    val daysInMonth: List<CalendarDay?> = emptyList(),
    val availableYears: List<Int> = listOf(YearMonth.now().year),
    val liturgicalYearInfo: String = "Ładowanie...",
    val isLoading: Boolean = true,
    val isDataMissing: Boolean = false,
    val downloadError: String? = null
)