package com.qjproject.liturgicalcalendar.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DayData(
    @SerialName("url") val urlCzytania: String? = null,
    @SerialName("tytul_dnia") val tytulDnia: String,
    @SerialName("czy_datowany") val czyDatowany: Boolean,
    val czytania: List<Reading>,
    @SerialName("piesniSugerowane") val piesniSugerowane: List<SuggestedSong?>? = null
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