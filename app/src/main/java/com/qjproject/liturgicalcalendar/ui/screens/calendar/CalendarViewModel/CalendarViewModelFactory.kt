// Ścieżka: app/src/main/java/com/qjproject/liturgicalcalendar/ui/screens/calendar/CalendarViewModel/CalendarViewModelFactory.kt
// Opis: Fabryka odpowiedzialna za tworzenie instancji CalendarViewModel. Zapewnia wstrzykiwanie zależności (repozytoriów) do ViewModelu, co jest kluczowe dla poprawnej architektury i testowalności.
package com.qjproject.liturgicalcalendar.ui.screens.calendar.CalendarViewModel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.qjproject.liturgicalcalendar.data.repository.FileSystemRepository.FileSystemRepository
import com.qjproject.liturgicalcalendar.ui.screens.calendar.CalendarRepository.CalendarRepository

class CalendarViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CalendarViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CalendarViewModel(
                CalendarRepository(context.applicationContext),
                FileSystemRepository(context.applicationContext)
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}