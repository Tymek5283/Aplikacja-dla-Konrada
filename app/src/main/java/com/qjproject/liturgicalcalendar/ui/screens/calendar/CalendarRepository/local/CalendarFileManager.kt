package com.qjproject.liturgicalcalendar.ui.screens.calendar.CalendarRepository.local

import android.content.Context
import android.util.Log
import com.qjproject.liturgicalcalendar.ui.screens.calendar.CalendarRepository.model.LiturgicalEventDetails
import com.qjproject.liturgicalcalendar.ui.screens.calendar.CalendarRepository.model.LiturgicalYear
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

internal class CalendarFileManager(private val context: Context) {
    private val calendarDir = File(context.filesDir, "calendar_data")
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    init {
        if (!calendarDir.exists()) {
            calendarDir.mkdirs()
        }
    }

    fun isYearAvailable(year: Int): Boolean = File(calendarDir, "$year.json").exists()

    fun getAvailableYears(): List<Int> {
        return calendarDir.listFiles { _, name -> name.endsWith(".json") }
            ?.mapNotNull { it.nameWithoutExtension.toIntOrNull() }
            ?.sorted() ?: emptyList()
    }

    suspend fun getLiturgicalEvents(year: Int): List<LiturgicalEventDetails>? {
        val file = File(calendarDir, "$year.json")
        if (!file.exists()) return null
        return try {
            val content = file.readText()
            json.decodeFromString(ListSerializer(LiturgicalEventDetails.serializer()), content)
        } catch (e: Exception) {
            Log.e("CalendarFileManager", "Error reading or parsing $year.json", e)
            null
        }
    }

    fun saveYearData(year: Int, events: List<LiturgicalEventDetails>): Result<Unit> {
        val file = File(calendarDir, "$year.json")
        return try {
            val content = json.encodeToString(ListSerializer(LiturgicalEventDetails.serializer()), events)
            file.writeText(content)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("CalendarFileManager", "Error saving $year.json", e)
            Result.failure(e)
        }
    }

    fun deleteAllCalendarFiles() {
        if (calendarDir.exists()) {
            calendarDir.deleteRecursively()
            calendarDir.mkdirs()
        }
    }
}