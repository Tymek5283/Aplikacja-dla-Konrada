package com.qjproject.liturgicalcalendar.ui.screens.calendar

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

sealed class NavigationAction {
    data class NavigateToDay(val path: String) : NavigationAction()
    data class ShowDateEvents(val title: String, val paths: List<String>) : NavigationAction()
}

@Serializable
data class LiturgicalEventDetails(
    val name: String,
    val data: String,
    val rok_litera: String,
    val rok_cyfra: String,
    val typ: String,
    val kolor: String
)

data class LiturgicalYearInfo(
    val mainInfo: String,
    val transitionInfo: String?
)

data class CalendarDay(
    val dayOfMonth: Int,
    val month: YearMonth,
    val isToday: Boolean,
    val events: List<LiturgicalEventDetails>,
    val dominantEventColorName: String?
) {
    val hasEvents: Boolean get() = events.isNotEmpty()
}


class LiturgicalYear(private val events: Map<String, LiturgicalEventDetails>) {
    private val eventsByDate by lazy {
        events.values.groupBy {
            LocalDate.parse(it.data, DateTimeFormatter.ofPattern("dd-MM-yyyy"))
        }
    }

    fun eventsForDate(date: LocalDate): List<LiturgicalEventDetails> {
        return eventsByDate[date] ?: emptyList()
    }
}


class CalendarRepository(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    private val calendarDir = File(context.filesDir, "kalendarz").apply { mkdirs() }
    private val cache = mutableMapOf<Int, LiturgicalYear>()

    private val eventTypeHierarchy = mapOf(
        "Uroczystość" to 1,
        "Święto" to 2,
        "Wspomnienie obowiązkowe" to 3,
        "" to 4,
        "Wspomnienie dowolne" to 5
    )

    suspend fun getLiturgicalYear(year: Int): LiturgicalYear? = withContext(Dispatchers.IO) {
        if (cache.containsKey(year)) return@withContext cache[year]

        val file = File(calendarDir, "$year.json")
        if (!file.exists()) return@withContext null

        return@withContext try {
            val jsonString = file.readText()
            val jsonObject = Json.parseToJsonElement(jsonString).jsonObject
            val events = jsonObject.mapValues {
                Json.decodeFromJsonElement<LiturgicalEventDetails>(it.value).copy(name = it.key)
            }
            val liturgicalYear = LiturgicalYear(events)
            cache[year] = liturgicalYear
            liturgicalYear
        } catch (e: Exception) {
            null
        }
    }

    fun getAvailableYears(): List<Int> {
        return calendarDir.listFiles { _, name -> name.endsWith(".json") }
            ?.mapNotNull { it.nameWithoutExtension.toIntOrNull() }
            ?.sorted() ?: emptyList()
    }

    fun isYearAvailable(year: Int): Boolean = File(calendarDir, "$year.json").exists()

    suspend fun downloadAndSaveYearIfNeeded(year: Int): Result<Unit> = withContext(Dispatchers.IO) {
        if (isYearAvailable(year)) return@withContext Result.success(Unit)
        if (!isNetworkAvailable()) return@withContext Result.failure(IOException("Brak połączenia z internetem."))

        var connection: HttpURLConnection? = null
        try {
            val url = URL("https://gcatholic.org/calendar/ics/$year-pl-PL.ics?v=3")
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val icsContent = connection.inputStream.bufferedReader().readText()
                val parsedEvents = parseIcsToEvents(icsContent)
                val jsonString = buildJsonString(parsedEvents)
                File(calendarDir, "$year.json").writeText(jsonString)
                cache.remove(year)
                Result.success(Unit)
            } else {
                Result.failure(IOException("Serwer odpowiedział kodem ${connection.responseCode} dla roku $year."))
            }
        } catch (e: Exception) {
            val errorMessage = when (e) {
                is UnknownHostException -> "Brak połączenia z internetem."
                is SocketTimeoutException -> "Przekroczono limit czasu odpowiedzi serwera."
                else -> e.localizedMessage ?: e.javaClass.simpleName
            }
            Result.failure(IOException(errorMessage))
        } finally {
            connection?.disconnect()
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    fun getDominantEvent(events: List<LiturgicalEventDetails>): LiturgicalEventDetails? {
        if (events.isEmpty()) return null
        return events.minWithOrNull(eventComparator)
    }

    val eventComparator = compareBy<LiturgicalEventDetails>(
        { eventTypeHierarchy[it.typ] ?: 99 },
        { !it.name.any { char -> char.isDigit() } },
        { it.name }
    )

    private fun formatYearId(rok_litera: String?, rok_cyfra: String?): String? {
        if (rok_litera.isNullOrBlank() || rok_litera.equals("null", ignoreCase = true) ||
            rok_cyfra.isNullOrBlank() || rok_cyfra.equals("null", ignoreCase = true)) {
            return null
        }
        return "$rok_litera, $rok_cyfra"
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
                    // Znaleziono punkt przejściowy. Utwórz informację tylko jeśli poprzedni rok jest prawidłowy.
                    currentYearId?.let {
                        val transitionDay = day + 1
                        val monthName = date.month.getDisplayName(TextStyle.FULL, Locale("pl"))
                        transitionInfo = "Rok obowiązujący do ${transitionDay - 1} $monthName: $it"
                    }
                    break // Przerwij pętlę bez względu na wszystko, ponieważ znaleziono pierwszy punkt różnicy.
                }
            }
        }

        return LiturgicalYearInfo(mainInfo, transitionInfo)
    }

    fun deleteAllCalendarFiles() {
        if (calendarDir.exists()) {
            calendarDir.listFiles()?.forEach { it.delete() }
        }
        cache.clear()
    }

    data class LiturgicalCycles(val sundayCycle: String, val weekdayCycle: String)

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

    private fun parseIcsToEvents(icsContent: String): List<LiturgicalEventDetails> {
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
                    val cycles = calculateLiturgicalCycles(eventDate)
                    val cleanedName = parseName(currentSummary)
                    val finalName = translationMap[cleanedName] ?: cleanedName

                    if (finalName.isNotBlank()) {
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

    private fun buildJsonString(events: List<LiturgicalEventDetails>): String {
        val eventMap = events.associateBy { it.name }
        return json.encodeToString(MapSerializer(String.serializer(), LiturgicalEventDetails.serializer()), eventMap)
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
}