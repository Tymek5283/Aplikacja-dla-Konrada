// Ścieżka: app/src/main/java/com/qjproject/liturgicalcalendar/ui/screens/daydetails/daydetailsviewmodel/DayDetailsViewModelFactory.kt
package com.qjproject.liturgicalcalendar.ui.screens.daydetails.daydetailsviewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.qjproject.liturgicalcalendar.data.repository.FileSystemRepository.FileSystemRepository

class DayDetailsViewModelFactory(
    private val context: Context,
    private val dayId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DayDetailsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DayDetailsViewModel(dayId, FileSystemRepository(context.applicationContext)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}