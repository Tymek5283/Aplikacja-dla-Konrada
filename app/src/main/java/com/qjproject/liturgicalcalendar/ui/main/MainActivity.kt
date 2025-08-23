// Ścieżka: app/src/main/java/com/qjproject/liturgicalcalendar/ui/main/MainActivity.kt
// Opis: Główna aktywność aplikacji. Została przeniesiona do pakietu ui.main i przemianowana z index.kt, aby naprawić błąd uruchamiania i poprawić strukturę projektu.
package com.qjproject.liturgicalcalendar.ui.main

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.qjproject.liturgicalcalendar.ui.theme.LiturgicalCalendarTheme
import kotlin.system.exitProcess

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (!isGranted) {
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        setContent {
            LiturgicalCalendarTheme {
                MainAppHost()
            }
        }
    }
}

fun restartApp(activity: Activity) {
    val intent = Intent(activity, MainActivity::class.java)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    activity.startActivity(intent)
    activity.finish()
    exitProcess(0)
}