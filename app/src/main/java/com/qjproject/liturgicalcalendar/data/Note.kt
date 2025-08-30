// Ścieżka: app/src/main/java/com/qjproject/liturgicalcalendar/data/Note.kt
// Opis: Model danych reprezentujący pojedynczą notatkę w aplikacji
package com.qjproject.liturgicalcalendar.data

import kotlinx.serialization.Serializable

@Serializable
data class Note(
    val id: String,
    val title: String,
    val description: String = "",
    val content: String = "", // Sformatowana treść notatki (HTML/Markdown)
    val createdAt: Long,
    val modifiedAt: Long
)
