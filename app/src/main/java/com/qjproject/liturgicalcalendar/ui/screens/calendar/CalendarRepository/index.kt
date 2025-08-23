// Ścieżka: C:\Users\blzej\Desktop\Aplikacja dla studenta\Aplikacja-dla-Konrada\app\src\main\java\com\qjproject\liturgicalcalendar\ui\screens\calendar\CalendarRepository\index.kt
// Opis: Główny plik repozytorium, który integruje i zarządza wszystkimi operacjami, delegując zadania do wyspecjalizowanych managerów. Stanowi centralny, publiczny punkt dostępu do danych kalendarza.
package com.qjproject.liturgicalcalendar.ui.screens.calendar.CalendarRepository

import android.content.Context
import com.qjproject.liturgicalcalendar.ui.screens.calendar.CalendarRepository.logic.LiturgicalLogic
import com.qjproject.liturgicalcalendar.ui.screens.calendar.CalendarRepository.model.LiturgicalEventDetails
import com.qjproject.liturgicalcalendar.ui.screens.calendar.CalendarRepository.model.LiturgicalYear
import com.qjproject.liturgicalcalendar.ui.screens.calendar.CalendarRepository.model.LiturgicalYearInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.YearMonth

class CalendarRepository(private val context: Context) {
    private val fileManager = CalendarFileManager(context)
    private val networkManager = NetworkManager(context)
    private val icsParser = IcsParser()
    private val liturgicalLogic = LiturgicalLogic()

    val eventComparator = liturgicalLogic.eventComparator

    suspend fun getLiturgicalYear(year: Int): LiturgicalYear? = fileManager.getLiturgicalYear(year)
    fun getAvailableYears(): List<Int> = fileManager.getAvailableYears()
    fun isYearAvailable(year: Int): Boolean = fileManager.isYearAvailable(year)

    suspend fun downloadAndSaveYearIfNeeded(year: Int): Result<Unit> = withContext(Dispatchers.IO) {
        if (fileManager.isYearAvailable(year)) return@withContext Result.success(Unit)

        return@withContext networkManager.downloadIcsData(year).fold(
            onSuccess = { icsContent ->
                val parsedEvents = icsParser.parseIcsToEvents(icsContent)
                fileManager.saveYearData(year, parsedEvents)
            },
            onFailure = { Result.failure(it) }
        )
    }

    fun getDominantEvent(events: List<LiturgicalEventDetails>): LiturgicalEventDetails? {
        return liturgicalLogic.getDominantEvent(events)
    }

    fun getLiturgicalYearInfoForMonth(yearMonth: YearMonth, yearData: LiturgicalYear): LiturgicalYearInfo {
        return liturgicalLogic.getLiturgicalYearInfoForMonth(yearMonth, yearData)
    }

    fun deleteAllCalendarFiles() {
        fileManager.deleteAllCalendarFiles()
    }
}