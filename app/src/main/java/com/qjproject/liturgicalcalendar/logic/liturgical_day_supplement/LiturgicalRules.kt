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
}