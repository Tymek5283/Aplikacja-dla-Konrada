package com.qjproject.liturgicalcalendar.data

// Prosta klasa do reprezentowania pliku lub folderu
data class FileSystemItem(
    val name: String,
    val isDirectory: Boolean
)