package com.qjproject.liturgicalcalendar.ui.screens.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.qjproject.liturgicalcalendar.data.repository.FileSystemRepository.FileSystemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val message: String? = null,
    val showRestartPrompt: Boolean = false,
    val showExportConfigDialog: Boolean = false,
    val showImportConfigDialog: Boolean = false,
    val importPreviewState: ImportPreviewState? = null,
    val pendingImportUri: Uri? = null
)

class SettingsViewModel(private val repository: FileSystemRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    fun showExportConfigDialog() {
        _uiState.update { it.copy(showExportConfigDialog = true) }
    }

    fun hideExportConfigDialog() {
        _uiState.update { it.copy(showExportConfigDialog = false) }
    }

    fun exportData(configuration: ExportConfiguration) {
        _uiState.update { it.copy(isExporting = true, message = null, showExportConfigDialog = false) }
        val result = repository.exportDataToZip(configuration)
        result.fold(
            onSuccess = { file ->
                _uiState.update { it.copy(isExporting = false, message = "Eksport udany! Zapisano jako ${file.name} w folderze Pobrane.") }
            },
            onFailure = { error ->
                _uiState.update { it.copy(isExporting = false, message = "Błąd eksportu: ${error.localizedMessage}") }
            }
        )
    }

    fun startImport(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(
                showImportConfigDialog = true,
                pendingImportUri = uri,
                importPreviewState = ImportPreviewState(
                    availableData = AvailableImportData(),
                    configuration = ImportConfiguration(),
                    isAnalyzing = true
                )
            ) }
            
            val analysisResult = repository.analyzeImportData(uri)
            analysisResult.fold(
                onSuccess = { availableData ->
                    val defaultConfig = ImportConfiguration(
                        includeDays = availableData.hasDays,
                        includeSongs = availableData.hasSongs,
                        includeCategories = availableData.hasCategories,
                        includeNeumy = availableData.hasNeumy,
                        includeYears = availableData.hasYears
                    )
                    _uiState.update { it.copy(
                        importPreviewState = ImportPreviewState(
                            availableData = availableData,
                            configuration = defaultConfig,
                            isAnalyzing = false
                        )
                    ) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(
                        importPreviewState = ImportPreviewState(
                            availableData = AvailableImportData(),
                            configuration = ImportConfiguration(),
                            isAnalyzing = false,
                            analysisError = "Błąd analizy: ${error.localizedMessage}"
                        )
                    ) }
                }
            )
        }
    }
    
    fun updateImportConfiguration(configuration: ImportConfiguration) {
        _uiState.value.importPreviewState?.let { currentState ->
            _uiState.update { it.copy(
                importPreviewState = currentState.copy(configuration = configuration)
            ) }
        }
    }
    
    fun confirmImport() {
        val configuration = _uiState.value.importPreviewState?.configuration ?: return
        val uri = _uiState.value.pendingImportUri ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(
                isImporting = true,
                message = null,
                showImportConfigDialog = false,
                importPreviewState = null,
                pendingImportUri = null
            ) }
            
            val result = repository.importDataFromZip(uri, configuration)
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
    
    fun hideImportConfigDialog() {
        _uiState.update { it.copy(
            showImportConfigDialog = false,
            importPreviewState = null,
            pendingImportUri = null
        ) }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    fun dismissRestartPrompt() {
        _uiState.update { it.copy(showRestartPrompt = false) }
    }
    
    // Zachowujemy starą metodę dla kompatybilności wstecznej
    @Deprecated("Użyj startImport() zamiast tego")
    fun importData(uri: Uri) {
        startImport(uri)
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