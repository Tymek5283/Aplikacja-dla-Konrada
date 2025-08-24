// Ścieżka: app/src/main/java/com/qjproject/liturgicalcalendar/data/DayData.kt
package com.qjproject.liturgicalcalendar.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- POCZĄTEK ZMIANY ---
// Usunięto klasę `Inserts`, która powodowała problemy z parsowaniem.
// Zastąpiono ją bardziej elastyczną i niezawodną mapą, która poprawnie
// obsługuje zarówno obecność, jak i brak danych o wstawkach w plikach JSON.
// --- KONIEC ZMIANY ---

@Serializable
data class DayData(
    @SerialName("url") val urlCzytania: String? = null,
    @SerialName("tytul_dnia") val tytulDnia: String,
    @SerialName("czy_datowany") val czyDatowany: Boolean,
    val czytania: List<Reading>,
    @SerialName("piesniSugerowane") val piesniSugerowane: List<SuggestedSong?>? = null,
    val wstawki: Map<String, String>? = null
)

@Serializable
data class Reading(
    val typ: String,
    val sigla: String? = "",
    val opis: String? = "",
    val tekst: String
)

@Serializable
data class SuggestedSong(
    val numer: String,
    val piesn: String,
    val opis: String,
    val moment: String
)