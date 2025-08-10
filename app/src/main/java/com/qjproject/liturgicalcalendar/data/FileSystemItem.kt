package com.qjproject.liturgicalcalendar.data

data class FileSystemItem(
    val name: String,
    val isDirectory: Boolean,
    val path: String // Względna ścieżka od katalogu głównego aplikacji (filesDir)
)