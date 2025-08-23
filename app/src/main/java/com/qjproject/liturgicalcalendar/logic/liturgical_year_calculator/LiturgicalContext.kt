// Ścieżka: app/src/main/java/com/qjproject/liturgicalcalendar/logic/liturgical_year_calculator/LiturgicalContext.kt
// Opis: Ten plik zawiera logikę do określania kontekstu liturgicznego dla danej daty.

package com.qjproject.liturgicalcalendar.logic.liturgical_year_calculator

import java.time.DayOfWeek
import java.time.LocalDate

internal fun determineContextForDate(date: LocalDate, boundaries: BoundaryDates): LiturgicalDayContext {
    val (firstSundayOfAdvent, christmas, baptismOfLord, ashWednesday, holyThursday, easterSunday, pentecostSunday) = boundaries

    val season: LiturgicalSeason
    val week: Int

    when {
        date.isAfter(pentecostSunday) && date.isBefore(firstSundayOfAdvent.withYear(date.year)) -> {
            season = LiturgicalSeason.ORDINARY_TIME_PART2
            week = calculateWeekInOrdinaryTime(date, pentecostSunday)
        }
        date.isAfter(easterSunday.minusDays(1)) && date.isBefore(pentecostSunday.plusDays(1)) -> {
            season = LiturgicalSeason.EASTER_TIME
            week = calculateWeekOfSeason(date, easterSunday, DayOfWeek.SUNDAY)
        }
        date.isAfter(holyThursday.minusDays(1)) && date.isBefore(easterSunday) -> {
            season = LiturgicalSeason.TRIDUUM
            week = 0
        }
        date.isAfter(ashWednesday.minusDays(1)) && date.isBefore(holyThursday) -> {
            season = LiturgicalSeason.LENT
            week = calculateWeekOfSeason(date, ashWednesday, DayOfWeek.WEDNESDAY)
        }
        date.isAfter(baptismOfLord) && date.isBefore(ashWednesday) -> {
            season = LiturgicalSeason.ORDINARY_TIME_PART1
            week = calculateWeekOfSeason(date, baptismOfLord.plusDays(1), DayOfWeek.MONDAY)
        }
        date.isAfter(christmas.minusDays(1)) && date.isBefore(baptismOfLord.plusDays(1)) -> {
            season = LiturgicalSeason.CHRISTMAS_TIME
            week = if (date.isBefore(christmas.with(DayOfWeek.SUNDAY))) 1 else 2
        }
        else -> {
            season = LiturgicalSeason.ADVENT
            val adventStartYear = if (date.monthValue == 12) date.year else date.year - 1
            week = calculateWeekOfSeason(date, firstSundayOfAdvent.withYear(adventStartYear), DayOfWeek.SUNDAY)
        }
    }
    return LiturgicalDayContext(date, season, week)
}