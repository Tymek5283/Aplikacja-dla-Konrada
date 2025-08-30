package com.qjproject.liturgicalcalendar.data

import kotlinx.serialization.Serializable

@Serializable
data class Song(
    val tytul: String,
    val tekst: String? = null,
    val numerSiedl: String,
    val numerSAK: String,
    val numerDN: String,
    val numerSAK2020: String = "",
    val kategoria: String,
    val kategoriaSkr: String,
    val tagi: List<String> = emptyList()
)