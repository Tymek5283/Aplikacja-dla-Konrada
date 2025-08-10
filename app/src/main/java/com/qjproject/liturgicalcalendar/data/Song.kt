package com.qjproject.liturgicalcalendar.data

import kotlinx.serialization.Serializable

@Serializable
data class Song(
    val tytul: String,
    val numer: String,
    val tekst: String? = null,
    val opis: String? = null // Zapewnia, że opis nie zostanie utracony podczas zapisu
)