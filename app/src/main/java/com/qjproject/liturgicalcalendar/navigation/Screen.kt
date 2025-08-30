package com.qjproject.liturgicalcalendar.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import java.net.URLDecoder
import java.net.URLEncoder

private object UrlEncoder {
    fun encode(input: String): String = URLEncoder.encode(input, "UTF-8")
    fun decode(input: String): String = URLDecoder.decode(input, "UTF-8")
}

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Search : Screen("search", "Szukaj", Icons.Default.Search)
    object Browse : Screen("browse", "Przeglądaj", Icons.Default.List)
    object Calendar : Screen("calendar", "Kalendarz", Icons.Default.DateRange)
    object Settings : Screen("settings", "Ustawienia", Icons.Default.Settings)

    object DayDetails : Screen("day_details/{dayId}", "Szczegóły Dnia", Icons.Default.Info) {
        fun createRoute(dayId: String) = "day_details/${UrlEncoder.encode(dayId)}"
    }

    object DateEvents : Screen("date_events/{dateTitle}/{filePaths}", "Wydarzenia Dnia", Icons.Default.List) {
        private const val FILE_PATH_SEPARATOR = "|||"

        fun createRoute(dateTitle: String, filePaths: List<String>): String {
            val encodedTitle = UrlEncoder.encode(dateTitle)
            val encodedPaths = UrlEncoder.encode(filePaths.joinToString(FILE_PATH_SEPARATOR))
            return "date_events/$encodedTitle/$encodedPaths"
        }

        fun decodePaths(encodedPaths: String): List<String> {
            return UrlEncoder.decode(encodedPaths).split(FILE_PATH_SEPARATOR)
        }
    }
    object SongDetails : Screen("song_details/{songTitle}?siedlNum={siedlNum}&sakNum={sakNum}&dnNum={dnNum}&sak2020Num={sak2020Num}", "Szczegóły pieśni", Icons.Default.MenuBook) {
        fun createRoute(song: com.qjproject.liturgicalcalendar.data.Song): String {
            val title = UrlEncoder.encode(song.tytul)
            val siedl = UrlEncoder.encode(song.numerSiedl)
            val sak = UrlEncoder.encode(song.numerSAK)
            val dn = UrlEncoder.encode(song.numerDN)
            val sak2020 = UrlEncoder.encode(song.numerSAK2020)
            return "song_details/$title?siedlNum=$siedl&sakNum=$sak&dnNum=$dn&sak2020Num=$sak2020"
        }
    }

    object SongContent : Screen("song_content/{songTitle}?siedlNum={siedlNum}&sakNum={sakNum}&dnNum={dnNum}&sak2020Num={sak2020Num}&editOnStart={editOnStart}", "Treść pieśni", Icons.Default.MenuBook) {
        fun createRoute(song: com.qjproject.liturgicalcalendar.data.Song, editOnStart: Boolean = false): String {
            val title = UrlEncoder.encode(song.tytul)
            val siedl = UrlEncoder.encode(song.numerSiedl)
            val sak = UrlEncoder.encode(song.numerSAK)
            val dn = UrlEncoder.encode(song.numerDN)
            val sak2020 = UrlEncoder.encode(song.numerSAK2020)
            return "song_content/$title?siedlNum=$siedl&sakNum=$sak&dnNum=$dn&sak2020Num=$sak2020&editOnStart=$editOnStart"
        }
    }

    // --- POCZĄTEK ZMIANY ---
    object CategoryManagement : Screen("category_management", "Zarządzaj kategoriami", Icons.Default.Category)
    object TagManagement : Screen("tag_management", "Zarządzaj tagami", Icons.Default.Label)
    object Notes : Screen("notes", "Notatki", Icons.Default.Notes)
    object NoteDetails : Screen("note_details/{noteId}", "Szczegóły notatki", Icons.Default.Notes) {
        fun createRoute(noteId: String) = "note_details/${UrlEncoder.encode(noteId)}"
    }
    // --- KONIEC ZMIANY ---
}