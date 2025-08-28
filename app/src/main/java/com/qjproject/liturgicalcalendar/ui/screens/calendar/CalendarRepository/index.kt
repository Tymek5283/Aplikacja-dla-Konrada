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
import com.qjproject.liturgicalcalendar.ui.screens.calendar.CalendarRepository.model.LiturgicalYearDisplayInfo
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

        Log.i("CalendarRepository", "=== FAZA 1: POBIERANIE SUROWYCH DANYCH ===")
        Log.i("CalendarRepository", "Pobieranie brakujących lat: ${missingYears.joinToString()}")
        val failedYears = mutableListOf<Int>()
        val successfulYears = mutableListOf<Int>()

        // FAZA 1: Pobierz wszystkie brakujące lata (tylko surowe dane bez augmentacji)
        for (year in missingYears) {
            downloadAndSaveYearIfNeeded(year).fold(
                onSuccess = { 
                    successfulYears.add(year)
                    Log.d("CalendarRepository", "✓ Pobrano surowe dane dla roku $year")
                },
                onFailure = { 
                    failedYears.add(year)
                    Log.e("CalendarRepository", "✗ Nie udało się pobrać danych dla roku $year")
                }
            )
        }

        // FAZA 2: Wykonaj indywidualną augmentację dla każdego pomyślnie pobranego roku
        if (successfulYears.isNotEmpty()) {
            Log.i("CalendarRepository", "=== FAZA 2: AUGMENTACJA KAŻDEGO ROKU INDYWIDUALNIE ===")
            Log.i("CalendarRepository", "Rozpoczynanie augmentacji dla lat: ${successfulYears.joinToString()}")
            
            for (year in successfulYears) {
                try {
                    performIndividualAugmentation(year)
                    Log.i("CalendarRepository", "✓ Augmentacja zakończona dla roku $year")
                } catch (e: Exception) {
                    Log.e("CalendarRepository", "✗ Błąd augmentacji dla roku $year: ${e.message}")
                }
            }
        }

        return@withContext if (failedYears.isEmpty()) {
            Log.i("CalendarRepository", "=== PROCES ZAKOŃCZONY POMYŚLNIE ===")
            Log.i("CalendarRepository", "Wszystkie lata zostały pobrane i przetworzone: ${successfulYears.joinToString()}")
            Result.success(successfulYears)
        } else {
            Log.w("CalendarRepository", "=== PROCES ZAKOŃCZONY Z BŁĘDAMI ===")
            Log.w("CalendarRepository", "Pobrano: ${successfulYears.joinToString()}, błędy: ${failedYears.joinToString()}")
            Result.failure(Exception("Nie udało się pobrać danych dla lat: ${failedYears.joinToString()}"))
        }
    }

    fun areRequiredYearsAvailable(requiredYears: List<Int>): Boolean {
        return requiredYears.all { fileManager.isYearAvailable(it) }
    }

    fun getDominantEvent(events: List<LiturgicalEventDetails>): LiturgicalEventDetails? {
        return LiturgicalRules.getDominantEvent(events)
    }

    fun getLiturgicalYearInfo(yearData: LiturgicalYear?, date: LocalDate): LiturgicalYearDisplayInfo {
        if (yearData == null) {
            return LiturgicalYearDisplayInfo("Rok liturgiczny: Brak danych", null)
        }
        
        // Sprawdź cały miesiąc dla zmian roku liturgicznego
        val yearMonth = java.time.YearMonth.from(date)
        val firstDay = yearMonth.atDay(1)
        val lastDay = yearMonth.atEndOfMonth()
        
        // Pobierz informacje o roku liturgicznym dla pierwszego i ostatniego dnia miesiąca
        val firstDayEvent = yearData.eventsForDate(firstDay)?.let { getDominantEvent(it) }
        val lastDayEvent = yearData.eventsForDate(lastDay)?.let { getDominantEvent(it) }
        
        val firstLetter = firstDayEvent?.rok_litera
        val firstNumber = firstDayEvent?.rok_cyfra
        val lastLetter = lastDayEvent?.rok_litera
        val lastNumber = lastDayEvent?.rok_cyfra
        
        // Sprawdź czy wartości nie są 0-0 (nie wyświetlaj komunikatu dla 0-0)
        if ((firstLetter == "0" && firstNumber == "0") || 
            (firstLetter.isNullOrBlank() && firstNumber.isNullOrBlank())) {
            return LiturgicalYearDisplayInfo("", null)
        }
        
        // Jeśli rok liturgiczny się nie zmienia w ciągu miesiąca
        if (firstLetter == lastLetter && firstNumber == lastNumber) {
            return if (!firstLetter.isNullOrBlank() && !firstNumber.isNullOrBlank()) {
                LiturgicalYearDisplayInfo("Rok liturgiczny: $firstLetter, $firstNumber", null)
            } else {
                LiturgicalYearDisplayInfo("Rok liturgiczny: Brak danych", null)
            }
        }
        
        // Jeśli rok liturgiczny się zmienia, znajdź dokładny dzień zmiany
        var changeDate: LocalDate? = null
        var currentDate = firstDay
        var previousLetter = firstLetter
        var previousNumber = firstNumber
        
        while (currentDate.isBefore(lastDay) || currentDate.isEqual(lastDay)) {
            val dayEvent = yearData.eventsForDate(currentDate)?.let { getDominantEvent(it) }
            val dayLetter = dayEvent?.rok_litera
            val dayNumber = dayEvent?.rok_cyfra
            
            if (dayLetter != previousLetter || dayNumber != previousNumber) {
                changeDate = currentDate
                break
            }
            
            currentDate = currentDate.plusDays(1)
        }
        
        return if (changeDate != null && !firstLetter.isNullOrBlank() && !firstNumber.isNullOrBlank()) {
            val dayBefore = changeDate.minusDays(1)
            val newYearEvent = yearData.eventsForDate(changeDate)?.let { getDominantEvent(it) }
            val newLetter = newYearEvent?.rok_litera
            val newNumber = newYearEvent?.rok_cyfra
            
            if (!newLetter.isNullOrBlank() && !newNumber.isNullOrBlank()) {
                // Główny napis dla nowego roku, mały dopisek dla starego
                val mainText = "Rok liturgiczny: $newLetter, $newNumber"
                val subText = "Do ${dayBefore.dayOfMonth}. obowiązuje: $firstLetter"
                LiturgicalYearDisplayInfo(mainText, subText)
            } else {
                LiturgicalYearDisplayInfo("Rok liturgiczny: $firstLetter, $firstNumber", null)
            }
        } else if (!firstLetter.isNullOrBlank() && !firstNumber.isNullOrBlank()) {
            LiturgicalYearDisplayInfo("Rok liturgiczny: $firstLetter, $firstNumber", null)
        } else {
            LiturgicalYearDisplayInfo("Rok liturgiczny: Brak danych", null)
        }
    }

    fun deleteAllCalendarFiles() {
        fileManager.deleteAllCalendarFiles()
    }

    /**
     * Wykonuje indywidualną augmentację dla konkretnego roku.
     * Ta funkcja jest wywoływana po pobraniu WSZYSTKICH potrzebnych lat,
     * co zapewnia dostęp do danych sąsiadujących lat podczas augmentacji.
     */
    private suspend fun performIndividualAugmentation(year: Int) = withContext(Dispatchers.IO) {
        try {
            Log.d("CalendarRepository", "Rozpoczynanie augmentacji dla roku $year")
            
            // Pobierz dane dla roku oraz sąsiadujących lat (potrzebne do augmentacji)
            val prevYearEventsDeferred = async { fileManager.getLiturgicalEvents(year - 1) }
            val currentYearEventsDeferred = async { fileManager.getLiturgicalEvents(year) }
            val nextYearEventsDeferred = async { fileManager.getLiturgicalEvents(year + 1) }

            val prevYearEvents = prevYearEventsDeferred.await()
            val currentYearEvents = currentYearEventsDeferred.await()
            val nextYearEvents = nextYearEventsDeferred.await()

            if (currentYearEvents == null) {
                Log.e("CalendarRepository", "Brak danych dla roku $year. Nie można wykonać augmentacji.")
                return@withContext
            }

            Log.d("CalendarRepository", "Załadowano wydarzenia dla augmentacji roku $year: ${prevYearEvents?.size ?: 0} (rok ${year-1}), ${currentYearEvents.size} (rok $year), ${nextYearEvents?.size ?: 0} (rok ${year+1})")

            // Połącz wszystkie wydarzenia z trzech lat
            val allEvents = (prevYearEvents.orEmpty() + currentYearEvents + nextYearEvents.orEmpty()).distinct()

            // Wykonaj augmentację
            val augmentedEvents = LiturgicalDaySupplement.augmentYearlyEvents(allEvents)

            // Zwróć tylko wydarzenia dla docelowego roku
            val finalEventsForYear = augmentedEvents.filter {
                LocalDate.parse(it.data, LiturgicalEventDetails.DATE_FORMATTER).year == year
            }

            val originalCount = currentYearEvents.size
            val augmentedCount = finalEventsForYear.size
            val addedCount = augmentedCount - originalCount

            // Zapisz uzupełnione dane z powrotem do pliku
            fileManager.saveYearData(year, finalEventsForYear)
            Log.i("CalendarRepository", "Augmentacja roku $year zakończona: $originalCount → $augmentedCount (+$addedCount dni powszednich)")

        } catch (e: Exception) {
            Log.e("CalendarRepository", "Błąd podczas augmentacji roku $year: ${e.message}", e)
            throw e
        }
    }
}