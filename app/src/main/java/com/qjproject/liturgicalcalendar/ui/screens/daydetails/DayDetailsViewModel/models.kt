// Ścieżka: app/src/main/java/com/qjproject/liturgicalcalendar/ui/screens/daydetails/daydetailsviewmodel/models.kt
// Opis: Ten plik zawiera modele danych oraz klasy stanu (State) wykorzystywane przez DayDetailsViewModel. Definiuje strukturę danych dla interfejsu użytkownika, w tym stany dialogów i listy elementów.
package com.qjproject.liturgicalcalendar.ui.screens.daydetails.daydetailsviewmodel

import com.qjproject.liturgicalcalendar.data.DayData
import com.qjproject.liturgicalcalendar.data.SuggestedSong

sealed class DialogState {
    object None : DialogState()
    data class ConfirmDelete(val item: Any, val description: String) : DialogState()
    data class AddEditSong(
        val moment: String,
        val existingSong: SuggestedSong? = null,
        val error: String? = null
    ) : DialogState()
}

data class DayDetailsUiState(
    val isLoading: Boolean = true,
    val dayData: DayData? = null,
    val error: String? = null,
    val isEditMode: Boolean = false,
    val hasChanges: Boolean = false,
    val showConfirmExitDialog: Boolean = false,
    val activeDialog: DialogState = DialogState.None,
    val isReadingsSectionExpanded: Boolean = false,
    val isSongsSectionExpanded: Boolean = true,
    val isInsertsSectionExpanded: Boolean = false,
    val expandedReadings: Set<Int> = emptySet(),
    val expandedSongMoments: Set<String> = songMomentOrderMap.keys,
    val expandedInserts: Set<String> = emptySet()
)

sealed class ReorderableListItem {
    data class SongItem(val suggestedSong: SuggestedSong) : ReorderableListItem()
    data class HeaderItem(val momentKey: String, val momentName: String) : ReorderableListItem()
}

data class DisplayableSuggestedSong(
    val suggestedSong: SuggestedSong,
    val hasText: Boolean
)