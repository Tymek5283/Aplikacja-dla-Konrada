package com.qjproject.liturgicalcalendar.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.qjproject.liturgicalcalendar.data.FileSystemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SettingsUiState(
    val isExporting: Boolean = false,
    val exportMessage: String? = null
)

class SettingsViewModel(private val repository: FileSystemRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    fun exportData() {
        _uiState.update { it.copy(isExporting = true, exportMessage = null) }
        // --- POCZĄTEK ZMIANY: Eksportujemy od korzenia 'assets' ---
        val result = repository.exportAssetsToZip("")
        // --- KONIEC ZMIANY ---
        result.fold(
            onSuccess = { file ->
                _uiState.update { it.copy(isExporting = false, exportMessage = "Eksport udany! Zapisano jako ${file.name} w folderze Pobrane.") }
            },
            onFailure = { error ->
                _uiState.update { it.copy(isExporting = false, exportMessage = "Błąd eksportu: ${error.localizedMessage}") }
            }
        )
    }

    fun clearMessage() {
        _uiState.update { it.copy(exportMessage = null) }
    }
}

class SettingsViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingsViewModel(FileSystemRepository(context.applicationContext)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}