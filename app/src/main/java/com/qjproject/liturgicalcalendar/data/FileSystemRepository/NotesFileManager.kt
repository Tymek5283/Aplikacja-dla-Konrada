// Ścieżka: app/src/main/java/com/qjproject/liturgicalcalendar/data/FileSystemRepository/NotesFileManager.kt
// Opis: Odpowiada za zarządzanie plikami notatek w folderze "notatki", w tym odczyt, zapis oraz operacje CRUD na notatkach.
package com.qjproject.liturgicalcalendar.data.repository.FileSystemRepository

import android.content.Context
import android.util.Log
import com.qjproject.liturgicalcalendar.data.Note
import kotlinx.serialization.json.Json
import java.io.File

internal class NotesFileManager(
    private val context: Context,
    private val internalStorageRoot: File,
    private val json: Json
) {
    private val notesFolder = File(internalStorageRoot, "notatki")

    init {
        // Upewnij się, że folder notatek istnieje
        if (!notesFolder.exists()) {
            notesFolder.mkdirs()
        }
    }

    fun getAllNotes(): List<Note> {
        return try {
            if (!notesFolder.exists()) {
                return emptyList()
            }

            notesFolder.listFiles { file -> file.extension == "json" }
                ?.mapNotNull { file ->
                    try {
                        val jsonString = file.readText()
                        json.decodeFromString<Note>(jsonString)
                    } catch (e: Exception) {
                        Log.e("NotesFileManager", "Błąd podczas odczytu notatki ${file.name}", e)
                        null
                    }
                }
                ?.sortedByDescending { it.modifiedAt } // Sortuj od najnowszych
                ?: emptyList()
        } catch (e: Exception) {
            Log.e("NotesFileManager", "Błąd podczas wczytywania notatek", e)
            emptyList()
        }
    }

    fun saveNote(note: Note): Result<Unit> {
        return try {
            val fileName = "${note.id}.json"
            val file = File(notesFolder, fileName)
            val jsonString = json.encodeToString(Note.serializer(), note)
            file.writeText(jsonString)
            Log.d("NotesFileManager", "Zapisano pomyślnie notatkę: ${note.title}")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("NotesFileManager", "Błąd podczas zapisywania notatki: ${note.title}", e)
            Result.failure(e)
        }
    }

    fun deleteNote(noteId: String): Result<Unit> {
        return try {
            val fileName = "$noteId.json"
            val file = File(notesFolder, fileName)
            if (file.exists()) {
                file.delete()
                Log.d("NotesFileManager", "Usunięto pomyślnie notatkę o ID: $noteId")
                Result.success(Unit)
            } else {
                Result.failure(IllegalArgumentException("Notatka o ID $noteId nie istnieje"))
            }
        } catch (e: Exception) {
            Log.e("NotesFileManager", "Błąd podczas usuwania notatki o ID: $noteId", e)
            Result.failure(e)
        }
    }

    fun getNoteById(noteId: String): Note? {
        return try {
            val fileName = "$noteId.json"
            val file = File(notesFolder, fileName)
            if (file.exists()) {
                val jsonString = file.readText()
                json.decodeFromString<Note>(jsonString)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("NotesFileManager", "Błąd podczas odczytu notatki o ID: $noteId", e)
            null
        }
    }
}
