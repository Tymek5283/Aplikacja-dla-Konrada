// Ścieżka: app/src/main/java/com/qjproject/liturgicalcalendar/ui/screens/daydetails/daydetailsscreen/index.kt
// Opis: Główny plik komponentu DayDetailsScreen. Pełni rolę "indexu", składając widok z mniejszych, wyspecjalizowanych komponentów. Zarządza nawigacją i stanem głównym ekranu.
package com.qjproject.liturgicalcalendar.ui.screens.daydetails.daydetailsscreen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.qjproject.liturgicalcalendar.data.Song
import com.qjproject.liturgicalcalendar.ui.screens.daydetails.daydetailsviewmodel.DayDetailsViewModel
import com.qjproject.liturgicalcalendar.ui.screens.daydetails.daydetailsviewmodel.DayDetailsViewModelFactory
import com.qjproject.liturgicalcalendar.ui.screens.daydetails.daydetailsviewmodel.DialogState

@Composable
fun DayDetailsScreen(
    dayId: String?,
    onNavigateBack: () -> Unit,
    onNavigateToSongContent: (song: Song, startInEdit: Boolean) -> Unit,
    onNavigateToSongDetails: (song: Song) -> Unit = { }
) {
    if (dayId.isNullOrBlank()) {
        Scaffold { padding -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text("Błąd krytyczny: Brak identyfikatora dnia.") } }
        return
    }

    val context = LocalContext.current
    val viewModel: DayDetailsViewModel = viewModel(factory = DayDetailsViewModelFactory(context, dayId))
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadDayData()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    BackHandler(enabled = uiState.isEditMode) { viewModel.onTryExitEditMode() }
    BackHandler(enabled = !uiState.isEditMode, onBack = onNavigateBack)

    if (uiState.showConfirmExitDialog) {
        ConfirmExitDialog(
            onDismiss = { viewModel.dismissConfirmExitDialog() },
            onDiscard = { viewModel.onExitEditMode(save = false) }
        )
    }

    when (val dialog = uiState.activeDialog) {
        is DialogState.ConfirmDelete -> ConfirmDeleteDialog(
            description = dialog.description,
            onDismiss = { viewModel.dismissDialog() },
            onConfirm = { viewModel.deleteItem(dialog.item) }
        )
        is DialogState.AddEditSong -> AddEditSongDialog(
            moment = dialog.moment,
            existingSong = dialog.existingSong,
            viewModel = viewModel,
            error = dialog.error,
            onDismiss = { viewModel.dismissDialog() },
            onConfirm = { title, siedl, sak, dn, opis, moment, originalSong ->
                viewModel.addOrUpdateSong(title, siedl, sak, dn, opis, moment, originalSong)
            }
        )
        is DialogState.None -> {}
    }

    Scaffold(
        topBar = {
            if (uiState.isEditMode) {
                EditModeTopAppBar(
                    title = uiState.dayData?.tytulDnia ?: "Edycja",
                    onCancelClick = { viewModel.onTryExitEditMode() },
                    onSaveClick = { viewModel.onExitEditMode(save = true) },
                    isSaveEnabled = uiState.hasChanges
                )
            } else {
                ViewModeTopAppBar(
                    title = uiState.dayData?.tytulDnia ?: "Ładowanie...",
                    onNavigateBack = onNavigateBack,
                    onEditClick = { viewModel.onEnterEditMode() }
                )
            }
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            uiState.error != null -> Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) { Text("Błąd: ${uiState.error}") }
            uiState.isEditMode -> DayDetailsEditModeContent(modifier = Modifier.padding(innerPadding), viewModel = viewModel)
            else -> DayDetailsViewModeContent(
                modifier = Modifier.padding(innerPadding),
                viewModel = viewModel,
                onNavigateToSongContent = onNavigateToSongContent,
                onNavigateToSongDetails = onNavigateToSongDetails
            )
        }
    }
}