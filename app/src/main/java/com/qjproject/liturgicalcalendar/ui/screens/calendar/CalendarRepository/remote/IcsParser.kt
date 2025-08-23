// Ścieżka: app/src/main/java/com/qjproject/liturgicalcalendar/ui/screens/calendar/CalendarRepository/remote/IcsParser.kt
// Opis: Zawiera logikę odpowiedzialną za parsowanie danych w formacie ICS. Przekształca surowy tekst z pliku .ics na listę obiektów `LiturgicalEventDetails`.

package com.qjproject.liturgicalcalendar.ui.screens.calendar.CalendarRepository.remote

import com.qjproject.liturgicalcalendar.ui.screens.calendar.CalendarRepository.model.LiturgicalEventDetails
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

internal class IcsParser {
    // Mapa tłumaczeń została przeniesiona do pliku constants.kt w folderze `model`
    fun parseIcsToEvents(icsContent: String, translationMap: Map<String, String>): List<LiturgicalEventDetails> {
        val events = mutableListOf<LiturgicalEventDetails>()
        val lines = icsContent.lines()
        val dateParser = DateTimeFormatter.ofPattern("yyyyMMdd")
        var i = 0
        while (i < lines.size) {
            if (lines[i] == "BEGIN:VEVENT") {
                var currentSummary = ""
                var currentDtstart = ""
                while (i < lines.size && lines[i] != "END:VEVENT") {
                    val line = lines[i]
                    when {
                        line.startsWith("DTSTART;VALUE=DATE:") -> currentDtstart = line.substringAfter(":")
                        line.startsWith("SUMMARY:") -> {
                            val summaryBuilder = StringBuilder(line.substringAfter(":"))
                            while (i + 1 < lines.size && lines[i + 1].startsWith(" ")) {
                                i++
                                summaryBuilder.append(lines[i].trimStart())
                            }
                            currentSummary = summaryBuilder.toString()
                        }
                    }
                    i++
                }

                if (currentDtstart.isNotEmpty() && currentSummary.isNotEmpty()) {
                    val eventDate = LocalDate.parse(currentDtstart, dateParser)
                    val cleanedName = parseName(currentSummary)
                    val finalName = translationMap[cleanedName] ?: cleanedName

                    if (finalName.isNotBlank()) {
                        val cycles = calculateLiturgicalCycles(eventDate)
                        events.add(
                            LiturgicalEventDetails(
                                name = finalName,
                                data = eventDate.format(DateTimeFormatter.ofPattern("dd-MM-yyyy")),
                                rok_litera = cycles.sundayCycle,
                                rok_cyfra = cycles.weekdayCycle,
                                typ = parseType(currentSummary),
                                kolor = parseColor(currentSummary)
                            )
                        )
                    }
                }
            }
            i++
        }
        return events
    }

    private fun parseColor(summary: String): String {
        val lowerSummary = summary.lowercase(Locale.getDefault())
        return when {
            "gaudete" in lowerSummary || "iii niedziela adwentu" in lowerSummary || "3 niedziela adwentu" in lowerSummary -> "Różowy"
            "laetare" in lowerSummary || "iv niedziela wielkiego postu" in lowerSummary || "4 niedziela wielkiego postu" in lowerSummary -> "Różowy"
            "⚪" in summary -> "Biały"
            "🔴" in summary -> "Czerwony"
            "🟢" in summary -> "Zielony"
            "🟣" in summary -> "Fioletowy"
            "💗" in summary || "🩷" in summary -> "Różowy"
            else -> "Nieznany"
        }
    }

    private fun parseType(summary: String): String {
        val code = "\\[(.*?)\\]".toRegex().find(summary)?.groupValues?.getOrNull(1) ?: return ""
        return when (code) {
            "U" -> "Uroczystość"; "Ś" -> "Święto"; "W" -> "Wspomnienie obowiązkowe"
            "w", "w*" -> "Wspomnienie dowolne"; else -> ""
        }
    }

    private fun parseName(summary: String): String {
        return summary.replace(Regex("\\[.*?\\]\\s*"), "")
            .replace(Regex("[⚪🔴🟢🟣💗🩷?]"), "")
            .replace("\\", "")
            .replace("/", "")
            .trim()
            .replace(Regex("\\s+"), " ")
    }

    private data class LiturgicalCycles(val sundayCycle: String, val weekdayCycle: String)

    private fun calculateLiturgicalCycles(eventDate: LocalDate): LiturgicalCycles {
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
}