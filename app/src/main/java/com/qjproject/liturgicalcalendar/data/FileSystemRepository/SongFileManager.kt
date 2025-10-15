// Ścieżka: C:\Users\blzej\Desktop\Aplikacja dla studenta\Aplikacja-dla-Konrada\app\src\main\java\com\qjproject\liturgicalcalendar\data\repository\FileSystemRepository\SongFileManager.kt
// Opis: Koncentruje się na zarządzaniu plikiem `piesni.json`. Odpowiada za pobieranie, zapisywanie, wyszukiwanie oraz usuwanie pieśni.
package com.qjproject.liturgicalcalendar.data.repository.FileSystemRepository

import android.util.Log
import com.qjproject.liturgicalcalendar.data.Song
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File
import java.io.FileNotFoundException

internal class SongFileManager(
    private val context: android.content.Context,
    private val internalStorageRoot: File,
    private val json: Json,
    private val cacheManager: CacheManager,
    private val dayFileManager: DayFileManager
) {
    fun getSongList(): List<Song> {
        cacheManager.songListCache?.let { return it }
        return try {
            // Najpierw próbuj odczytać z pamięci wewnętrznej
            val internalFile = File(internalStorageRoot, "piesni.json")
            val jsonString = if (internalFile.exists()) {
                internalFile.bufferedReader().use { it.readText() }
            } else {
                // Jeśli nie ma w pamięci wewnętrznej, skopiuj z assets
                try {
                    val assetsContent = context.assets.open("piesni.json").bufferedReader().use { it.readText() }
                    // Zapisz do pamięci wewnętrznej dla przyszłych operacji
                    internalFile.writeText(assetsContent)
                    assetsContent
                } catch (e: Exception) {
                    Log.e("SongFileManager", "Nie można odczytać pliku piesni.json z assets", e)
                    return emptyList()
                }
            }
            // Odczytaj równolegle elementy JSON, aby wyciągnąć dynamiczne klucze "numer*"
            val root: JsonElement = try {
                json.parseToJsonElement(jsonString)
            } catch (e: Exception) {
                Log.e("SongFileManager", "Błąd parseToJsonElement piesni.json", e)
                null
            } ?: return emptyList()

            val (songs, numbers) = if (root is kotlinx.serialization.json.JsonArray) {
                val jsonObjects = root.jsonArray.map { it.jsonObject }
                val normalizedObjects = jsonObjects.map { normalizeSongJson(it) }
                val decoded = normalizedObjects.map { obj ->
                    try {
                        json.decodeFromJsonElement(Song.serializer(), obj)
                    } catch (e: Exception) {
                        Log.e("SongFileManager", "Błąd dekodowania elementu pieśni: $obj", e)
                        null
                    }
                }.filterNotNull()
                val dynMaps = jsonObjects.map { extractNumbers(it) }
                decoded to dynMaps
            } else emptyList<Song>() to emptyList<Map<String, String>>()

            val songsWithNumbers = songs.mapIndexed { index, song ->
                val dyn = numbers.getOrNull(index) ?: emptyMap()
                song.copy(numery = dyn)
            }

            cacheManager.setSongCache(songsWithNumbers)
            songsWithNumbers
        } catch (e: Exception) {
            Log.e("SongFileManager", "Błąd podczas wczytywania piesni.json", e)
            emptyList()
        }
    }

    fun saveSongList(songs: List<Song>): Result<Unit> {
        return try {
            val file = File(internalStorageRoot, "piesni.json")

            // Zapisz jako listę JsonObject, doklejając dynamiczne pola numer*
            val jsonObjects: List<JsonObject> = songs.map { song ->
                val base = json.encodeToJsonElement(Song.serializer(), song).jsonObject
                // Przenieś do nowego obiektu i dołóż dynamiczne klucze zaczynające się od "numer"
                val coreSuffixes = setOf("Siedl", "SAK", "DN", "SAK2020")
                buildJsonObject {
                    // Skopiuj pola bazowe
                    base.forEach { (k, v) -> put(k, v) }
                    // Dodaj dodatkowe numery, pomijając rdzeniowe aby nie dublować
                    song.numery.forEach { (suffix, value) ->
                        if (value.isNotBlank() && suffix !in coreSuffixes) {
                            put("numer$suffix", JsonPrimitive(value))
                        }
                    }
                }
            }
            file.writeText(json.encodeToString(ListSerializer(JsonObject.serializer()), jsonObjects))
            cacheManager.setSongCache(songs)
            Log.d("SongFileManager", "Zapisano pomyślnie listę pieśni.")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("SongFileManager", "Błąd podczas zapisywania listy pieśni", e)
            Result.failure(e)
        }
    }

    fun getSong(title: String, siedlNum: String?, sakNum: String?, dnNum: String?, sak2020Num: String? = null): Song? {
        if (title.isBlank()) return null
        val matchingSongs = getSongList().filter { it.tytul.equals(title, ignoreCase = true) }

        fun Song.anyNumberEquals(value: String): Boolean {
            if (value.isBlank()) return false
            // Sprawdź zarówno pola statyczne jak i dynamiczne
            return numerSiedl.equals(value, ignoreCase = true) ||
                    numerSAK.equals(value, ignoreCase = true) ||
                    numerDN.equals(value, ignoreCase = true) ||
                    numerSAK2020.equals(value, ignoreCase = true) ||
                    numery.values.any { it.equals(value, ignoreCase = true) }
        }

        return when {
            matchingSongs.isEmpty() -> null
            matchingSongs.size == 1 -> matchingSongs.first()
            else -> {
                val candidates = listOfNotNull(siedlNum, sakNum, dnNum, sak2020Num).filter { it.isNotBlank() }
                if (candidates.isEmpty()) return matchingSongs.first()
                matchingSongs.find { song -> candidates.any { candidate -> song.anyNumberEquals(candidate) } } ?: matchingSongs.first()
            }
        }
    }

    fun updateSongOccurrencesInDayFiles(originalSong: Song, updatedSong: Song): Result<Unit> {
        return try {
            dayFileManager.getAllDayFilePaths().forEach { path ->
                val dayData = dayFileManager.getDayData(path)
                if (dayData?.piesniSugerowane?.any { it?.numer == originalSong.numerSiedl && it.piesn == originalSong.tytul } == true) {
                    val updatedSuggestedSongs = dayData.piesniSugerowane.map {
                        if (it?.numer == originalSong.numerSiedl && it.piesn == originalSong.tytul)
                            it.copy(piesn = updatedSong.tytul, numer = updatedSong.numerSiedl)
                        else it
                    }
                    dayFileManager.saveDayData(path, dayData.copy(piesniSugerowane = updatedSuggestedSongs)).getOrThrow()
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("SongFileManager", "Błąd podczas aktualizacji wystąpień pieśni.", e)
            Result.failure(e)
        }
    }

    fun deleteSong(songToDelete: Song, deleteOccurrences: Boolean): Result<Unit> {
        return try {
            val currentSongs = getSongList().toMutableList()
            if (!currentSongs.removeAll { it.numerSiedl == songToDelete.numerSiedl }) {
                return Result.failure(FileNotFoundException("Nie znaleziono pieśni w głównym spisie."))
            }
            saveSongList(currentSongs).getOrThrow()

            if (deleteOccurrences) {
                dayFileManager.getAllDayFilePaths().forEach { path ->
                    val dayData = dayFileManager.getDayData(path)
                    if (dayData?.piesniSugerowane?.any { it?.numer == songToDelete.numerSiedl } == true) {
                        val updatedSongs = dayData.piesniSugerowane.filter { it?.numer != songToDelete.numerSiedl }
                        dayFileManager.saveDayData(path, dayData.copy(piesniSugerowane = updatedSongs)).getOrThrow()
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("SongFileManager", "Błąd podczas usuwania pieśni: ${songToDelete.tytul}", e)
            Result.failure(e)
        }
    }

    private fun extractNumbers(obj: JsonObject): Map<String, String> {
        // Zbierz wszystkie pola zaczynające się od "numer" i zwróć mapę: sufiks -> wartość
        val result = mutableMapOf<String, String>()
        obj.forEach { (key, value) ->
            if (key.startsWith("numer") && value is JsonPrimitive && value.isString) {
                val raw = value.content
                val suffix = key.removePrefix("numer")
                result[suffix] = raw
            }
        }
        return result
    }

    private fun normalizeSongJson(obj: JsonObject): JsonObject {
        // Normalizuje pole "tagi" do listy stringów: [] | ["..."]
        val tagElement = obj["tagi"]
        val normalizedTags = when (tagElement) {
            null -> kotlinx.serialization.json.JsonArray(emptyList())
            is kotlinx.serialization.json.JsonArray -> tagElement
            is JsonPrimitive -> {
                val content = if (tagElement.isString) tagElement.content else ""
                if (content.isBlank()) {
                    kotlinx.serialization.json.JsonArray(emptyList())
                } else {
                    kotlinx.serialization.json.JsonArray(listOf(JsonPrimitive(content)))
                }
            }
            else -> kotlinx.serialization.json.JsonArray(emptyList())
        }

        return buildJsonObject {
            obj.forEach { (k, v) ->
                if (k == "tagi") return@forEach
                put(k, v)
            }
            put("tagi", normalizedTags)
        }
    }
}