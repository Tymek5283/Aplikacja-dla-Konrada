// Ścieżka: app/src/main/java/com/qjproject/liturgicalcalendar/logic/liturgical_year_calculator/BoundaryDates.kt
// Opis: Ten plik zawiera logikę do znajdowania kluczowych dat granicznych w roku liturgicznym.

package com.qjproject.liturgicalcalendar.logic.liturgical_year_calculator

import android.util.Log
import com.qjproject.liturgicalcalendar.ui.screens.calendar.CalendarRepository.model.LiturgicalEventDetails
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")

internal data class BoundaryDates(
    val firstSundayOfAdvent: LocalDate,
    val christmas: LocalDate,
    val baptismOfLord: LocalDate,
    val ashWednesday: LocalDate,
    val holyThursday: LocalDate,
    val easterSunday: LocalDate,
    val pentecostSunday: LocalDate
)

internal fun findBoundaryDates(year: Int, allEvents: List<LiturgicalEventDetails>): BoundaryDates? {
    val firstSundayOfAdventPrevious = calculateFirstSundayOfAdvent(year - 1)
    val christmas = LocalDate.of(year - 1, 12, 25)
    Log.i("LiturgicalYearCalculator", "Using hardcoded Christmas date for previous year: $christmas")

    val baptismOfLord = findDateForEvent("Niedziela Chrztu Pańskiego", year, allEvents) ?: return null
    val ashWednesday = findDateForEvent("Środa Popielcowa", year, allEvents) ?: return null
    val holyThursday = findDateForEvent("Wielki Czwartek", year, allEvents) ?: return null
    val easterSunday = findDateForEvent("Niedziela Zmartwychwstania Pańskiego", year, allEvents) ?: return null
    val pentecostSunday = findDateForEvent("Zesłania Ducha Świętego", year, allEvents) ?: return null

    return BoundaryDates(firstSundayOfAdventPrevious, christmas, baptismOfLord, ashWednesday, holyThursday, easterSunday, pentecostSunday)
}

private fun findDateForEvent(eventName: String, year: Int, allEvents: List<LiturgicalEventDetails>): LocalDate? {
    Log.d("LiturgicalYearCalculator", "Searching for event: '$eventName' in year $year.")
    val event = allEvents.firstOrNull { event ->
        event.name.contains(eventName, ignoreCase = true) && LocalDate.parse(event.data, DATE_FORMATTER).year == year
    }
    if (event == null) {
        Log.e("LiturgicalYearCalculator", "Could not find event '$eventName' for year $year")
        val sampleEvents = allEvents.filter { it.name.contains("niedziela", ignoreCase = true) || it.name.contains("zesłania", ignoreCase = true) }.take(5).joinToString { it.name }
        Log.w("LiturgicalYearCalculator", "Sample of available relevant events: $sampleEvents")
    } else {
        Log.i("LiturgicalYearCalculator", "Found event '$eventName' for year $year on date: ${event.data}")
    }
    return event?.let { LocalDate.parse(it.data, DATE_FORMATTER) }
}

private fun calculateFirstSundayOfAdvent(year: Int): LocalDate {
    Log.d("LiturgicalYearCalculator", "Calculating First Sunday of Advent for liturgical year starting in $year.")
    val christmas = LocalDate.of(year, 12, 25)
    val sundayBeforeChristmas = christmas.with(TemporalAdjusters.previous(DayOfWeek.SUNDAY))
    val firstSunday = sundayBeforeChristmas.minusWeeks(3)
    Log.i("LiturgicalYearCalculator", "Calculated First Sunday of Advent for year $year is: $firstSunday")
    return firstSunday
}