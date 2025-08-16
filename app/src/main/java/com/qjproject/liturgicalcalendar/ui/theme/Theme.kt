package com.qjproject.liturgicalcalendar.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

private val CustomDarkColorScheme = darkColorScheme(
    primary = LightBlue,
    secondary = NavyLight,
    background = NavyDark,
    surface = DarkerNavy, // ZMIANA: Użycie nowego, bardziej niebieskiego koloru dla powierzchni (modali, kart)
    onPrimary = NavyDark,
    onSecondary = OffWhite,
    onBackground = OffWhite,
    onSurface = OffWhite
)

private val CustomLightColorScheme = lightColorScheme(
    primary = LightBlue,
    secondary = NavyLight,
    background = OffWhite,
    surface = Cream,
    onPrimary = NavyDark,
    onSecondary = OffWhite,
    onBackground = NavyDark,
    onSurface = NavyDark
)

@Composable
fun LiturgicalCalendarTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> CustomDarkColorScheme
        else -> CustomLightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Ustawienie przezroczystości dla obu pasków systemowych
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()

            // Umożliwia aplikacji rysowanie treści "od krawędzi do krawędzi" (edge-to-edge)
            WindowCompat.setDecorFitsSystemWindows(window, false)

            val insetsController = WindowCompat.getInsetsController(window, view)

            // --- POCZĄTEK ZMIANY ---
            // Ukryj paski systemowe (pasek statusu i nawigacji)
            insetsController.hide(WindowInsetsCompat.Type.systemBars())

            // Ustaw zachowanie, dzięki któremu paski pojawią się tymczasowo po przeciągnięciu palcem
            // od krawędzi ekranu, a następnie automatycznie znikną.
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            // --- KONIEC ZMIANY ---

            // Ustawienie wyglądu ikon na paskach systemowych (dla momentu, gdy są widoczne)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}