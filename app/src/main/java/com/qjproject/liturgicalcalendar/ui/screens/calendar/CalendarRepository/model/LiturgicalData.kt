package com.qjproject.liturgicalcalendar.ui.screens.calendar.CalendarRepository.model

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.format.DateTimeFormatter

enum class LiturgicalRank(val value: Int) {
    // --- POCZĄTEK ZMIANY ---
    // Zmieniono kolejność, aby generowany dzień powszedni (WEEKDAY)
    // pojawiał się przed wspomnieniami na liście wydarzeń.
    SOLEMNITY(1),
    FEAST(2),
    WEEKDAY(3),
    MEMORIAL_OBLIGATORY(4),
    MEMORIAL_OPTIONAL(5)
    // --- KONIEC ZMIANY ---
}

@Serializable
data class LiturgicalEventDetails(
    val name: String,
    val data: String,
    val rok_litera: String,
    val rok_cyfra: String,
    val typ: String,
    val kolor: String
) {
    companion object {
        val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
    }
}

data class LiturgicalYear(private val events: Map<String, LiturgicalEventDetails>) {
    private val eventsByDate by lazy {
        events.values.groupBy {
            LocalDate.parse(it.data, LiturgicalEventDetails.DATE_FORMATTER)
        }
    }
    fun eventsForDate(date: LocalDate): List<LiturgicalEventDetails> {
        return eventsByDate[date] ?: emptyList()
    }
}

data class CalendarDay(
    val dayOfMonth: Int,
    val isToday: Boolean,
    val events: List<LiturgicalEventDetails>,
    val dominantEventColorName: String?
) {
    val hasEvents: Boolean get() = events.isNotEmpty()
}