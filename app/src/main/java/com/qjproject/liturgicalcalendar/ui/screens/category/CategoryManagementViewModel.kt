package com.qjproject.liturgicalcalendar.ui.screens.category

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.qjproject.liturgicalcalendar.data.Category
import com.qjproject.liturgicalcalendar.data.repository.FileSystemRepository.FileSystemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class DialogState {
    object None : DialogState()
    data class AddEdit(val category: Category? = null) : DialogState()
    data class Delete(val category: Category) : DialogState()
}

data class CategoryManagementUiState(
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = true,
    val dialogState: DialogState = DialogState.None,
    val error: String? = null
)

class CategoryManagementViewModel(private val repository: FileSystemRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoryManagementUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            repository.invalidateCategoryCache() // Zawsze pobieraj świeżą listę
            val categories = repository.getCategoryList().sortedBy { it.nazwa }
            _uiState.update { it.copy(isLoading = false, categories = categories) }
        }
    }

    fun showAddDialog() = _uiState.update { it.copy(dialogState = DialogState.AddEdit(null), error = null) }
    fun showEditDialog(category: Category) = _uiState.update { it.copy(dialogState = DialogState.AddEdit(category), error = null) }
    fun showDeleteDialog(category: Category) = _uiState.update { it.copy(dialogState = DialogState.Delete(category)) }
    fun dismissDialog() = _uiState.update { it.copy(dialogState = DialogState.None, error = null) }

    fun validateCategory(name: String, abbreviation: String, originalCategory: Category?) {
        val trimmedName = name.trim()
        val trimmedAbbreviation = abbreviation.trim()
        val categories = _uiState.value.categories

        if (trimmedName.isNotBlank() && categories.any { it.nazwa.equals(trimmedName, ignoreCase = true) && it.nazwa != originalCategory?.nazwa }) {
            _uiState.update { it.copy(error = "Kategoria o tej nazwie już istnieje.") }
            return
        }
        if (trimmedAbbreviation.isNotBlank() && categories.any { it.skrot.equals(trimmedAbbreviation, ignoreCase = true) && it.skrot != originalCategory?.skrot }) {
            _uiState.update { it.copy(error = "Kategoria o tym skrócie już istnieje.") }
            return
        }
        _uiState.update { it.copy(error = null) }
    }

    fun addCategory(name: String, abbreviation: String) {
        viewModelScope.launch {
            val newCategory = Category(nazwa = name.trim(), skrot = abbreviation.trim())
            val updatedList = (_uiState.value.categories + newCategory).sortedBy { it.nazwa }
            repository.saveCategoryList(updatedList).onSuccess {
                _uiState.update { it.copy(categories = updatedList) }
                dismissDialog()
            }
        }
    }

    fun updateCategory(originalCategory: Category, newName: String, newAbbreviation: String) {
        viewModelScope.launch {
            val trimmedName = newName.trim()
            val trimmedAbbreviation = newAbbreviation.trim()
            val updatedCategory = Category(nazwa = trimmedName, skrot = trimmedAbbreviation)

            // Krok 1: Zaktualizuj wszystkie pieśni używające starej kategorii
            repository.updateCategoryInSongs(originalCategory, updatedCategory).onSuccess {
                // Krok 2: Zaktualizuj listę kategorii
                val updatedList = _uiState.value.categories.map {
                    if (it == originalCategory) updatedCategory else it
                }.sortedBy { it.nazwa }
                repository.saveCategoryList(updatedList).onSuccess {
                    _uiState.update { it.copy(categories = updatedList) }
                    dismissDialog()
                }
            }
        }
    }

    fun deleteCategory(category: Category, removeFromSongs: Boolean) {
        viewModelScope.launch {
            if (removeFromSongs) {
                repository.removeCategoryFromSongs(category).onSuccess {
                    deleteCategoryFromList(category)
                }
            } else {
                deleteCategoryFromList(category)
            }
        }
    }

    private fun deleteCategoryFromList(category: Category) {
        val updatedList = _uiState.value.categories.filter { it != category }.sortedBy { it.nazwa }
        repository.saveCategoryList(updatedList).onSuccess {
            _uiState.update { it.copy(categories = updatedList) }
            dismissDialog()
        }
    }
}

class CategoryManagementViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CategoryManagementViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CategoryManagementViewModel(FileSystemRepository(context.applicationContext)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}