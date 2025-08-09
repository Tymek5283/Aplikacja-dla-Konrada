package com.qjproject.liturgicalcalendar

import android.app.Application
import com.qjproject.liturgicalcalendar.data.DataManager

class LiturgicalCalendarApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Inicjalizujemy i uruchamiamy proces kopiowania danych przy starcie aplikacji
        DataManager(this).copyAssetsToInternalStorageIfNeeded()
    }
}