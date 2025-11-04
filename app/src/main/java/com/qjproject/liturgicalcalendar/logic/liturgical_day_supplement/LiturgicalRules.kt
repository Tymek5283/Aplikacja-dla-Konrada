// Ścieżka: app/src/main/java/com/qjproject/liturgicalcalendar/logic/liturgical_day_supplement/LiturgicalRules.kt
// Opis: Centralne miejsce przechowywania reguł liturgicznych. Definiuje hierarchię rang wydarzeń.

package com.qjproject.liturgicalcalendar.logic.liturgical_day_supplement

import com.qjproject.liturgicalcalendar.ui.screens.calendar.CalendarRepository.model.LiturgicalEventDetails
import com.qjproject.liturgicalcalendar.ui.screens.calendar.CalendarRepository.model.LiturgicalRank

internal object LiturgicalRules {
    val rankHierarchy = mapOf(
        "Uroczystość" to LiturgicalRank.SOLEMNITY,
        "Święto" to LiturgicalRank.FEAST,
        "Wspomnienie obowiązkowe" to LiturgicalRank.MEMORIAL_OBLIGATORY,
        "Wspomnienie dowolne" to LiturgicalRank.MEMORIAL_OPTIONAL,
        "" to LiturgicalRank.WEEKDAY
    )

    fun getDominantEvent(events: List<LiturgicalEventDetails>): LiturgicalEventDetails? {
        if (events.isEmpty()) return null
        return events.minByOrNull { rankHierarchy[it.typ]?.value ?: Int.MAX_VALUE }
    }
    fun getDominantEventForColor(events: List<LiturgicalEventDetails>): LiturgicalEventDetails? {
        if (events.isEmpty()) return null
        fun colorPriority(event: LiturgicalEventDetails): Int {
            return when (rankHierarchy[event.typ]) {
                LiturgicalRank.SOLEMNITY -> 1
                LiturgicalRank.FEAST -> 2
                LiturgicalRank.MEMORIAL_OBLIGATORY -> 3
                LiturgicalRank.WEEKDAY -> 4
                LiturgicalRank.MEMORIAL_OPTIONAL -> 5
                else -> Int.MAX_VALUE
            }
        }
        return events.minByOrNull { colorPriority(it) }
    }
}