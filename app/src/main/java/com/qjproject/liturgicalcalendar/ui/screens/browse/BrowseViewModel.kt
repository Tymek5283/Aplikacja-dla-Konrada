package com.qjproject.liturgicalcalendar.ui.screens.browse

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.qjproject.liturgicalcalendar.data.FileSystemItem
import com.qjproject.liturgicalcalendar.data.FileSystemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class BrowseUiState(
    val currentPath: List<String> = listOf("data"),
    val items: List<FileSystemItem> = emptyList(),
    val screenTitle: String = "Czego szukasz?",
    val isBackArrowVisible: Boolean = false
)

class BrowseViewModel(private val repository: FileSystemRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(BrowseUiState())
    val uiState: StateFlow<BrowseUiState> = _uiState.asStateFlow()

    init {
        loadContentForCurrentPath()
    }

    fun onResetToRoot() {
        _uiState.update {
            it.copy(currentPath = listOf("data"))
        }
        loadContentForCurrentPath()
    }

    fun onBackPress() {
        if (_uiState.value.currentPath.size > 1) {
            _uiState.update {
                it.copy(currentPath = it.currentPath.dropLast(1))
            }
            loadContentForCurrentPath()
        }
    }

    fun onDirectoryClick(directoryName: String) {
        _uiState.update {
            it.copy(currentPath = it.currentPath + directoryName)
        }
        loadContentForCurrentPath()
    }

    private fun loadContentForCurrentPath() {
        val pathString = _uiState.value.currentPath.joinToString("/")
        val items = repository.getItems(pathString).filter { it.name != "piesni" }
        _uiState.update {
            val title = if (it.currentPath.size == 1) {
                "Czego szukasz?"
            } else {
                it.currentPath.lastOrNull()?.replace("_", " ") ?: "Czego szukasz?"
            }
            it.copy(
                items = items,
                screenTitle = title,
                isBackArrowVisible = it.currentPath.size > 1
            )
        }
    }
}

class BrowseViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BrowseViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BrowseViewModel(FileSystemRepository(context.applicationContext)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}