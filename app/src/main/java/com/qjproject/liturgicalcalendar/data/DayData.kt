package com.qjproject.liturgicalcalendar.data

import kotlinx.serialization.Serializable

@Serializable
data class DayData(
    val urlCzytania: String? = null,
    val urlPiesni: String? = null,
    val tytulDnia: String,
    val czytania: List<Reading>,
    val piesni: Songs
)

@Serializable
data class Reading(
    val typ: String,
    val sigle: String? = null, // Używamy sigle, bo sigla to forma mnoga
    val opis: String? = null,
    val tekst: String
)

@Serializable
data class Songs(
    val piesniStale: List<ConstantSong?>? = null,
    val piesniSugerowane: List<SuggestedSong?>? = null
)

@Serializable
data class ConstantSong(
    val moment: String,
    val piesn1: String? = null,
    val piesn2: String? = null,
    val piesn3: String? = null,
    val opis: String
)

@Serializable
data class SuggestedSong(
    val piesn: String,
    val numer: String,
    val opis: String
)