package com.qjproject.liturgicalcalendar.ui.screens.songcontent

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.qjproject.liturgicalcalendar.data.Category
import com.qjproject.liturgicalcalendar.data.repository.FileSystemRepository.FileSystemRepository
import com.qjproject.liturgicalcalendar.data.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.URLDecoder

data class SongContentUiState(
    val isLoading: Boolean = true,
    val song: Song? = null,
    val error: String? = null,
    val isEditMode: Boolean = false,
    val hasChanges: Boolean = false,
    val showConfirmExitDialog: Boolean = false,
    val allCategories: List<Category> = emptyList()
)

class SongContentViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: FileSystemRepository
) : ViewModel() {

    private val songTitle: String? = savedStateHandle.get<String>("songTitle")?.let { URLDecoder.decode(it, "UTF-8") }
    private val siedlNum: String? = savedStateHandle.get<String>("siedlNum")?.let { URLDecoder.decode(it, "UTF-8") }
    private val sakNum: String? = savedStateHandle.get<String>("sakNum")?.let { URLDecoder.decode(it, "UTF-8") }
    private val dnNum: String? = savedStateHandle.get<String>("dnNum")?.let { URLDecoder.decode(it, "UTF-8") }
    private val sak2020Num: String? = savedStateHandle.get<String>("sak2020Num")?.let { URLDecoder.decode(it, "UTF-8") }

    private val _uiState = MutableStateFlow(SongContentUiState())
    val uiState = _uiState.asStateFlow()

    var editableTitle = mutableStateOf("")
        private set
    var editableNumerSiedl = mutableStateOf("")
        private set
    var editableNumerSak = mutableStateOf("")
        private set
    var editableNumerSak2020 = mutableStateOf("")
        private set
    var editableNumerDn = mutableStateOf("")
        private set
    var editableCategory = mutableStateOf("")
        private set
    var editableText = mutableStateOf("")
        private set

    init {
        loadSong()
    }

    private fun loadSong() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            if (songTitle.isNullOrBlank()) {
                _uiState.update { it.copy(isLoading = false, error = "Brak tytułu pieśni.") }
                return@launch
            }
            val foundSong = repository.getSong(songTitle, siedlNum, sakNum, dnNum, sak2020Num)
            val categories = repository.getCategoryList()
            if (foundSong != null) {
                _uiState.update { it.copy(isLoading = false, song = foundSong, allCategories = categories) }
            } else {
                _uiState.update { it.copy(isLoading = false, error = "Nie znaleziono pieśni o tytule: $songTitle", allCategories = categories) }
            }
        }
    }

    fun onEnterEditMode() {
        _uiState.value.song?.let { song ->
            editableTitle.value = song.tytul
            editableNumerSiedl.value = song.numerSiedl
            editableNumerSak.value = song.numerSAK
            editableNumerDn.value = song.numerDN
            editableNumerSak2020.value = song.numerSAK2020
            editableCategory.value = song.kategoria
            editableText.value = song.tekst ?: ""
            _uiState.update { it.copy(isEditMode = true, hasChanges = false) }
        }
    }

    fun onTryExitEditMode() {
        if (_uiState.value.hasChanges) {
            _uiState.update { it.copy(showConfirmExitDialog = true) }
        } else {
            onDiscardChanges()
        }
    }

    fun onDiscardChanges() {
        _uiState.update { it.copy(isEditMode = false, hasChanges = false, showConfirmExitDialog = false) }
    }

    fun dismissConfirmExitDialog() {
        _uiState.update { it.copy(showConfirmExitDialog = false) }
    }


    fun onEditableFieldChange(
        title: String = editableTitle.value,
        siedl: String = editableNumerSiedl.value,
        sak: String = editableNumerSak.value,
        dn: String = editableNumerDn.value,
        sak2020: String = editableNumerSak2020.value,
        category: String = editableCategory.value,
        text: String = editableText.value
    ) {
        val originalSong = _uiState.value.song
        editableTitle.value = title
        editableNumerSiedl.value = siedl
        editableNumerSak.value = sak
        editableNumerDn.value = dn
        editableNumerSak2020.value = sak2020
        editableCategory.value = category
        editableText.value = text

        val changed = originalSong?.tytul != title ||
                originalSong.numerSiedl != siedl ||
                originalSong.numerSAK != sak ||
                originalSong.numerDN != dn ||
                originalSong.numerSAK2020 != sak2020 ||
                originalSong.kategoria != category ||
                originalSong.tekst != text

        _uiState.update { it.copy(hasChanges = changed) }
    }

    fun onSaveChanges() {
        val originalSong = _uiState.value.song ?: return
        viewModelScope.launch {
            val allSongs = repository.getSongList().toMutableList()
            val songIndex = allSongs.indexOfFirst { it.tytul == originalSong.tytul && it.numerSiedl == originalSong.numerSiedl }

            if (songIndex != -1) {
                val allCategories = repository.getCategoryList()
                val selectedCategory = allCategories.find { it.nazwa.equals(editableCategory.value, ignoreCase = true) }
                val newSkr = selectedCategory?.skrot ?: ""

                val updatedSong = originalSong.copy(
                    tytul = editableTitle.value.trim(),
                    tekst = editableText.value.trim(),
                    numerSiedl = editableNumerSiedl.value.trim(),
                    numerSAK = editableNumerSak.value.trim(),
                    numerDN = editableNumerDn.value.trim(),
                    numerSAK2020 = editableNumerSak2020.value.trim(),
                    kategoria = editableCategory.value,
                    kategoriaSkr = newSkr
                )
                allSongs[songIndex] = updatedSong
                repository.saveSongList(allSongs).onSuccess {
                    // Po zapisaniu głównej listy pieśni, zaktualizuj jej wystąpienia we wszystkich plikach dni
                    repository.updateSongOccurrencesInDayFiles(originalSong, updatedSong).onSuccess {
                        // Wszystko poszło dobrze, zaktualizuj UI
                        _uiState.update { it.copy(isEditMode = false, hasChanges = false, song = updatedSong) }
                    }.onFailure { error ->
                        // Główna lista została zapisana, ale wystąpienia nie. Poinformuj użytkownika, ale nadal odzwierciedlaj główną zmianę.
                        _uiState.update { it.copy(
                            isEditMode = false,
                            hasChanges = false,
                            song = updatedSong,
                            error = "Zapisano pieśń, ale błąd przy aktualizacji dni: ${error.localizedMessage}")
                        }
                    }
                }.onFailure { error ->
                    _uiState.update { it.copy(error = "Błąd zapisu: ${error.localizedMessage}") }
                }
            } else {
                _uiState.update { it.copy(error = "Nie można znaleźć pieśni do zaktualizowania.") }
            }
        }
    }
}

class SongContentViewModelFactory(
    private val context: Context,
    private val savedStateHandle: SavedStateHandle
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SongContentViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SongContentViewModel(savedStateHandle, FileSystemRepository(context.applicationContext)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}