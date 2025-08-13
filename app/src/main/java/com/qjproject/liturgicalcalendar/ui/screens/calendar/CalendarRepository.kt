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
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
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

    fun getLiturgicalYearInfoForMonth(yearMonth: YearMonth, yearData: LiturgicalYear): LiturgicalYearInfo {
        val lastDayOfMonth = yearMonth.atEndOfMonth()
        val eventsOnLastDay = yearData.eventsForDate(lastDayOfMonth)
        val dominantEventOnLastDay = getDominantEvent(eventsOnLastDay)

        if (dominantEventOnLastDay == null) {
            return LiturgicalYearInfo("Brak danych o roku liturgicznym", null)
        }

        val finalYearId = "${dominantEventOnLastDay.rok_litera}, ${dominantEventOnLastDay.rok_cyfra}"
        val mainInfo = "Aktualny rok: $finalYearId"
        var transitionInfo: String? = null

        var transitionDay = -1
        for (day in lastDayOfMonth.dayOfMonth - 1 downTo 1) {
            val date = yearMonth.atDay(day)
            val events = yearData.eventsForDate(date)
            val dominantEvent = getDominantEvent(events)
            val currentYearId = "${dominantEvent?.rok_litera}, ${dominantEvent?.rok_cyfra}"

            if (currentYearId != finalYearId) {
                transitionDay = day + 1
                val monthName = date.month.getDisplayName(TextStyle.FULL, Locale("pl"))
                transitionInfo = "Rok do dnia ${transitionDay - 1} $monthName: $currentYearId"
                break
            }
        }

        return LiturgicalYearInfo(mainInfo, transitionInfo)
    }

    // --- POCZĄTEK ZMIANY ---
    fun deleteAllCalendarFiles() {
        if (calendarDir.exists()) {
            calendarDir.listFiles()?.forEach { it.delete() }
        }
        cache.clear()
    }
    // --- KONIEC ZMIANY ---


    // Zgodnie z prośbą, zawartość tej mapy została pominięta.
    private val translationMap: Map<String, String> = mapOf(
        "Czwartek I tygodnia Adwentu" to "1 Czwartek Adwentu",
        "Piątek I tygodnia Adwentu" to "1 Piątek Adwentu",
        "Poniedziałek I tygodnia Adwentu" to "1 Poniedziałek Adwentu",
        "Sobota I tygodnia Adwentu" to "1 Sobota Adwentu",
        "Wtorek I tygodnia Adwentu" to "1 Wtorek Adwentu",
        "Środa I tygodnia Adwentu" to "1 Środa Adwentu",
        "I Niedziela Adwentu" to "1 Niedziela Adwentu",
        "Czwartek II tygodnia Adwentu" to "2 Czwartek Adwentu",
        "Piątek II tygodnia Adwentu" to "2 Piątek Adwentu",
        "Poniedziałek II tygodnia Adwentu" to "2 Poniedziałek Adwentu",
        "Sobota II tygodnia Adwentu" to "2 Sobota Adwentu",
        "Wtorek II tygodnia Adwentu" to "2 Wtorek Adwentu",
        "Środa II tygodnia Adwentu" to "2 Środa Adwentu",
        "II Niedziela Adwentu" to "2 Niedziela Adwentu",
        "Czwartek III tygodnia Adwentu" to "3 Czwartek Adwentu",
        "Piątek III tygodnia Adwentu" to "3 Piątek Adwentu",
        "Poniedziałek III tygodnia Adwentu" to "3 Poniedziałek Adwentu",
        "Wtorek III tygodnia Adwentu" to "3 Wtorek Adwentu",
        "Środa III tygodnia Adwentu" to "3 Środa Adwentu",
        "III Niedziela Adwentu" to "3 Niedziela Adwentu",
        "IV Niedziela Adwentu" to "4 Niedziela Adwentu",
        "Dzień adwentu (17 grudnia)" to "17 grudnia - Dzień powszedni w okresie Adwentu",
        "Dzień adwentu (18 grudnia)" to "18 grudnia - Dzień powszedni w okresie Adwentu",
        "Dzień adwentu (19 grudnia)" to "19 grudnia - Dzień powszedni w okresie Adwentu",
        "Dzień adwentu (20 grudnia)" to "20 grudnia - Dzień powszedni w okresie Adwentu",
        "Dzień adwentu (21 grudnia)" to "21 grudnia - Dzień powszedni w okresie Adwentu",
        "Dzień adwentu (22 grudnia)" to "22 grudnia - Dzień powszedni w okresie Adwentu",
        "Dzień adwentu (23 grudnia)" to "23 grudnia - Dzień powszedni w okresie Adwentu",
        "Dzień adwentu (24 grudnia)" to "24 grudnia - Dzień powszedni w okresie Adwentu",
        "Św. Barnaby, Apostoła" to "11 czerwca - św. Barnaby Apostoła",
        "Narodzenie św. Jana Chrzciciela" to "24 czerwca - Uroczystość Narodzenia św. Jana Chrzciciela",
        "Świętych Apostołów Piotra i Pawła" to "29 czerwca - Uroczystość św. Apostołów Piotra i Pawła",
        "Niepokalane poczęcie Najświętszej Maryi Panny" to "8 grudnia - Uroczystość Niepokalanego Poczęcia NMP",
        "Św. Wojciecha, biskupa i męczennika" to "23 kwietnia - Uroczystość św. Wojciecha, biskupa i męczennika, głównego patrona Polski",
        "Św. Marka, Ewangelisty" to "25 kwietnia - św. Marka Ewangelisty",
        "Św. Katarzyny Sieneńskiej, dziewicy i doktora Kościoła" to "29 kwietnia - św. Katarzyny ze Sieny, dziewicy i doktora Kościoła",
        "Św. Benedykta, opata" to "11 lipca - św. Benedykta, patrona Europy",
        "Św. Marii Magdaleny" to "22 lipca - Święto św. Marii Magdaleny",
        "Św. Brygidy, zakonnicy" to "23 lipca - Święto św. Brygidy, patronki Europy",
        "Św. Jakuba, Apostoła" to "25 lipca - św. Jakuba Apostoła",
        "Świętych Marty, Marii i Łazarza" to "29 lipca - św. Marty",
        "Św. Tomasza, Apostoła" to "3 lipca - św. Tomasza Apostoła",
        "Wszystkich Świętych" to "1 listopada - Uroczystość Wszystkich Świętych",
        "Wspomnienie wszystkich wiernych zmarłych" to "2 listopada - Wspomnienie Wszystkich Wiernych Zmarłych",
        "Ofiarowanie Najświętszej Maryi Panny" to "21 listopada - Ofiarowanie Najświętszej Maryi Panny",
        "Św. Andrzeja, Apostoła" to "30 listopada - św. Andrzeja Apostoła",
        "Rocznica poświęcenia Bazyliki Laterańskiej" to "9 listopada - Rocznica poświęcenia Bazyliki Laterańskiej",
        "Świętych Cyryla, mnicha i Metodego, biskupa" to "14 lutego - św. Cyryla i Metodego, patronów Europy",
        "Ofiarowanie Pańskie" to "2 lutego - Ofiarowanie Pańskie",
        "Św. Józefa, rzemieślnika" to "1 maja - św. Józefa rzemieślnika",
        "Św. Macieja, Apostoła" to "14 maja - św. Macieja Apostoła",
        "Św. Andrzeja Boboli, prezbitera i męczennika" to "16 maja - św. Andrzeja Boboli",
        "Najświętszej Maryi Panny, Królowej Polski" to "3 maja - Uroczystość NMP Królowej Polski",
        "Świętych Apostołów Filipa i Jakuba" to "6 maja - św. Apostołów Filipa i Jakuba",
        "Św. Stanisława, biskupa i męczennika" to "8 maja - Uroczystość św. Stanisława, głównego patrona Polski",
        "Św. Józefa, Oblubieńca Najświętszej Maryi Panny" to "19 marca - Uroczystość św. Józefa, Oblubieńca NMP",
        "Zwiastowanie Pańskie" to "25 marca - Uroczystość Zwiastowania Pańskiego",
        "Św. Kazimierza" to "4 marca - św. Kazimierza królewicza",
        "Świętych Aniołów Stróżów" to "2 października - Świętych Aniołów Stróżów",
        "Najświętszej Maryi Panny Częstochowskiej" to "26 sierpnia - Uroczystość NMP Częstochowskiej",
        "Świętych Apostołów Szymona i Judy Tadeusza" to "28 października - św. Apostołów Szymona i Judy Tadeusza",
        "Męczeństwo św. Jana Chrzciciela" to "29 sierpnia - Męczeństwo św. Jana Chrzciciela",
        "Św. Wawrzyńca, diakona i męczennika" to "10 sierpnia - św. Wawrzyńca",
        "Św. Maksymiliana Marii Kolbego, prezbitera i męczennika" to "14 sierpnia - św. Maksymiliana Marii Kolbego",
        "Wniebowzięcie Najświętszej Maryi Panny" to "15 sierpnia - Uroczystość Wniebowzięcia NMP",
        "Najświętszej Maryi Panny, Królowej" to "22 sierpnia - NMP Królowej Polski",
        "Św. Bartłomieja, Apostoła" to "24 sierpnia - Święto św. Bartłomieja, Apostoła",
        "Przemienienie Pańskie" to "6 sierpnia - Święto Przemienienia Pańskiego",
        "Świętych biskupów Tymoteusza i Tytusa" to "26 stycznia - św. biskupów Tymoteusza i Tytusa",
        "Podwyższenie Krzyża Świętego" to "14 września - Święto Podwyższenia Krzyża Świętego",
        "Najświętszej Maryi Panny Bolesnej" to "15 września - NMP Bolesnej",
        "Św. Stanisława Kostki, zakonnika" to "18 września - św. Stanisława Kostki, zakonnika, patrona Polski",
        "Św. Mateusza, Apostoła i Ewangelisty" to "21 września - Święto św. Mateusza, Apostoła i Ewangelisty",
        "Świętych Archaniołów Michała, Gabriela i Rafała" to "29 września - Święto św. Archaniołów Michała, Gabriela i Rafała",
        "Narodzenie Najświętszej Maryi Panny" to "8 września - Narodzenie NMP",
        "Świętej Bożej Rodzicielki Maryi" to "1 stycznia - Uroczystość Świętej Bożej Rodzicielki Maryi",
        "Chrzest Pański" to "Niedziela Chrztu Pańskiego",
        "II Niedziela po Bożym Narodzeniu" to "2 Niedziela po Narodzeniu Pańskim",
        "Świętych Bazylego Wielkiego i Grzegorza z Nazjanzu, biskupów i doktorów Kościoła" to "2 stycznia - Dzień powszedni w Okresie Narodzenia Pańskiego",
        "Narodzenie Pańskie" to "25 grudnia - Uroczystość Narodzenia Pańskiego",
        "Św. Szczepana, pierwszego męczennika" to "26 grudnia - Święto św. Szczepana, pierwszego męczennika",
        "Świętych Młodziaków, męczenników" to "28 grudnia - Święto Świętych Młodzianków, męczenników",
        "V dzień w oktawie Narodzenia Pańskiego" to "29 grudnia - Piąty dzień w oktawie Narodzenia Pańskiego",
        "VI dzień w oktawie Narodzenia Pańskiego" to "30 grudnia - Szósty dzień w oktawie Narodzenia Pańskiego",
        "VII dzień w oktawie Narodzenia Pańskiego" to "31 grudnia - Siódmy dzień w oktawie Narodzenia Pańskiego",
        "Objawienie Pańskie" to "6 stycznia - Uroczystość Objawienia Pańskiego",
        "Świętej Rodziny Jezusa, Maryi i Józefa" to "Niedziela w oktawie Narodzenia Pańskiego - Święto Świętej Rodziny",
        "Czwartek w Oktawie Wielkanocy" to "Czwartek w Oktawie Wielkanocy",
        "Niedziela Zmartwychwstania Pańskiego" to "Niedziela Zmartwychwstania Pańskiego",
        "Piątek w Oktawie Wielkanocy" to "Piątek w Oktawie Wielkanocy",
        "Poniedziałek w Oktawie Wielkanocy" to "Poniedziałek w Oktawie Wielkanocy",
        "Sobota w Oktawie Wielkanocy" to "Sobota w Oktawie Wielkanocy",
        "Wtorek w Oktawie Wielkanocy" to "Wtorek w Oktawie Wielkanocy",
        "Środa w Oktawie Wielkanocy" to "Środa w Oktawie Wielkanocy",
        "Czwartek II Tygodnia Wielkanocnego" to "2 Czwartek Okresu Wielkanocnego",
        "Piątek II Tygodnia Wielkanocnego" to "2 Piątek Okresu Wielkanocnego",
        "Poniedziałek II Tygodnia Wielkanocnego" to "2 Poniedziałek Okresu Wielkanocnego",
        "Sobota II Tygodnia Wielkanocnego" to "2 Sobota Okresu Wielkanocnego",
        "Wtorek II Tygodnia Wielkanocnego" to "2 Wtorek Okresu Wielkanocnego",
        "Środa II Tygodnia Wielkanocnego" to "2 Środa Okresu Wielkanocnego",
        "II Niedziela Wielkanocna czyli Miłosierdzia Bożego" to "2 Niedziela Okresu Wielkanocnego",
        "Czwartek III Tygodnia Wielkanocnego" to "3 Czwartek Okresu Wielkanocnego",
        "Piątek III Tygodnia Wielkanocnego" to "3 Piątek Okresu Wielkanocnego",
        "Poniedziałek III Tygodnia Wielkanocnego" to "3 Poniedziałek Okresu Wielkanocnego",
        "Sobota III Tygodnia Wielkanocnego" to "3 Sobota Okresu Wielkanocnego",
        "Wtorek III Tygodnia Wielkanocnego" to "3 Wtorek Okresu Wielkanocnego",
        "Środa III Tygodnia Wielkanocnego" to "3 Środa Okresu Wielkanocnego",
        "III Niedziela Wielkanocna" to "3 Niedziela Okresu Wielkanocnego",
        "Czwartek IV Tygodnia Wielkanocnego" to "4 Czwartek Okresu Wielkanocnego",
        "Piątek IV Tygodnia Wielkanocnego" to "4 Piątek Okresu Wielkanocnego",
        "Poniedziałek IV Tygodnia Wielkanocnego" to "4 Poniedziałek Okresu Wielkanocnego",
        "Sobota IV Tygodnia Wielkanocnego" to "4 Sobota Okresu Wielkanocnego",
        "Wtorek IV Tygodnia Wielkanocnego" to "4 Wtorek Okresu Wielkanocnego",
        "Środa IV Tygodnia Wielkanocnego" to "4 Środa Okresu Wielkanocnego",
        "IV Niedziela Wielkanocna" to "4 Niedziela Okresu Wielkanocnego",
        "Czwartek V Tygodnia Wielkanocnego" to "5 Czwartek Okresu Wielkanocnego",
        "Piątek V Tygodnia Wielkanocnego" to "5 Piątek Okresu Wielkanocnego",
        "Poniedziałek V Tygodnia Wielkanocnego" to "5 Poniedziałek Okresu Wielkanocnego",
        "Sobota V Tygodnia Wielkanocnego" to "5 Sobota Okresu Wielkanocnego",
        "Wtorek V Tygodnia Wielkanocnego" to "5 Wtorek Okresu Wielkanocnego",
        "Środa V Tygodnia Wielkanocnego" to "5 Środa Okresu Wielkanocnego",
        "V Niedziela Wielkanocna" to "5 Niedziela Okresu Wielkanocnego",
        "Czwartek VI Tygodnia Wielkanocnego" to "6 Czwartek Okresu Wielkanocnego",
        "Piątek VI Tygodnia Wielkanocnego" to "6 Piątek Okresu Wielkanocnego",
        "Poniedziałek VI Tygodnia Wielkanocnego" to "6 Poniedziałek Okresu Wielkanocnego",
        "Sobota VI Tygodnia Wielkanocnego" to "6 Sobota Okresu Wielkanocnego",
        "Wtorek VI Tygodnia Wielkanocnego" to "6 Wtorek Okresu Wielkanocnego",
        "Środa VI Tygodnia Wielkanocnego" to "6 Środa Okresu Wielkanocnego",
        "VI Niedziela Wielkanocna" to "6 Niedziela Okresu Wielkanocnego",
        "Czwartek VII Tygodnia Wielkanocnego" to "7 Czwartek Okresu Wielkanocnego",
        "Piątek VII Tygodnia Wielkanocnego" to "7 Piątek Okresu Wielkanocnego",
        "Poniedziałek VII Tygodnia Wielkanocnego" to "7 Poniedziałek Okresu Wielkanocnego",
        "Sobota VII Tygodnia Wielkanocnego" to "7 Sobota Okresu Wielkanocnego",
        "Wtorek VII Tygodnia Wielkanocnego" to "7 Wtorek Okresu Wielkanocnego",
        "Środa VII Tygodnia Wielkanocnego" to "7 Środa Okresu Wielkanocnego",
        "VII Niedziela Wielkanocna" to "7 Niedziela Okresu Wielkanocnego",
        "Wniebowstąpienie Pańskie" to "Uroczystość Wniebowstąpienia Pańskiego",
        "Czwartek I tygodnia zwykłego" to "1 Czwartek Okresu Zwykłego",
        "Piątek I tygodnia zwykłego" to "1 Piątek Okresu Zwykłego",
        "Poniedziałek I tygodnia zwykłego" to "1 Poniedziałek Okresu Zwykłego",
        "Sobota I tygodnia zwykłego" to "1 Sobota Okresu Zwykłego",
        "Wtorek I tygodnia zwykłego" to "1 Wtorek Okresu Zwykłego",
        "Środa I tygodnia zwykłego" to "1 Środa Okresu Zwykłego",
        "Czwartek X tygodnia zwykłego" to "10 Czwartek Okresu Zwykłego",
        "X Niedziela Zwykła" to "10 Niedziela Okresu Zwykłego",
        "Piątek X tygodnia zwykłego" to "10 Piątek Okresu Zwykłego",
        "Poniedziałek X tygodnia zwykłego" to "10 Poniedziałek Okresu Zwykłego",
        "Sobota X tygodnia zwykłego" to "10 Sobota Okresu Zwykłego",
        "Wtorek X tygodnia zwykłego" to "10 Wtorek Okresu Zwykłego",
        "Środa X tygodnia zwykłego" to "10 Środa Okresu Zwykłego",
        "Czwartek XI tygodnia zwykłego" to "11 Czwartek Okresu Zwykłego",
        "XI Niedziela Zwykła" to "11 Niedziela Okresu Zwykłego",
        "Piątek XI tygodnia zwykłego" to "11 Piątek Okresu Zwykłego",
        "Poniedziałek XI tygodnia zwykłego" to "11 Poniedziałek Okresu Zwykłego",
        "Sobota XI tygodnia zwykłego" to "11 Sobota Okresu Zwykłego",
        "Wtorek XI tygodnia zwykłego" to "11 Wtorek Okresu Zwykłego",
        "Środa XI tygodnia zwykłego" to "11 Środa Okresu Zwykłego",
        "Czwartek XII tygodnia zwykłego" to "12 Czwartek Okresu Zwykłego",
        "XII Niedziela Zwykła" to "12 Niedziela Okresu Zwykłego",
        "Piątek XII tygodnia zwykłego" to "12 Piątek Okresu Zwykłego",
        "Poniedziałek XII tygodnia zwykłego" to "12 Poniedziałek Okresu Zwykłego",
        "Sobota XII tygodnia zwykłego" to "12 Sobota Okresu Zwykłego",
        "Wtorek XII tygodnia zwykłego" to "12 Wtorek Okresu Zwykłego",
        "Środa XII tygodnia zwykłego" to "12 Środa Okresu Zwykłego",
        "Czwartek XIII tygodnia zwykłego" to "13 Czwartek Okresu Zwykłego",
        "XIII Niedziela Zwykła" to "13 Niedziela Okresu Zwykłego",
        "Piątek XIII tygodnia zwykłego" to "13 Piątek Okresu Zwykłego",
        "Poniedziałek XIII tygodnia zwykłego" to "13 Poniedziałek Okresu Zwykłego",
        "Sobota XIII tygodnia zwykłego" to "13 Sobota Okresu Zwykłego",
        "Wtorek XIII tygodnia zwykłego" to "13 Wtorek Okresu Zwykłego",
        "Środa XIII tygodnia zwykłego" to "13 Środa Okresu Zwykłego",
        "Czwartek XIV tygodnia zwykłego" to "14 Czwartek Okresu Zwykłego",
        "XIV Niedziela Zwykła" to "14 Niedziela Okresu Zwykłego",
        "Piątek XIV tygodnia zwykłego" to "14 Piątek Okresu Zwykłego",
        "Poniedziałek XIV tygodnia zwykłego" to "14 Poniedziałek Okresu Zwykłego",
        "Sobota XIV tygodnia zwykłego" to "14 Sobota Okresu Zwykłego",
        "Wtorek XIV tygodnia zwykłego" to "14 Wtorek Okresu Zwykłego",
        "Środa XIV tygodnia zwykłego" to "14 Środa Okresu Zwykłego",
        "Czwartek XV tygodnia zwykłego" to "15 Czwartek Okresu Zwykłego",
        "XV Niedziela Zwykła" to "15 Niedziela Okresu Zwykłego",
        "Piątek XV tygodnia zwykłego" to "15 Piątek Okresu Zwykłego",
        "Poniedziałek XV tygodnia zwykłego" to "15 Poniedziałek Okresu Zwykłego",
        "Sobota XV tygodnia zwykłego" to "15 Sobota Okresu Zwykłego",
        "Wtorek XV tygodnia zwykłego" to "15 Wtorek Okresu Zwykłego",
        "Środa XV tygodnia zwykłego" to "15 Środa Okresu Zwykłego",
        "Czwartek XVI tygodnia zwykłego" to "16 Czwartek Okresu Zwykłego",
        "XVI Niedziela Zwykła" to "16 Niedziela Okresu Zwykłego",
        "Piątek XVI tygodnia zwykłego" to "16 Piątek Okresu Zwykłego",
        "Poniedziałek XVI tygodnia zwykłego" to "16 Poniedziałek Okresu Zwykłego",
        "Sobota XVI tygodnia zwykłego" to "16 Sobota Okresu Zwykłego",
        "Wtorek XVI tygodnia zwykłego" to "16 Wtorek Okresu Zwykłego",
        "Środa XVI tygodnia zwykłego" to "16 Środa Okresu Zwykłego",
        "Czwartek XVII tygodnia zwykłego" to "17 Czwartek Okresu Zwykłego",
        "XVII Niedziela Zwykła" to "17 Niedziela Okresu Zwykłego",
        "Piątek XVII tygodnia zwykłego" to "17 Piątek Okresu Zwykłego",
        "Poniedziałek XVII tygodnia zwykłego" to "17 Poniedziałek Okresu Zwykłego",
        "Sobota XVII tygodnia zwykłego" to "17 Sobota Okresu Zwykłego",
        "Wtorek XVII tygodnia zwykłego" to "17 Wtorek Okresu Zwykłego",
        "Środa XVII tygodnia zwykłego" to "17 Środa Okresu Zwykłego",
        "Czwartek XVIII tygodnia zwykłego" to "18 Czwartek Okresu Zwykłego",
        "XVIII Niedziela Zwykła" to "18 Niedziela Okresu Zwykłego",
        "Piątek XVIII tygodnia zwykłego" to "18 Piątek Okresu Zwykłego",
        "Poniedziałek XVIII tygodnia zwykłego" to "18 Poniedziałek Okresu Zwykłego",
        "Sobota XVIII tygodnia zwykłego" to "18 Sobota Okresu Zwykłego",
        "Wtorek XVIII tygodnia zwykłego" to "18 Wtorek Okresu Zwykłego",
        "Środa XVIII tygodnia zwykłego" to "18 Środa Okresu Zwykłego",
        "Czwartek XIX tygodnia zwykłego" to "19 Czwartek Okresu Zwykłego",
        "XIX Niedziela Zwykła" to "19 Niedziela Okresu Zwykłego",
        "Piątek XIX tygodnia zwykłego" to "19 Piątek Okresu Zwykłego",
        "Poniedziałek XIX tygodnia zwykłego" to "19 Poniedziałek Okresu Zwykłego",
        "Sobota XIX tygodnia zwykłego" to "19 Sobota Okresu Zwykłego",
        "Wtorek XIX tygodnia zwykłego" to "19 Wtorek Okresu Zwykłego",
        "Środa XIX tygodnia zwykłego" to "19 Środa Okresu Zwykłego",
        "Czwartek II tygodnia zwykłego" to "2 Czwartek Okresu Zwykłego",
        "II Niedziela Zwykła" to "2 Niedziela Okresu Zwykłego",
        "Piątek II tygodnia zwykłego" to "2 Piątek Okresu Zwykłego",
        "Poniedziałek II tygodnia zwykłego" to "2 Poniedziałek Okresu Zwykłego",
        "Sobota II tygodnia zwykłego" to "2 Sobota Okresu Zwykłego",
        "Wtorek II tygodnia zwykłego" to "2 Wtorek Okresu Zwykłego",
        "Środa II tygodnia zwykłego" to "2 Środa Okresu Zwykłego",
        "Czwartek XX tygodnia zwykłego" to "20 Czwartek Okresu Zwykłego",
        "XX Niedziela Zwykła" to "20 Niedziela Okresu Zwykłego",
        "Piątek XX tygodnia zwykłego" to "20 Piątek Okresu Zwykłego",
        "Poniedziałek XX tygodnia zwykłego" to "20 Poniedziałek Okresu Zwykłego",
        "Sobota XX tygodnia zwykłego" to "20 Sobota Okresu Zwykłego",
        "Wtorek XX tygodnia zwykłego" to "20 Wtorek Okresu Zwykłego",
        "Środa XX tygodnia zwykłego" to "20 Środa Okresu Zwykłego",
        "Czwartek XXI tygodnia zwykłego" to "21 Czwartek Okresu Zwykłego",
        "XXI Niedziela Zwykła" to "21 Niedziela Okresu Zwykłego",
        "Piątek XXI tygodnia zwykłego" to "21 Piątek Okresu Zwykłego",
        "Poniedziałek XXI tygodnia zwykłego" to "21 Poniedziałek Okresu Zwykłego",
        "Sobota XXI tygodnia zwykłego" to "21 Sobota Okresu Zwykłego",
        "Wtorek XXI tygodnia zwykłego" to "21 Wtorek Okresu Zwykłego",
        "Środa XXI tygodnia zwykłego" to "21 Środa Okresu Zwykłego",
        "Czwartek XXII tygodnia zwykłego" to "22 Czwartek Okresu Zwykłego",
        "XXII Niedziela Zwykła" to "22 Niedziela Okresu Zwykłego",
        "Piątek XXII tygodnia zwykłego" to "22 Piątek Okresu Zwykłego",
        "Poniedziałek XXII tygodnia zwykłego" to "22 Poniedziałek Okresu Zwykłego",
        "Sobota XXII tygodnia zwykłego" to "22 Sobota Okresu Zwykłego",
        "Wtorek XXII tygodnia zwykłego" to "22 Wtorek Okresu Zwykłego",
        "Środa XXII tygodnia zwykłego" to "22 Środa Okresu Zwykłego",
        "Czwartek XXIII tygodnia zwykłego" to "23 Czwartek Okresu Zwykłego",
        "XXIII Niedziela Zwykła" to "23 Niedziela Okresu Zwykłego",
        "Piątek XXIII tygodnia zwykłego" to "23 Piątek Okresu Zwykłego",
        "Poniedziałek XXIII tygodnia zwykłego" to "23 Poniedziałek Okresu Zwykłego",
        "Sobota XXIII tygodnia zwykłego" to "23 Sobota Okresu Zwykłego",
        "Wtorek XXIII tygodnia zwykłego" to "23 Wtorek Okresu Zwykłego",
        "Środa XXIII tygodnia zwykłego" to "23 Środa Okresu Zwykłego",
        "Czwartek XXIV tygodnia zwykłego" to "24 Czwartek Okresu Zwykłego",
        "XXIV Niedziela Zwykła" to "24 Niedziela Okresu Zwykłego",
        "Piątek XXIV tygodnia zwykłego" to "24 Piątek Okresu Zwykłego",
        "Poniedziałek XXIV tygodnia zwykłego" to "24 Poniedziałek Okresu Zwykłego",
        "Sobota XXIV tygodnia zwykłego" to "24 Sobota Okresu Zwykłego",
        "Wtorek XXIV tygodnia zwykłego" to "24 Wtorek Okresu Zwykłego",
        "Środa XXIV tygodnia zwykłego" to "24 Środa Okresu Zwykłego",
        "Czwartek XXV tygodnia zwykłego" to "25 Czwartek Okresu Zwykłego",
        "XXV Niedziela Zwykła" to "25 Niedziela Okresu Zwykłego",
        "Piątek XXV tygodnia zwykłego" to "25 Piątek Okresu Zwykłego",
        "Poniedziałek XXV tygodnia zwykłego" to "25 Poniedziałek Okresu Zwykłego",
        "Sobota XXV tygodnia zwykłego" to "25 Sobota Okresu Zwykłego",
        "Wtorek XXV tygodnia zwykłego" to "25 Wtorek Okresu Zwykłego",
        "Środa XXV tygodnia zwykłego" to "25 Środa Okresu Zwykłego",
        "Czwartek XXVI tygodnia zwykłego" to "26 Czwartek Okresu Zwykłego",
        "XXVI Niedziela Zwykła" to "26 Niedziela Okresu Zwykłego",
        "Piątek XXVI tygodnia zwykłego" to "26 Piątek Okresu Zwykłego",
        "Poniedziałek XXVI tygodnia zwykłego" to "26 Poniedziałek Okresu Zwykłego",
        "Sobota XXVI tygodnia zwykłego" to "26 Sobota Okresu Zwykłego",
        "Wtorek XXVI tygodnia zwykłego" to "26 Wtorek Okresu Zwykłego",
        "Środa XXVI tygodnia zwykłego" to "26 Środa Okresu Zwykłego",
        "Czwartek XXVII tygodnia zwykłego" to "27 Czwartek Okresu Zwykłego",
        "XXVII Niedziela Zwykła" to "27 Niedziela Okresu Zwykłego",
        "Piątek XXVII tygodnia zwykłego" to "27 Piątek Okresu Zwykłego",
        "Poniedziałek XXVII tygodnia zwykłego" to "27 Poniedziałek Okresu Zwykłego",
        "Sobota XXVII tygodnia zwykłego" to "27 Sobota Okresu Zwykłego",
        "Wtorek XXVII tygodnia zwykłego" to "27 Wtorek Okresu Zwykłego",
        "Środa XXVII tygodnia zwykłego" to "27 Środa Okresu Zwykłego",
        "Czwartek XXVIII tygodnia zwykłego" to "28 Czwartek Okresu Zwykłego",
        "XXVIII Niedziela Zwykła" to "28 Niedziela Okresu Zwykłego",
        "Piątek XXVIII tygodnia zwykłego" to "28 Piątek Okresu Zwykłego",
        "Poniedziałek XXVIII tygodnia zwykłego" to "28 Poniedziałek Okresu Zwykłego",
        "Sobota XXVIII tygodnia zwykłego" to "28 Sobota Okresu Zwykłego",
        "Wtorek XXVIII tygodnia zwykłego" to "28 Wtorek Okresu Zwykłego",
        "Środa XXVIII tygodnia zwykłego" to "28 Środa Okresu Zwykłego",
        "Czwartek XXIX tygodnia zwykłego" to "29 Czwartek Okresu Zwykłego",
        "XXIX Niedziela Zwykła" to "29 Niedziela Okresu Zwykłego",
        "Piątek XXIX tygodnia zwykłego" to "29 Piątek Okresu Zwykłego",
        "Poniedziałek XXIX tygodnia zwykłego" to "29 Poniedziałek Okresu Zwykłego",
        "Sobota XXIX tygodnia zwykłego" to "29 Sobota Okresu Zwykłego",
        "Wtorek XXIX tygodnia zwykłego" to "29 Wtorek Okresu Zwykłego",
        "Środa XXIX tygodnia zwykłego" to "29 Środa Okresu Zwykłego",
        "Czwartek III tygodnia zwykłego" to "3 Czwartek Okresu Zwykłego",
        "III Niedziela Zwykła" to "3 Niedziela Okresu Zwykłego",
        "Piątek III tygodnia zwykłego" to "3 Piątek Okresu Zwykłego",
        "Poniedziałek III tygodnia zwykłego" to "3 Poniedziałek Okresu Zwykłego",
        "Sobota III tygodnia zwykłego" to "3 Sobota Okresu Zwykłego",
        "Wtorek III tygodnia zwykłego" to "3 Wtorek Okresu Zwykłego",
        "Środa III tygodnia zwykłego" to "3 Środa Okresu Zwykłego",
        "Czwartek XXX tygodnia zwykłego" to "30 Czwartek Okresu Zwykłego",
        "XXX Niedziela Zwykła" to "30 Niedziela Okresu Zwykłego",
        "Piątek XXX tygodnia zwykłego" to "30 Piątek Okresu Zwykłego",
        "Poniedziałek XXX tygodnia zwykłego" to "30 Poniedziałek Okresu Zwykłego",
        "Sobota XXX tygodnia zwykłego" to "30 Sobota Okresu Zwykłego",
        "Wtorek XXX tygodnia zwykłego" to "30 Wtorek Okresu Zwykłego",
        "Środa XXX tygodnia zwykłego" to "30 Środa Okresu Zwykłego",
        "Czwartek XXXI tygodnia zwykłego" to "31 Czwartek Okresu Zwykłego",
        "XXXI Niedziela Zwykła" to "31 Niedziela Okresu Zwykłego",
        "Piątek XXXI tygodnia zwykłego" to "31 Piątek Okresu Zwykłego",
        "Poniedziałek XXXI tygodnia zwykłego" to "31 Poniedziałek Okresu Zwykłego",
        "Sobota XXXI tygodnia zwykłego" to "31 Sobota Okresu Zwykłego",
        "Wtorek XXXI tygodnia zwykłego" to "31 Wtorek Okresu Zwykłego",
        "Środa XXXI tygodnia zwykłego" to "31 Środa Okresu Zwykłego",
        "Czwartek XXXII tygodnia zwykłego" to "32 Czwartek Okresu Zwykłego",
        "XXXII Niedziela Zwykła" to "32 Niedziela Okresu Zwykłego",
        "Piątek XXXII tygodnia zwykłego" to "32 Piątek Okresu Zwykłego",
        "Poniedziałek XXXII tygodnia zwykłego" to "32 Poniedziałek Okresu Zwykłego",
        "Sobota XXXII tygodnia zwykłego" to "32 Sobota Okresu Zwykłego",
        "Wtorek XXXII tygodnia zwykłego" to "32 Wtorek Okresu Zwykłego",
        "Środa XXXII tygodnia zwykłego" to "32 Środa Okresu Zwykłego",
        "Czwartek XXXIII tygodnia zwykłego" to "33 Czwartek Okresu Zwykłego",
        "XXXIII Niedziela Zwykła" to "33 Niedziela Okresu Zwykłego",
        "Piątek XXXIII tygodnia zwykłego" to "33 Piątek Okresu Zwykłego",
        "Poniedziałek XXXIII tygodnia zwykłego" to "33 Poniedziałek Okresu Zwykłego",
        "Sobota XXXIII tygodnia zwykłego" to "33 Sobota Okresu Zwykłego",
        "Wtorek XXXIII tygodnia zwykłego" to "33 Wtorek Okresu Zwykłego",
        "Środa XXXIII tygodnia zwykłego" to "33 Środa Okresu Zwykłego",
        "Czwartek XXXIV tygodnia zwykłego" to "34 Czwartek Okresu Zwykłego",
        "Piątek XXXIV tygodnia zwykłego" to "34 Piątek Okresu Zwykłego",
        "Poniedziałek XXXIV tygodnia zwykłego" to "34 Poniedziałek Okresu Zwykłego",
        "Sobota XXXIV tygodnia zwykłego" to "34 Sobota Okresu Zwykłego",
        "Wtorek XXXIV tygodnia zwykłego" to "34 Wtorek Okresu Zwykłego",
        "Środa XXXIV tygodnia zwykłego" to "34 Środa Okresu Zwykłego",
        "Jezusa Chrystusa, Króla Wszechświata" to "Uroczystość Jezusa Chrystusa, Króla Wszechświata",
        "Czwartek IV tygodnia zwykłego" to "4 Czwartek Okresu Zwykłego",
        "IV Niedziela Zwykła" to "4 Niedziela Okresu Zwykłego",
        "Piątek IV tygodnia zwykłego" to "4 Piątek Okresu Zwykłego",
        "Poniedziałek IV tygodnia zwykłego" to "4 Poniedziałek Okresu Zwykłego",
        "Sobota IV tygodnia zwykłego" to "4 Sobota Okresu Zwykłego",
        "Wtorek IV tygodnia zwykłego" to "4 Wtorek Okresu Zwykłego",
        "Środa IV tygodnia zwykłego" to "4 Środa Okresu Zwykłego",
        "Czwartek V tygodnia zwykłego" to "5 Czwartek Okresu Zwykłego",
        "V Niedziela Zwykła" to "5 Niedziela Okresu Zwykłego",
        "Piątek V tygodnia zwykłego" to "5 Piątek Okresu Zwykłego",
        "Poniedziałek V tygodnia zwykłego" to "5 Poniedziałek Okresu Zwykłego",
        "Sobota V tygodnia zwykłego" to "5 Sobota Okresu Zwykłego",
        "Wtorek V tygodnia zwykłego" to "5 Wtorek Okresu Zwykłego",
        "Środa V tygodnia zwykłego" to "5 Środa Okresu Zwykłego",
        "Czwartek VI tygodnia zwykłego" to "6 Czwartek Okresu Zwykłego",
        "VI Niedziela Zwykła" to "6 Niedziela Okresu Zwykłego",
        "Piątek VI tygodnia zwykłego" to "6 Piątek Okresu Zwykłego",
        "Poniedziałek VI tygodnia zwykłego" to "6 Poniedziałek Okresu Zwykłego",
        "Sobota VI tygodnia zwykłego" to "6 Sobota Okresu Zwykłego",
        "Wtorek VI tygodnia zwykłego" to "6 Wtorek Okresu Zwykłego",
        "Środa VI tygodnia zwykłego" to "6 Środa Okresu Zwykłego",
        "Czwartek VII tygodnia zwykłego" to "7 Czwartek Okresu Zwykłego",
        "VII Niedziela Zwykła" to "7 Niedziela Okresu Zwykłego",
        "Piątek VII tygodnia zwykłego" to "7 Piątek Okresu Zwykłego",
        "Poniedziałek VII tygodnia zwykłego" to "7 Poniedziałek Okresu Zwykłego",
        "Sobota VII tygodnia zwykłego" to "7 Sobota Okresu Zwykłego",
        "Wtorek VII tygodnia zwykłego" to "7 Wtorek Okresu Zwykłego",
        "Środa VII tygodnia zwykłego" to "7 Środa Okresu Zwykłego",
        "Czwartek VIII tygodnia zwykłego" to "8 Czwartek Okresu Zwykłego",
        "VIII Niedziela Zwykła" to "8 Niedziela Okresu Zwykłego",
        "Piątek VIII tygodnia zwykłego" to "8 Piątek Okresu Zwykłego",
        "Poniedziałek VIII tygodnia zwykłego" to "8 Poniedziałek Okresu Zwykłego",
        "Sobota VIII tygodnia zwykłego" to "8 Sobota Okresu Zwykłego",
        "Wtorek VIII tygodnia zwykłego" to "8 Wtorek Okresu Zwykłego",
        "Środa VIII tygodnia zwykłego" to "8 Środa Okresu Zwykłego",
        "Czwartek IX tygodnia zwykłego" to "9 Czwartek Okresu Zwykłego",
        "IX Niedziela Zwykła" to "9 Niedziela Okresu Zwykłego",
        "Piątek IX tygodnia zwykłego" to "9 Piątek Okresu Zwykłego",
        "Poniedziałek IX tygodnia zwykłego" to "9 Poniedziałek Okresu Zwykłego",
        "Sobota IX tygodnia zwykłego" to "9 Sobota Okresu Zwykłego",
        "Wtorek IX tygodnia zwykłego" to "9 Wtorek Okresu Zwykłego",
        "Środa IX tygodnia zwykłego" to "9 Środa Okresu Zwykłego",
        "Wielki Czwartek: Wieczerzy Pańskiej" to "Wielki Czwartek",
        "Wielki Piątek: Męki Pańskiej" to "Wielki Piątek Męki Pańskiej",
        "Wielka Sobota" to "Wigilia Paschalna",
        "Czwartek I tygodnia Wielkiego Postu" to "1 Czwartek Wielkiego Postu",
        "Piątek I tygodnia Wielkiego Postu" to "1 Piątek Wielkiego Postu",
        "Poniedziałek I tygodnia Wielkiego Postu" to "1 Poniedziałek Wielkiego Postu",
        "Sobota I tygodnia Wielkiego Postu" to "1 Sobota Wielkiego Postu",
        "Wtorek I tygodnia Wielkiego Postu" to "1 Wtorek Wielkiego Postu",
        "Środa I tygodnia Wielkiego Postu" to "1 Środa Wielkiego Postu",
        "I Niedziela Wielkiego Postu" to "1 Niedziela Wielkiego Postu",
        "Czwartek II tygodnia Wielkiego Postu" to "2 Czwartek Wielkiego Postu",
        "Piątek II tygodnia Wielkiego Postu" to "2 Piątek Wielkiego Postu",
        "Poniedziałek II tygodnia Wielkiego Postu" to "2 Poniedziałek Wielkiego Postu",
        "Sobota II tygodnia Wielkiego Postu" to "2 Sobota Wielkiego Postu",
        "Wtorek II tygodnia Wielkiego Postu" to "2 Wtorek Wielkiego Postu",
        "Środa II tygodnia Wielkiego Postu" to "2 Środa Wielkiego Postu",
        "II Niedziela Wielkiego Postu" to "2 Niedziela Wielkiego Postu",
        "Czwartek III tygodnia Wielkiego Postu" to "3 Czwartek Wielkiego Postu",
        "Piątek III tygodnia Wielkiego Postu" to "3 Piątek Wielkiego Postu",
        "Poniedziałek III tygodnia Wielkiego Postu" to "3 Poniedziałek Wielkiego Postu",
        "Sobota III tygodnia Wielkiego Postu" to "3 Sobota Wielkiego Postu",
        "Wtorek III tygodnia Wielkiego Postu" to "3 Wtorek Wielkiego Postu",
        "Środa III tygodnia Wielkiego Postu" to "3 Środa Wielkiego Postu",
        "III Niedziela Wielkiego Postu" to "3 Niedziela Wielkiego Postu",
        "Środa IV tygodnia Wielkiego Postu" to "4 Środa Wielkiego Postu",
        "Piątek IV tygodnia Wielkiego Postu" to "4 Piątek Wielkiego Postu",
        "Poniedziałek IV tygodnia Wielkiego Postu" to "4 Poniedziałek Okresu Zwykłego",
        "Sobota IV tygodnia Wielkiego Postu" to "4 Sobota Wielkiego Postu",
        "Wtorek IV tygodnia Wielkiego Postu" to "4 Wtorek Wielkiego Postu",
        "IV Niedziela Wielkiego Postu „Laetare”" to "4 Niedziela Wielkiego Postu",
        "Czwartek V tygodnia Wielkiego Postu" to "5 Czwartek Wielkiego Postu",
        "Piątek V tygodnia Wielkiego Postu" to "5 Piątek Wielkiego Postu",
        "Poniedziałek V tygodnia Wielkiego Postu" to "5 Poniedziałek Okresu Zwykłego",
        "Sobota V tygodnia Wielkiego Postu" to "5 Sobota Wielkiego Postu",
        "Wtorek V tygodnia Wielkiego Postu" to "5 Wtorek Wielkiego Postu",
        "Środa V tygodnia Wielkiego Postu" to "5 Środa Wielkiego Postu",
        "V Niedziela Wielkiego Postu" to "5 Niedziela Wielkiego Postu",
        "Wielka Środa" to "Wielka Środa",
        "Wielki Poniedziałek" to "Wielki Poniedziałek",
        "Wielki Wtorek" to "Wielki Wtorek",
        "Niedziela Palmowa Męki Pańskiej" to "Niedziela Palmowa Męki Pańskiej",
        "Czwartek po Popielcu" to "Czwartek po Popielcu",
        "Piątek po Popielcu" to "Piątek po Popielcu",
        "Sobota po Popielcu" to "Sobota po Popielcu",
        "Środa Popielcowa" to "Środa Popielcowa",
        "Niepokalanego Serca Najświętszej Maryi Panny" to "Wspomnienie Niepokalanego Serca NMP",
        "Najświętszej Maryi Panny, Matki Kościoła" to "Wspomnienie NMP Matki Kościoła",
        "Najświętszego Ciała i Krwi Chrystusa" to "Uroczystość Najświętszego Ciała i Krwi Chrystusa",
        "Najświętszego Serca Pana Jezusa" to "Uroczystość Najświętszego Serca Pana Jezusa",
        "Najświętszej Trójcy" to "Uroczystość Najświętszej Trójcy",
        "Niedziela Zesłania Ducha Świętego" to "Uroczystość Zesłania Ducha Świętego",
        "Jezusa Chrystusa, Najwyższego i Wiecznego Kapłana" to "Święto Jezusa Chrystusa, Najwyższego i Wiecznego Kapłana"
    )




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

    private fun parseColor(summary: String): String = when {
        "⚪" in summary -> "Biały"; "🔴" in summary -> "Czerwony"; "🟢" in summary -> "Zielony"
        "🟣" in summary -> "Fioletowy"; "💗" in summary || "🩷" in summary -> "Różowy"
        else -> "Nieznany"
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