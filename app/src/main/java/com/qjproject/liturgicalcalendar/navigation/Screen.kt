package com.qjproject.liturgicalcalendar.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.List
import androidx.compose.ui.graphics.vector.ImageVector
import java.net.URLDecoder
import java.net.URLEncoder

// --- POCZĄTEK ZMIANY: Dodajemy nową, generyczną klasę kodującą dla bezpieczeństwa URL ---
private object UrlEncoder {
    fun encode(input: String): String = URLEncoder.encode(input, "UTF-8")
    fun decode(input: String): String = URLDecoder.decode(input, "UTF-8")
}
// --- KONIEC ZMIANY ---

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Search : Screen("search", "Szukaj", Icons.Default.Search)
    object Browse : Screen("browse", "Przeglądaj", Icons.Default.List)
    object Calendar : Screen("calendar", "Kalendarz", Icons.Default.DateRange)
    object Settings : Screen("settings", "Ustawienia", Icons.Default.Settings)

    object DayDetails : Screen("day_details/{dayId}", "Szczegóły Dnia", Icons.Default.Info) {
        fun createRoute(dayId: String) = "day_details/${UrlEncoder.encode(dayId)}"
    }

    // --- POCZĄTEK ZMIANY: Definicja nowego ekranu dla wielu wydarzeń w danym dniu ---
    object DateEvents : Screen("date_events/{dateTitle}/{filePaths}", "Wydarzenia Dnia", Icons.Default.List) {
        private const val FILE_PATH_SEPARATOR = "|||" // Unikalny separator

        fun createRoute(dateTitle: String, filePaths: List<String>): String {
            val encodedTitle = UrlEncoder.encode(dateTitle)
            val encodedPaths = UrlEncoder.encode(filePaths.joinToString(FILE_PATH_SEPARATOR))
            return "date_events/$encodedTitle/$encodedPaths"
        }

        fun decodePaths(encodedPaths: String): List<String> {
            return UrlEncoder.decode(encodedPaths).split(FILE_PATH_SEPARATOR)
        }
    }
    // --- KONIEC ZMIANY ---
}