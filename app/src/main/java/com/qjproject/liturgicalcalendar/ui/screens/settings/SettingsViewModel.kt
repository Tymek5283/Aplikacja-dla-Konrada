package com.qjproject.liturgicalcalendar.ui.screens.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.qjproject.liturgicalcalendar.data.FileSystemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val message: String? = null,
    val showRestartPrompt: Boolean = false
)

class SettingsViewModel(private val repository: FileSystemRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    fun exportData() {
        _uiState.update { it.copy(isExporting = true, message = null) }
        val result = repository.exportDataToZip()
        result.fold(
            onSuccess = { file ->
                _uiState.update { it.copy(isExporting = false, message = "Eksport udany! Zapisano jako ${file.name} w folderze Pobrane.") }
            },
            onFailure = { error ->
                _uiState.update { it.copy(isExporting = false, message = "Błąd eksportu: ${error.localizedMessage}") }
            }
        )
    }

    fun importData(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, message = null) }
            val result = repository.importDataFromZip(uri)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isImporting = false, showRestartPrompt = true) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isImporting = false, message = "Błąd importu: ${error.localizedMessage}") }
                }
            )
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun dismissRestartPrompt() {
        _uiState.update { it.copy(showRestartPrompt = false) }
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