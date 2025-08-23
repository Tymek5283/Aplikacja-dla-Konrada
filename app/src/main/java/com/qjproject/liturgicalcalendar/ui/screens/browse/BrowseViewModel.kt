// Ścieżka: C:\Users\blzej\Desktop\Aplikacja dla studenta\Aplikacja-dla-Konrada\app\src\main\java\com\qjproject\liturgicalcalendar\ui\screens\browse\BrowseViewModel.kt
// Opis: ViewModel dla ekranu przeglądania plików. Odpowiada za logikę nawigacji po folderach, edycję, tworzenie i usuwanie elementów.
package com.qjproject.liturgicalcalendar.ui.screens.browse

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.qjproject.liturgicalcalendar.data.FileSystemItem
import com.qjproject.liturgicalcalendar.data.repository.FileSystemRepository.FileSystemRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.burnoutcrew.reorderable.ItemPosition
import java.util.Locale

sealed class BrowseDialogState {
    object None : BrowseDialogState()
    object AddOptions : BrowseDialogState()
    object CreateFolder : BrowseDialogState()
    object CreateDay : BrowseDialogState()
    data class ConfirmDelete(val item: FileSystemItem, val index: Int) : BrowseDialogState()
    data class RenameItem(val item: FileSystemItem, val index: Int) : BrowseDialogState()
}

data class BrowseUiState(
    val currentPath: List<String> = listOf("data"),
    val items: List<FileSystemItem> = emptyList(),
    val screenTitle: String = "Czego szukasz?",
    val isBackArrowVisible: Boolean = false,
    val isEditMode: Boolean = false,
    val showConfirmExitDialog: Boolean = false,
    val hasChanges: Boolean = false,
    val activeDialog: BrowseDialogState = BrowseDialogState.None,
    val operationError: String? = null
) {
    val isRoot: Boolean get() = currentPath.size <= 1
    val canReorder: Boolean get() = isEditMode && items.size > 1
}

