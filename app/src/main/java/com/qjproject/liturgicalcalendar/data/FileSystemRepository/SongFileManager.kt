// Ścieżka: C:\Users\blzej\Desktop\Aplikacja dla studenta\Aplikacja-dla-Konrada\app\src\main\java\com\qjproject\liturgicalcalendar\data\repository\FileSystemRepository\SongFileManager.kt
// Opis: Koncentruje się na zarządzaniu plikiem `piesni.json`. Odpowiada za pobieranie, zapisywanie, wyszukiwanie oraz usuwanie pieśni.
package com.qjproject.liturgicalcalendar.data.repository.FileSystemRepository

import android.util.Log
import com.qjproject.liturgicalcalendar.data.Song
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
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
            
            val songs = json.decodeFromString<List<Song>>(jsonString)
            cacheManager.setSongCache(songs)
            songs
        } catch (e: Exception) {
            Log.e("SongFileManager", "Błąd podczas wczytywania piesni.json", e)
            emptyList()
        }
    }

    fun saveSongList(songs: List<Song>): Result<Unit> {
        return try {
            val file = File(internalStorageRoot, "piesni.json")
            val jsonString = json.encodeToString(ListSerializer(Song.serializer()), songs)
            file.writeText(jsonString)
            cacheManager.setSongCache(songs)
            Log.d("SongFileManager", "Zapisano pomyślnie listę pieśni.")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("SongFileManager", "Błąd podczas zapisywania listy pieśni", e)
            Result.failure(e)
        }
    }

    fun getSong(title: String, siedlNum: String?, sakNum: String?, dnNum: String?): Song? {
        if (title.isBlank()) return null
        val matchingSongs = getSongList().filter { it.tytul.equals(title, ignoreCase = true) }

        return when {
            matchingSongs.isEmpty() -> null
            matchingSongs.size == 1 -> matchingSongs.first()
            else -> matchingSongs.find { song ->
                (siedlNum?.isNotBlank() == true && song.numerSiedl.equals(siedlNum, ignoreCase = true)) ||
                        (sakNum?.isNotBlank() == true && song.numerSAK.equals(sakNum, ignoreCase = true)) ||
                        (dnNum?.isNotBlank() == true && song.numerDN.equals(dnNum, ignoreCase = true))
            } ?: matchingSongs.first()
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
}