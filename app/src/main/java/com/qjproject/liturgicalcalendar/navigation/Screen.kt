package com.qjproject.liturgicalcalendar.navigation // POPRAWIONA NAZWA PAKIETU

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.List
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Search : Screen("search", "Szukaj", Icons.Default.Search)
    object Browse : Screen("browse", "Przeglądaj", Icons.Default.List)
    object Calendar : Screen("calendar", "Kalendarz", Icons.Default.DateRange) // POPRAWIONA IKONA
    object Settings : Screen("settings", "Ustawienia", Icons.Default.Settings)

    // Ekran docelowy, który nie jest w dolnym pasku nawigacji
    object DayDetails : Screen("day_details/{dayId}", "Szczegóły Dnia", Icons.Default.Info) {
        fun createRoute(dayId: String) = "day_details/$dayId"
    }
}