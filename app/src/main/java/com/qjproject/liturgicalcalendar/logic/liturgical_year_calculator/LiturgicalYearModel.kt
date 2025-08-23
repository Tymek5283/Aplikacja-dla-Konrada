// Ścieżka: app/src/main/java/com/qjproject/liturgicalcalendar/logic/liturgical_year_calculator/LiturgicalYearModel.kt
// Opis: Definiuje podstawowe modele danych i typy wyliczeniowe używane w logice kalendarza liturgicznego, takie jak okresy liturgiczne i kontekst dnia.
package com.qjproject.liturgicalcalendar.logic.liturgical_year_calculator

import java.time.LocalDate

enum class LiturgicalSeason {
    ADVENT,
    CHRISTMAS_TIME,
    ORDINARY_TIME_PART1,
    LENT,
    TRIDUUM,
    EASTER_TIME,
    ORDINARY_TIME_PART2
}

data class LiturgicalDayContext(
    val date: LocalDate,
    val season: LiturgicalSeason,
    val week: Int
)