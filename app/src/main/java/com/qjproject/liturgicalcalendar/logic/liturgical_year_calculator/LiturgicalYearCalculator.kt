// Ścieżka: app/src/main/java/com/qjproject/liturgicalcalendar/logic/liturgical_year_calculator/LiturgicalYearCalculator.kt
// Opis: Główna klasa, która integruje wszystkie elementy logiki roku liturgicznego do stworzenia mapy kontekstu dla każdego dnia w roku.
package com.qjproject.liturgicalcalendar.logic.liturgical_year_calculator

import android.util.Log
import com.qjproject.liturgicalcalendar.ui.screens.calendar.CalendarRepository.model.LiturgicalEventDetails
import java.time.LocalDate

class LiturgicalYearCalculator(private val allEvents: List<LiturgicalEventDetails>) {

    fun buildLiturgicalYearMap(year: Int): Map<LocalDate, LiturgicalDayContext> {
        Log.d("LiturgicalYearCalculator", "Attempting to build liturgical map for year: $year")
        val boundaryDates = findBoundaryDates(year, allEvents)
        if (boundaryDates == null) {
            Log.e("LiturgicalYearCalculator", "Failed to find all boundary dates for year $year. Cannot build map.")
            return emptyMap()
        }
        Log.d("LiturgicalYearCalculator", "Found boundary dates: $boundaryDates")

        val yearMap = mutableMapOf<LocalDate, LiturgicalDayContext>()
        var currentDate = LocalDate.of(year, 1, 1)
        val endDate = LocalDate.of(year, 12, 31)

        while (!currentDate.isAfter(endDate)) {
            val context = determineContextForDate(currentDate, boundaryDates)
            yearMap[currentDate] = context
            currentDate = currentDate.plusDays(1)
        }
        Log.d("LiturgicalYearCalculator", "Successfully built liturgical map for year: $year")
        return yearMap
    }
}