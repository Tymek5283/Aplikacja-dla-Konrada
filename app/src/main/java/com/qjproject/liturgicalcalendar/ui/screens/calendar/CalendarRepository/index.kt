// Ścieżka: app/src/main/java/com/qjproject/liturgicalcalendar/ui/screens/calendar/CalendarRepository/index.kt
// Opis: Repozytorium dla danych kalendarza. Odpowiada za pobieranie, zapisywanie, parsowanie i augmentację danych liturgicznych na dany rok. Koordynuje pracę menedżera plików i menedżera sieci.
package com.qjproject.liturgicalcalendar.ui.screens.calendar.CalendarRepository

import android.content.Context
import android.util.Log
import com.qjproject.liturgicalcalendar.logic.liturgical_day_supplement.LiturgicalDaySupplement
import com.qjproject.liturgicalcalendar.logic.liturgical_day_supplement.LiturgicalRules
import com.qjproject.liturgicalcalendar.ui.screens.calendar.CalendarRepository.local.CalendarFileManager
import com.qjproject.liturgicalcalendar.ui.screens.calendar.CalendarRepository.model.LiturgicalEventDetails
import com.qjproject.liturgicalcalendar.ui.screens.calendar.CalendarRepository.model.LiturgicalYear
import com.qjproject.liturgicalcalendar.ui.screens.calendar.CalendarRepository.model.translationMap
import com.qjproject.liturgicalcalendar.ui.screens.calendar.CalendarRepository.remote.IcsParser
import com.qjproject.liturgicalcalendar.ui.screens.calendar.CalendarRepository.remote.NetworkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import java.time.LocalDate

class CalendarRepository(context: Context) {
    private val fileManager = CalendarFileManager(context)
    private val networkManager = NetworkManager(context)
    private val icsParser = IcsParser()

    val eventComparator: Comparator<LiturgicalEventDetails> = compareBy<LiturgicalEventDetails>(
        { LiturgicalRules.rankHierarchy[it.typ]?.value ?: Int.MAX_VALUE },
        { !it.name.any { char -> char.isDigit() } },
        { it.name }
    )

    suspend fun getAugmentedLiturgicalYear(year: Int): LiturgicalYear? = withContext(Dispatchers.IO) {
        val prevYearEventsDeferred = async { fileManager.getLiturgicalEvents(year - 1) }
        val currentYearEventsDeferred = async { fileManager.getLiturgicalEvents(year) }
        val nextYearEventsDeferred = async { fileManager.getLiturgicalEvents(year + 1) }

        val prevYearEvents = prevYearEventsDeferred.await()
        val currentYearEvents = currentYearEventsDeferred.await()
        val nextYearEvents = nextYearEventsDeferred.await()

        if (currentYearEvents == null) {
            Log.e("CalendarRepository", "Brak danych dla bieżącego roku ($year). Nie można kontynuować augmentacji.")
            return@withContext null
        }
        if (prevYearEvents == null) {
            Log.w("CalendarRepository", "Brak danych dla poprzedniego roku (${year - 1}). Augmentacja może być niekompletna dla początku roku.")
        }

        Log.d("CalendarRepository", "Załadowano wydarzenia: ${prevYearEvents?.size ?: 0} (rok ${year-1}), ${currentYearEvents.size} (rok $year), ${nextYearEvents?.size ?: 0} (rok ${year+1})")

        val allEvents = (prevYearEvents.orEmpty() + currentYearEvents + nextYearEvents.orEmpty()).distinct()

        val augmentedEvents = LiturgicalDaySupplement.augmentYearlyEvents(allEvents)

        val finalEventsForYear = augmentedEvents.filter {
            LocalDate.parse(it.data, LiturgicalEventDetails.DATE_FORMATTER).year == year
        }

        val eventsMap = finalEventsForYear.associateBy { "${it.data}_${it.name}" }
        LiturgicalYear(eventsMap)
    }

    fun getAvailableYears(): List<Int> = fileManager.getAvailableYears()

    fun isYearAvailable(year: Int): Boolean = fileManager.isYearAvailable(year)

    suspend fun downloadAndSaveYearIfNeeded(year: Int): Result<Unit> = withContext(Dispatchers.IO) {
        if (fileManager.isYearAvailable(year)) {
            Log.d("CalendarRepository", "Dane dla roku $year są już dostępne lokalnie. Pomijanie pobierania.")
            return@withContext Result.success(Unit)
        }

        Log.i("CalendarRepository", "Rozpoczynanie pobierania danych dla roku $year.")
        return@withContext networkManager.downloadIcsData(year).fold(
            onSuccess = { icsContent ->
                val parsedEvents = icsParser.parseIcsToEvents(icsContent, translationMap)
                fileManager.saveYearData(year, parsedEvents)
            },
            onFailure = {
                Log.e("CalendarRepository", "Pobieranie danych dla roku $year nie powiodło się.", it)
                Result.failure(it)
            }
        )
    }

    suspend fun downloadMissingYearsOnly(requiredYears: List<Int>): Result<List<Int>> = withContext(Dispatchers.IO) {
        val missingYears = requiredYears.filter { !fileManager.isYearAvailable(it) }
        
        if (missingYears.isEmpty()) {
            Log.d("CalendarRepository", "Wszystkie wymagane lata (${requiredYears.joinToString()}) są już dostępne lokalnie.")
            return@withContext Result.success(emptyList())
        }

        Log.i("CalendarRepository", "Pobieranie brakujących lat: ${missingYears.joinToString()}")
        val failedYears = mutableListOf<Int>()

        for (year in missingYears) {
            downloadAndSaveYearIfNeeded(year).onFailure { 
                failedYears.add(year)
                Log.e("CalendarRepository", "Nie udało się pobrać danych dla roku $year")
            }
        }

        return@withContext if (failedYears.isEmpty()) {
            Result.success(missingYears)
        } else {
            Result.failure(Exception("Nie udało się pobrać danych dla lat: ${failedYears.joinToString()}"))
        }
    }

    fun areRequiredYearsAvailable(requiredYears: List<Int>): Boolean {
        return requiredYears.all { fileManager.isYearAvailable(it) }
    }

    fun getDominantEvent(events: List<LiturgicalEventDetails>): LiturgicalEventDetails? {
        return LiturgicalRules.getDominantEvent(events)
    }

    fun getLiturgicalYearInfo(yearData: LiturgicalYear?, date: LocalDate): String {
        val event = yearData?.eventsForDate(date)?.let { getDominantEvent(it) }
        val letter = event?.rok_litera
        val number = event?.rok_cyfra
        return if (!letter.isNullOrBlank() && !number.isNullOrBlank()) {
            "Rok liturgiczny: $letter, $number"
        } else {
            "Rok liturgiczny: Brak danych"
        }
    }

    fun deleteAllCalendarFiles() {
        fileManager.deleteAllCalendarFiles()
    }
}