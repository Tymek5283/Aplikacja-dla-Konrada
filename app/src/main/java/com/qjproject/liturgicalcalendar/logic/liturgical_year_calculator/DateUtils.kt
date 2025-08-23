// Ścieżka: app/src/main/java/com/qjproject/liturgicalcalendar/logic/liturgical_year_calculator/DateUtils.kt
// Opis: Ten plik zawiera funkcje pomocnicze do operacji na datach w kalendarzu liturgicznym.

package com.qjproject.liturgicalcalendar.logic.liturgical_year_calculator

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale

internal fun calculateWeekOfSeason(currentDate: LocalDate, seasonStartDate: LocalDate, weekStartDay: DayOfWeek): Int {
    val weekFields = WeekFields.of(weekStartDay, 1)
    val startWeek = seasonStartDate.get(weekFields.weekOfYear())
    val currentWeek = currentDate.get(weekFields.weekOfYear())
    if (currentWeek < startWeek) {
        val weeksInStartYear = WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear().range().maximum
        return ((weeksInStartYear - startWeek + currentWeek) + 1).toInt()
    }
    return (currentWeek - startWeek) + 1
}

internal fun calculateWeekInOrdinaryTime(currentDate: LocalDate, pentecostSunday: LocalDate): Int {
    val weekFields = WeekFields.of(Locale.GERMANY)
    val pentecostWeekOfYear = pentecostSunday.get(weekFields.weekOfWeekBasedYear())
    val currentWeekOfYear = currentDate.get(weekFields.weekOfWeekBasedYear())
    // --- POCZĄTEK ZMIANY ---
    // Poprawiono obliczenia tygodnia, aby usunąć błąd przesunięcia o 1.
    return currentWeekOfYear - pentecostWeekOfYear + 9
    // --- KONIEC ZMIANY ---
}