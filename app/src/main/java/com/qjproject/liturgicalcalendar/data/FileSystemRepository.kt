package com.qjproject.liturgicalcalendar.data

import android.content.Context
import kotlinx.serialization.json.Json
import java.io.IOException
import java.io.InputStream

class FileSystemRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }

    fun getItems(path: String): List<FileSystemItem> {
        return try {
            // AssetManager.list zwraca listę plików i folderów na danym poziomie
            context.assets.list(path)?.map { itemName ->
                // Sprawdzamy, czy dany element jest folderem, próbując wylistować jego zawartość.
                // Jeśli się uda i lista nie jest pusta, to jest to folder.
                // Pliki z rozszerzeniem (np. .json) traktujemy jako pliki.
                val isDirectory = try {
                    !itemName.contains(".") && (context.assets.list("$path/$itemName")?.isNotEmpty() == true)
                } catch (e: IOException) {
                    false
                }
                FileSystemItem(name = itemName.removeSuffix(".json"), isDirectory = isDirectory)
            } ?: emptyList()
        } catch (e: IOException) {
            emptyList()
        }
    }

    fun getDayData(path: String): DayData? {
        return try {
            val inputStream: InputStream = context.assets.open("$path.json")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            json.decodeFromString<DayData>(jsonString)
        } catch (e: Exception) {
            // Tutaj można dodać logowanie błędu
            e.printStackTrace()
            null
        }
    }
}