// Ścieżka: app/src/main/java/com/qjproject/liturgicalcalendar/logic/liturgical_day_supplement/LiturgicalDaySupplement.kt
// Opis: Główny moduł logiki odpowiedzialny za augmentację (uzupełnianie) kalendarza liturgicznego o brakujące dni powszednie.
package com.qjproject.liturgicalcalendar.logic.liturgical_day_supplement

import android.util.Log
import com.qjproject.liturgicalcalendar.logic.liturgical_year_calculator.LiturgicalDayContext
import com.qjproject.liturgicalcalendar.logic.liturgical_year_calculator.LiturgicalSeason
import com.qjproject.liturgicalcalendar.logic.liturgical_year_calculator.LiturgicalYearCalculator
import com.qjproject.liturgicalcalendar.ui.screens.calendar.CalendarRepository.model.LiturgicalEventDetails
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

internal object LiturgicalDaySupplement {

    private const val TAG = "LiturgicalSupplement"

    fun augmentYearlyEvents(events: List<LiturgicalEventDetails>): List<LiturgicalEventDetails> {
        if (events.isEmpty()) {
            Log.w(TAG, "Input event list is empty. Aborting augmentation.")
            return emptyList()
        }
        val yearString = events.firstOrNull()?.data?.substring(6, 10)
        if (yearString == null) {
            Log.e(TAG, "Could not determine year from first event. Aborting.")
            return events
        }
        val year = yearString.toIntOrNull()
        if (year == null) {
            Log.e(TAG, "Could not parse year from first event. Aborting.")
            return events
        }

        Log.d(TAG, "Starting augmentation for year $year.")
        val calculator = LiturgicalYearCalculator(events)
        val liturgicalYearMap = calculator.buildLiturgicalYearMap(year)

        if (liturgicalYearMap.isEmpty()) {
            Log.e(TAG, "LiturgicalYearMap is empty. Returning original events.")
            return events
        }

        val eventsByDate = events.groupBy { LocalDate.parse(it.data, DateTimeFormatter.ofPattern("dd-MM-yyyy")) }
        val augmentedEvents = events.toMutableList()

        for ((date, context) in liturgicalYearMap) {
            val dailyEvents = eventsByDate[date] ?: emptyList()
            Log.d(TAG, ">>> Processing Date: $date")
            if (dailyEvents.isNotEmpty()) {
                Log.d(TAG, "Found ${dailyEvents.size} existing event(s): ${dailyEvents.joinToString { "'${it.name}' (typ: '${it.typ}')" }}")
            } else {
                Log.d(TAG, "No existing events for this date.")
            }

            val isDayEmpty = dailyEvents.isEmpty()

            val hasOnlyMemorials = dailyEvents.isNotEmpty() && dailyEvents.all {
                it.typ == "Wspomnienie obowiązkowe" || it.typ == "Wspomnienie dowolne"
            }
            Log.d(TAG, "Check result: isDayEmpty = $isDayEmpty, hasOnlyMemorials = $hasOnlyMemorials")

            if (isDayEmpty || hasOnlyMemorials) {
                Log.i(TAG, "CONDITION MET for $date. Attempting to create and add weekday event.")
                createWeekdayEvent(context, events)?.let { newEvent ->
                    if (augmentedEvents.none { it.data == newEvent.data && it.name == newEvent.name }) {
                        Log.i(TAG, "Successfully created and added new event: '${newEvent.name}'")
                        augmentedEvents.add(newEvent)
                    } else {
                        Log.w(TAG, "Weekday event '${newEvent.name}' already exists. Skipping.")
                    }
                }
            } else {
                Log.d(TAG, "CONDITION NOT MET for $date. Skipping weekday creation.")
            }
        }
        Log.d(TAG, "Augmentation finished. Total events: ${augmentedEvents.size}")
        return augmentedEvents.distinctBy { it.name + it.data }
    }

    private fun createWeekdayEvent(context: LiturgicalDayContext, allEvents: List<LiturgicalEventDetails>): LiturgicalEventDetails? {
        val (date, season, weekNumber) = context

        val periodName = when (season) {
            LiturgicalSeason.ADVENT -> "Adwentu"
            LiturgicalSeason.LENT -> "Wielkiego Postu"
            LiturgicalSeason.EASTER_TIME -> "Okresu Wielkanocnego"
            LiturgicalSeason.ORDINARY_TIME_PART1, LiturgicalSeason.ORDINARY_TIME_PART2 -> "Okresu Zwykłego"
            else -> {
                Log.v(TAG, "Skipping weekday creation for season '$season' on date $date.")
                return null
            }
        }

        val dayOfWeekName = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("pl"))
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

        // --- POCZĄTEK ZMIANY ---
        // Poprawiono format nazwy dnia, aby był zgodny ze schematem: [numer] [dzień] [okres].
        val newEventName = "$weekNumber $dayOfWeekName $periodName"
        // --- KONIEC ZMIANY ---
        Log.d(TAG, "Generated event name: '$newEventName'")

        val color = when (season) {
            LiturgicalSeason.ADVENT, LiturgicalSeason.LENT -> "Fioletowy"
            LiturgicalSeason.EASTER_TIME -> "Biały"
            else -> "Zielony"
        }

        val baseEventForCycles = allEvents.firstOrNull { LocalDate.parse(it.data, DateTimeFormatter.ofPattern("dd-MM-yyyy")) == date }
            ?: allEvents.firstOrNull()

        if (baseEventForCycles == null) {
            Log.e(TAG, "Could not find any base event to determine liturgical cycles for date $date.")
        }

        return LiturgicalEventDetails(
            name = newEventName,
            data = date.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")),
            rok_litera = baseEventForCycles?.rok_litera ?: "N/A",
            rok_cyfra = baseEventForCycles?.rok_cyfra ?: "N/A",
            typ = "",
            kolor = color
        )
    }
}