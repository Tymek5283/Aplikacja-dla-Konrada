// Ścieżka: C:\Users\blzej\Desktop\Aplikacja dla studenta\Aplikacja-dla-Konrada\app\src\main\java\com\qjproject\liturgicalcalendar\data\repository\FileSystemRepository\DayFileManager.kt
// Opis: Odpowiada za operacje związane z plikami dni liturgicznych, w tym ich odczyt, zapis, oraz pobieranie listy plików dla konkretnego miesiąca.
package com.qjproject.liturgicalcalendar.data.repository.FileSystemRepository

import android.util.Log
import com.qjproject.liturgicalcalendar.data.DayData
import kotlinx.serialization.json.Json
import java.io.File
import java.time.Month
import java.time.format.TextStyle
import java.util.Locale

internal class DayFileManager(
    private val internalStorageRoot: File,
    private val json: Json
) {
    fun getAllDayFilePaths(): List<String> {
        val paths = mutableListOf<String>()
        val dataDir = File(internalStorageRoot, "data")
        val datowaneDir = File(internalStorageRoot, "Datowane")

        fun findJsonFiles(directory: File) {
            directory.walkTopDown().forEach { file ->
                if (file.isFile && file.extension == "json" && file.name != "piesni.json" && file.name != "kategorie.json") {
                    paths.add(file.absolutePath.removePrefix(internalStorageRoot.absolutePath + "/"))
                }
            }
        }

        if (dataDir.exists() && dataDir.isDirectory) findJsonFiles(dataDir)
        if (datowaneDir.exists() && datowaneDir.isDirectory) findJsonFiles(datowaneDir)

        return paths
    }

    fun getDayData(path: String): DayData? {
        try {
            val file = File(internalStorageRoot, path)
            if (!file.exists()) {
                Log.e("DayFileManager", "Plik nie istnieje: ${file.absolutePath}")
                return null
            }
            val jsonString = file.bufferedReader().use { it.readText() }
            if (jsonString.isBlank()) {
                Log.w("DayFileManager", "Pusty plik JSON, pomijanie: $path")
                return null
            }
            return json.decodeFromString<DayData>(jsonString)
        } catch (e: Exception) {
            Log.w("DayFileManager", "Błąd podczas odczytu lub parsowania pliku: $path. Powód: ${e.message}")
            return null
        }
    }

    fun saveDayData(path: String, dayData: DayData): Result<Unit> {
        return try {
            val file = File(internalStorageRoot, path)
            file.parentFile?.mkdirs()
            val jsonString = json.encodeToString(DayData.serializer(), dayData)
            file.writeText(jsonString)
            Log.d("DayFileManager", "Zapisano pomyślnie dane do: ${file.absolutePath}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("DayFileManager", "Błąd podczas zapisywania danych do $path", e)
            Result.failure(e)
        }
    }

    fun getMonthlyFileMap(month: Month): Map<Int, List<String>> {
        val monthName = month.getDisplayName(TextStyle.FULL_STANDALONE, Locale("pl"))
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("pl")) else it.toString() }
        val monthDir = File(internalStorageRoot, "Datowane/$monthName")

        if (!monthDir.exists() || !monthDir.isDirectory) {
            Log.w("DayFileManager", "Folder dla miesiąca nie istnieje: ${monthDir.path}")
            return emptyMap()
        }

        val fileMap = mutableMapOf<Int, MutableList<String>>()
        monthDir.listFiles()?.forEach { file ->
            try {
                val dayNumber = file.nameWithoutExtension.substringBefore(" ").toIntOrNull()
                if (dayNumber != null) {
                    val relativePath = "Datowane/$monthName/${file.name}"
                    fileMap.getOrPut(dayNumber) { mutableListOf() }.add(relativePath)
                }
            } catch (e: Exception) {
                Log.w("DayFileManager", "Pominięto plik o nieprawidłowej nazwie: ${file.name}", e)
            }
        }
        return fileMap
    }
}