class BrowseViewModel(private val repository: FileSystemRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(BrowseUiState())
    val uiState: StateFlow<BrowseUiState> = _uiState.asStateFlow()

    private var originalItemsOnEdit: List<FileSystemItem> = emptyList()

    init {
        loadContentForCurrentPath()
    }

    private fun validateNewItemName(name: String, isFolder: Boolean, originalName: String? = null) {
        val trimmedName = name.trim()
        val existingNames = _uiState.value.items.map { it.name.lowercase(Locale.getDefault()) }
        val finalName = trimmedName.lowercase(Locale.getDefault())

        if (finalName != originalName?.lowercase(Locale.getDefault()) && existingNames.contains(finalName)) {
            val itemType = if (isFolder) "Folder" else "Plik"
            _uiState.update { it.copy(operationError = "$itemType o tej nazwie już istnieje.") }
        } else {
            _uiState.update { it.copy(operationError = null) }
        }
    }

    fun onNewItemNameChange(name: String) {
        when (val dialog = _uiState.value.activeDialog) {
            is BrowseDialogState.CreateFolder -> validateNewItemName(name, isFolder = true)
            is BrowseDialogState.CreateDay -> validateNewItemName(name, isFolder = false)
            is BrowseDialogState.RenameItem -> validateNewItemName(name, isFolder = dialog.item.isDirectory, originalName = dialog.item.name)
            else -> {}
        }
    }

    fun onResetToRoot() {
        if (_uiState.value.isEditMode) onTryExitEditMode { _onResetToRoot() }
        else _onResetToRoot()
    }

    private fun _onResetToRoot() {
        _uiState.update { it.copy(currentPath = listOf("data")) }
        loadContentForCurrentPath()
    }

    fun onBackPress() {
        if (_uiState.value.isEditMode) onTryExitEditMode { _onBackPress() }
        else _onBackPress()
    }

    private fun _onBackPress() {
        if (!_uiState.value.isRoot) {
            _uiState.update { it.copy(currentPath = it.currentPath.dropLast(1)) }
            loadContentForCurrentPath()
        }
    }

    fun onDirectoryClick(directoryName: String) {
        val action = {
            _uiState.update { it.copy(currentPath = it.currentPath + directoryName) }
            loadContentForCurrentPath()
        }
        if (_uiState.value.isEditMode) onTryExitEditMode(onContinue = action)
        else action()
    }

    private fun loadContentForCurrentPath() {
        viewModelScope.launch {
            val pathString = _uiState.value.currentPath.joinToString("/")
            val items = repository.getItems(pathString).filter { it.name != "piesni" }
            _uiState.update {
                val title = if (it.isRoot) "Czego szukasz?" else it.currentPath.last().replace("_", " ")
                it.copy(
                    items = items,
                    screenTitle = title,
                    isBackArrowVisible = !it.isRoot,
                    hasChanges = false
                )
            }
        }
    }

    fun onEnterEditMode() {
        originalItemsOnEdit = _uiState.value.items
        _uiState.update { it.copy(isEditMode = true, hasChanges = false) }
    }

    fun onTryExitEditMode(onContinue: () -> Unit) {
        if (_uiState.value.hasChanges) {
            _uiState.update { it.copy(showConfirmExitDialog = true) }
        } else {
            exitEditMode(onContinue = onContinue)
        }
    }

    fun onSaveEditMode() {
        viewModelScope.launch {
            val currentItems = _uiState.value.items
            val originalItemsMap = originalItemsOnEdit.associateBy { it.path }
            val currentItemsMapByPath = currentItems.associateBy { it.path }

            val deletedItems = originalItemsOnEdit.filter { !currentItemsMapByPath.containsKey(it.path) }
            for (item in deletedItems) {
                repository.deleteItem(item.path)
            }

            val finalItems = currentItems.toMutableList()
            val itemsToUpdate = mutableMapOf<Int, FileSystemItem>()

            for ((index, item) in currentItems.withIndex()) {
                val originalItem = originalItemsMap[item.path]
                if (originalItem == null) {
                    val pathString = _uiState.value.currentPath.joinToString("/")
                    if (item.isDirectory) {
                        repository.createFolder(pathString, item.name)
                    } else {
                        repository.createDayFile(pathString, item.name, null)
                    }
                } else if (originalItem.name != item.name) {
                    repository.renameItem(item.path, item.name).onSuccess { newPath ->
                        itemsToUpdate[index] = item.copy(path = newPath)
                    }
                }
            }
            itemsToUpdate.forEach { (index, updatedItem) -> finalItems[index] = updatedItem }

            val finalOrderedNames = finalItems.map { it.name }
            repository.saveOrder(_uiState.value.currentPath.joinToString("/"), finalOrderedNames)

            exitEditMode()
        }
    }

    fun onCancelEditMode(isFromDialog: Boolean = false) {
        val onContinue = if (isFromDialog) {
            {}
        } else null
        _uiState.update { it.copy(items = originalItemsOnEdit) }
        exitEditMode(onContinue = onContinue)
    }

    private fun exitEditMode(onContinue: (() -> Unit)? = null) {
        originalItemsOnEdit = emptyList()
        _uiState.update { it.copy(isEditMode = false, hasChanges = false, showConfirmExitDialog = false) }
        loadContentForCurrentPath()
        onContinue?.invoke()
    }


    fun reorderItems(from: ItemPosition, to: ItemPosition) {
        _uiState.update { state ->
            val updatedList = state.items.toMutableList().apply { add(to.index, removeAt(from.index)) }
            state.copy(
                items = updatedList,
                hasChanges = updatedList != originalItemsOnEdit
            )
        }
    }

    fun showDialog(dialogState: BrowseDialogState) = _uiState.update { it.copy(activeDialog = dialogState, operationError = null) }
    fun dismissDialog() = _uiState.update { it.copy(activeDialog = BrowseDialogState.None, operationError = null) }
    fun dismissConfirmExitDialog() = _uiState.update { it.copy(showConfirmExitDialog = false) }

    fun deleteItem(index: Int) {
        if (index !in _uiState.value.items.indices) return
        val currentItems = _uiState.value.items.toMutableList()
        currentItems.removeAt(index)
        _uiState.update {
            it.copy(
                items = currentItems,
                hasChanges = currentItems != originalItemsOnEdit,
                activeDialog = BrowseDialogState.None
            )
        }
    }

    fun renameItem(index: Int, newName: String) {
        val trimmedName = newName.trim()
        if (trimmedName.isBlank()) {
            _uiState.update { it.copy(operationError = "Nazwa nie może być pusta.") }
            return
        }

        val currentItems = _uiState.value.items.toMutableList()
        if (index !in currentItems.indices) return

        val oldItem = currentItems[index]
        if (trimmedName == oldItem.name) {
            dismissDialog()
            return
        }
        val updatedItem = oldItem.copy(name = trimmedName)
        currentItems[index] = updatedItem

        _uiState.update {
            it.copy(
                items = currentItems,
                hasChanges = currentItems != originalItemsOnEdit,
                activeDialog = BrowseDialogState.None,
                operationError = null
            )
        }
    }

    fun createFolder(name: String) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            _uiState.update { it.copy(operationError = "Nazwa folderu nie może być pusta.") }
            return
        }
        val currentItems = _uiState.value.items.toMutableList()
        val tempPath = _uiState.value.currentPath.joinToString("/") + "/$trimmedName"
        val newItem = FileSystemItem(name = trimmedName, isDirectory = true, path = tempPath)
        currentItems.add(newItem)

        _uiState.update {
            it.copy(
                items = currentItems,
                hasChanges = currentItems != originalItemsOnEdit,
                activeDialog = BrowseDialogState.None,
                operationError = null
            )
        }
    }

    fun createDay(name: String, url: String?) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            _uiState.update { it.copy(operationError = "Nazwa dnia nie może być pusta.") }
            return
        }
        val currentItems = _uiState.value.items.toMutableList()
        val tempPath = _uiState.value.currentPath.joinToString("/") + "/$trimmedName.json"
        val newItem = FileSystemItem(name = trimmedName, isDirectory = false, path = tempPath)
        currentItems.add(newItem)

        _uiState.update {
            it.copy(
                items = currentItems,
                hasChanges = currentItems != originalItemsOnEdit,
                activeDialog = BrowseDialogState.None,
                operationError = null
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