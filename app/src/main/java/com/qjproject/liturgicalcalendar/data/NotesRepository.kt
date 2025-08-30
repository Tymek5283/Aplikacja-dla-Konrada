// Ścieżka: app/src/main/java/com/qjproject/liturgicalcalendar/data/NotesRepository.kt
// Opis: Repository odpowiedzialne za zarządzanie notatkami, łączące warstwę danych z logiką biznesową
package com.qjproject.liturgicalcalendar.data

import android.content.Context
import com.qjproject.liturgicalcalendar.data.repository.FileSystemRepository.NotesFileManager
import kotlinx.serialization.json.Json
import java.io.File

class NotesRepository(context: Context) {
    private val internalStorageRoot = context.filesDir
    private val json = Json { 
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    
    private val notesFileManager = NotesFileManager(
        context = context,
        internalStorageRoot = internalStorageRoot,
        json = json
    )

    fun getAllNotes(): List<Note> {
        return notesFileManager.getAllNotes()
    }

    fun addNote(title: String, description: String): Result<Note> {
        val trimmedTitle = title.trim()
        if (trimmedTitle.isEmpty()) {
            return Result.failure(IllegalArgumentException("Tytuł notatki nie może być pusty"))
        }

        val currentTime = System.currentTimeMillis()
        val note = Note(
            id = java.util.UUID.randomUUID().toString(),
            title = trimmedTitle,
            description = description.trim(),
            content = "", // Nowe notatki zaczynają z pustą treścią
            createdAt = currentTime,
            modifiedAt = currentTime
        )

        return notesFileManager.saveNote(note).map { note }
    }

    fun updateNote(note: Note): Result<Unit> {
        val updatedNote = note.copy(modifiedAt = System.currentTimeMillis())
        return notesFileManager.saveNote(updatedNote)
    }

    fun deleteNote(noteId: String): Result<Unit> {
        return notesFileManager.deleteNote(noteId)
    }

    fun getNoteById(noteId: String): Note? {
        return notesFileManager.getNoteById(noteId)
    }
}
