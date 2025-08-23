// Ścieżka: C:\Users\blzej\Desktop\Aplikacja dla studenta\Aplikacja-dla-Konrada\app\src\main\java\com\qjproject\liturgicalcalendar\ui\screens\calendar\CalendarRepository\LiturgicalLogic.kt
// Opis: Zawiera czystą logikę biznesową związaną z kalendarzem liturgicznym, taką jak określanie hierarchii świąt, wyznaczanie cykli liturgicznych i formatowanie informacji o roku.
package com.qjproject.liturgicalcalendar.ui.screens.calendar.CalendarRepository.logic

import com.qjproject.liturgicalcalendar.ui.screens.calendar.CalendarRepository.model.LiturgicalEventDetails
import com.qjproject.liturgicalcalendar.ui.screens.calendar.CalendarRepository.model.LiturgicalYear
import com.qjproject.liturgicalcalendar.ui.screens.calendar.CalendarRepository.model.LiturgicalYearInfo
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

internal class LiturgicalLogic {
    private val eventTypeHierarchy = mapOf(
        "Uroczystość" to 1,
        "Święto" to 2,
        "Wspomnienie obowiązkowe" to 3,
        "" to 4,
        "Wspomnienie dowolne" to 5
    )

    val eventComparator: Comparator<LiturgicalEventDetails> = compareBy<LiturgicalEventDetails>(
        { eventTypeHierarchy[it.typ] ?: 99 },
        { !it.name.any { char -> char.isDigit() } },
        { it.name }
    )

    fun getDominantEvent(events: List<LiturgicalEventDetails>): LiturgicalEventDetails? {
        if (events.isEmpty()) return null
        return events.minWithOrNull(eventComparator)
    }

    fun getLiturgicalYearInfoForMonth(yearMonth: YearMonth, yearData: LiturgicalYear): LiturgicalYearInfo {
        val lastDayOfMonth = yearMonth.atEndOfMonth()
        val eventsOnLastDay = yearData.eventsForDate(lastDayOfMonth)
        val dominantEventOnLastDay = getDominantEvent(eventsOnLastDay)

        if (dominantEventOnLastDay == null) {
            return LiturgicalYearInfo("Brak danych o roku liturgicznym", null)
        }

        val finalYearId = formatYearId(dominantEventOnLastDay.rok_litera, dominantEventOnLastDay.rok_cyfra)
        val mainInfo = finalYearId?.let { "Aktualny rok: $it" } ?: "Brak danych o roku liturgicznym"
        var transitionInfo: String? = null

        if (finalYearId != null) {
            for (day in lastDayOfMonth.dayOfMonth - 1 downTo 1) {
                val date = yearMonth.atDay(day)
                val events = yearData.eventsForDate(date)
                val dominantEvent = getDominantEvent(events)
                val currentYearId = formatYearId(dominantEvent?.rok_litera, dominantEvent?.rok_cyfra)

                if (currentYearId != finalYearId) {
                    currentYearId?.let {
                        val monthName = date.month.getDisplayName(TextStyle.FULL, Locale("pl"))
                        transitionInfo = "Rok obowiązujący do ${day} $monthName: $it"
                    }
                    break
                }
            }
        }
        return LiturgicalYearInfo(mainInfo, transitionInfo)
    }

    private fun formatYearId(rok_litera: String?, rok_cyfra: String?): String? {
        if (rok_litera.isNullOrBlank() || rok_litera.equals("null", ignoreCase = true) ||
            rok_cyfra.isNullOrBlank() || rok_cyfra.equals("null", ignoreCase = true)
        ) {
            return null
        }
        return "$rok_litera, $rok_cyfra"
    }
}

internal data class LiturgicalCycles(val sundayCycle: String, val weekdayCycle: String)

internal fun calculateLiturgicalCycles(eventDate: LocalDate): LiturgicalCycles {
    val calendarYear = eventDate.year
    val weekdayCycle = if (calendarYear % 2 != 0) "1" else "2"
    val dec3rd = LocalDate.of(calendarYear, 12, 3)
    val dayIndex = dec3rd.dayOfWeek.value % 7
    val firstSundayOfAdvent = dec3rd.minusDays(dayIndex.toLong())
    val referenceYear = if (eventDate.isBefore(firstSundayOfAdvent)) calendarYear - 1 else calendarYear
    val sundayCycle = when (referenceYear % 3) {
        0 -> "A"; 1 -> "B"; 2 -> "C"; else -> "Error"
    }
    return LiturgicalCycles(sundayCycle, weekdayCycle)
}