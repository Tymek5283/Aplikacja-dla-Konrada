package com.qjproject.liturgicalcalendar.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// --- POCZĄTEK OSTATECZNEJ POPRAWKI: Usunięcie błędnej klasy 'Songs' i dostosowanie do płaskiej struktury JSON ---
@Serializable
data class DayData(
    @SerialName("url") val urlCzytania: String? = null,
    @SerialName("tytul_dnia") val tytulDnia: String,
    @SerialName("czy_datowany") val czyDatowany: Boolean,
    val czytania: List<Reading>,
    // Lista pieśni jest teraz bezpośrednio w DayData, tak jak 'czytania'
    @SerialName("piesniSugerowane") val piesniSugerowane: List<SuggestedSong?>? = null
)

@Serializable
data class Reading(
    val typ: String,
    val sigla: String? = null,
    val opis: String? = null,
    val tekst: String
)

// Usunięto zbędną klasę 'Songs'

@Serializable
data class SuggestedSong(
    val numer: String,
    val piesn: String,
    val opis: String,
    val moment: String
)
// --- KONIEC OSTATECZNEJ POPRAWKI ---