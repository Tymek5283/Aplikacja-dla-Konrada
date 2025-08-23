// Ścieżka: app/src/main/java/com/qjproject/liturgicalcalendar/ui/screens/calendar/CalendarRepository/CalendarFileManager.kt
// Opis: Zarządza operacjami na plikach dla danych kalendarza liturgicznego, w tym zapisem, odczytem i weryfikacją dostępności danych dla poszczególnych lat.
package com.qjproject.liturgicalcalendar.ui.screens.calendar.CalendarRepository

import android.content.Context
import android.util.Log
import com.qjproject.liturgicalcalendar.ui.screens.calendar.CalendarRepository.model.LiturgicalEventDetails
import com.qjproject.liturgicalcalendar.ui.screens.calendar.CalendarRepository.model.LiturgicalYear
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

internal class CalendarFileManager(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
    private val calendarDataDir = File(context.filesDir, "calendar_data")

    init {
        if (!calendarDataDir.exists()) {
            calendarDataDir.mkdirs()
        }
    }

    fun saveYearData(year: Int, events: List<LiturgicalEventDetails>): Result<Unit> {
        return try {
            val file = File(calendarDataDir, "$year.json")
            val jsonString = json.encodeToString(ListSerializer(LiturgicalEventDetails.serializer()), events)
            file.writeText(jsonString)
            Log.d("CalendarFileManager", "Zapisano dane dla roku $year.")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("CalendarFileManager", "Błąd zapisu danych dla roku $year.", e)
            Result.failure(e)
        }
    }

    suspend fun getLiturgicalYear(year: Int): LiturgicalYear? {
        val file = File(calendarDataDir, "$year.json")
        if (!file.exists()) return null

        return try {
            val jsonString = file.readText()
            val events = json.decodeFromString<List<LiturgicalEventDetails>>(jsonString)
            val eventsMap = events.associateBy { "${it.data}-${it.name}" }
            LiturgicalYear(eventsMap)
        } catch (e: Exception) {
            Log.e("CalendarFileManager", "Błąd odczytu lub parsowania pliku dla roku $year.", e)
            null
        }
    }

    fun isYearAvailable(year: Int): Boolean {
        return File(calendarDataDir, "$year.json").exists()
    }

    fun getAvailableYears(): List<Int> {
        return calendarDataDir.listFiles { _, name -> name.endsWith(".json") }
            ?.mapNotNull { it.nameWithoutExtension.toIntOrNull() }
            ?.sorted()
            ?: emptyList()
    }

    fun deleteAllCalendarFiles() {
        if (calendarDataDir.exists()) {
            calendarDataDir.deleteRecursively()
            calendarDataDir.mkdirs()
            Log.d("CalendarFileManager", "Usunięto wszystkie pliki danych kalendarza.")
        }
    }
}