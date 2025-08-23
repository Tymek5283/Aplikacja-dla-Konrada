// Ścieżka: C:\Users\blzej\Desktop\Aplikacja dla studenta\Aplikacja-dla-Konrada\app\src\main\java\com\qjproject\liturgicalcalendar\ui\screens\calendar\CalendarRepository\models.kt
// Opis: Zawiera definicje wszystkich modeli danych oraz klas zapieczętowanych, które strukturyzują informacje o wydarzeniach liturgicznych, dniach w kalendarzu i akcjach nawigacyjnych.
package com.qjproject.liturgicalcalendar.ui.screens.calendar.CalendarRepository.model

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

sealed class NavigationAction {
    data class NavigateToDay(val path: String) : NavigationAction()
    data class ShowDateEvents(val title: String, val paths: List<String>) : NavigationAction()
}

@Serializable
data class LiturgicalEventDetails(
    val name: String,
    val data: String,
    val rok_litera: String,
    val rok_cyfra: String,
    val typ: String,
    val kolor: String
)

data class LiturgicalYearInfo(
    val mainInfo: String,
    val transitionInfo: String?
)

data class CalendarDay(
    val dayOfMonth: Int,
    val month: YearMonth,
    val isToday: Boolean,
    val events: List<LiturgicalEventDetails>,
    val dominantEventColorName: String?
) {
    val hasEvents: Boolean get() = events.isNotEmpty()
}

class LiturgicalYear(private val events: Map<String, LiturgicalEventDetails>) {
    private val eventsByDate by lazy {
        events.values.groupBy {
            LocalDate.parse(it.data, DateTimeFormatter.ofPattern("dd-MM-yyyy"))
        }
    }

    fun eventsForDate(date: LocalDate): List<LiturgicalEventDetails> {
        return eventsByDate[date] ?: emptyList()
    }
}