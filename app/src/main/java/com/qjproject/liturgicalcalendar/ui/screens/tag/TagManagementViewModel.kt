package com.qjproject.liturgicalcalendar.ui.screens.tag

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.qjproject.liturgicalcalendar.data.repository.FileSystemRepository.FileSystemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class TagDialogState {
    object None : TagDialogState()
    data class AddEdit(val tag: String? = null) : TagDialogState()
    data class Delete(val tag: String) : TagDialogState()
    data class AssignSongs(val tag: String) : TagDialogState()
}

data class TagManagementUiState(
    val tags: List<String> = emptyList(),
    val isLoading: Boolean = true,
    val dialogState: TagDialogState = TagDialogState.None,
    val error: String? = null
)

class TagManagementViewModel(private val repository: FileSystemRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(TagManagementUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadTags()
    }

    private fun loadTags() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.invalidateTagCache() // Zawsze pobieraj świeżą listę
            val tags = repository.getTagList().sorted()
            _uiState.update { it.copy(isLoading = false, tags = tags) }
        }
    }

    fun showAddDialog() = _uiState.update { it.copy(dialogState = TagDialogState.AddEdit(null), error = null) }
    fun showEditDialog(tag: String) = _uiState.update { it.copy(dialogState = TagDialogState.AddEdit(tag), error = null) }
    fun showDeleteDialog(tag: String) = _uiState.update { it.copy(dialogState = TagDialogState.Delete(tag)) }
    fun showAssignSongsDialog(tag: String) = _uiState.update { it.copy(dialogState = TagDialogState.AssignSongs(tag)) }
    fun dismissDialog() = _uiState.update { it.copy(dialogState = TagDialogState.None, error = null) }

    fun validateTag(name: String, originalTag: String?) {
        val trimmedName = name.trim()
        val tags = _uiState.value.tags

        if (tags.any { it.equals(trimmedName, ignoreCase = true) && !it.equals(originalTag, ignoreCase = true) }) {
            _uiState.update { it.copy(error = "Tag o tej nazwie już istnieje.") }
            return
        }
        
        _uiState.update { it.copy(error = null) }
    }

    fun addTag(name: String) {
        viewModelScope.launch {
            val trimmedName = name.trim()
            repository.addTag(trimmedName).onSuccess {
                loadTags() // Przeładuj listę po dodaniu
                dismissDialog()
            }.onFailure { error ->
                _uiState.update { it.copy(error = error.message ?: "Błąd podczas dodawania tagu") }
            }
        }
    }

    fun updateTag(originalTag: String, newName: String) {
        viewModelScope.launch {
            val trimmedName = newName.trim()
            repository.updateTag(originalTag, trimmedName).onSuccess {
                loadTags() // Przeładuj listę po aktualizacji
                dismissDialog()
            }.onFailure { error ->
                _uiState.update { it.copy(error = error.message ?: "Błąd podczas aktualizacji tagu") }
            }
        }
    }

    fun deleteTag(tag: String) {
        viewModelScope.launch {
            repository.removeTag(tag).onSuccess {
                loadTags() // Przeładuj listę po usunięciu
                dismissDialog()
            }.onFailure { error ->
                _uiState.update { it.copy(error = error.message ?: "Błąd podczas usuwania tagu") }
            }
        }
    }
}

class TagManagementViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TagManagementViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TagManagementViewModel(FileSystemRepository(context.applicationContext)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
