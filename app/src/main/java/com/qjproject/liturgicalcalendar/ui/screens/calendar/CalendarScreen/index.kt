// Ścieżka: app/src/main/java/com/qjproject/liturgicalcalendar/ui/screens/calendar/CalendarScreen/CalendarScreen.kt
// Opis: Główny plik komponentu CalendarScreen. Odpowiada za składanie interfejsu użytkownika kalendarza z mniejszych komponentów, obsługę nawigacji oraz wyświetlanie dialogów na podstawie stanu z ViewModelu.
package com.qjproject.liturgicalcalendar.ui.screens.calendar.CalendarScreen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.qjproject.liturgicalcalendar.ui.screens.calendar.CalendarRepository.model.LiturgicalEventDetails
import com.qjproject.liturgicalcalendar.ui.screens.calendar.CalendarViewModel.CalendarViewModel
import com.qjproject.liturgicalcalendar.ui.screens.calendar.CalendarViewModel.CalendarViewModelFactory
import com.qjproject.liturgicalcalendar.ui.screens.calendar.CalendarViewModel.model.NavigationAction

@Composable
fun CalendarScreen(
    onNavigateToDay: (String) -> Unit,
    onNavigateToDateEvents: (String, List<String>) -> Unit
) {
    val context = LocalContext.current
    val viewModel: CalendarViewModel = viewModel(factory = CalendarViewModelFactory(context))
    val uiState by viewModel.uiState.collectAsState()
    var showEventSelectionDialog by remember { mutableStateOf<List<LiturgicalEventDetails>?>(null) }

    if (showEventSelectionDialog != null) {
        EventSelectionDialog(
            events = showEventSelectionDialog!!,
            viewModel = viewModel,
            onDismiss = { showEventSelectionDialog = null },
            onEventSelected = { event ->
                viewModel.handleEventSelection(event) { navigationAction ->
                    showEventSelectionDialog = null
                    when (navigationAction) {
                        is NavigationAction.NavigateToDay -> onNavigateToDay(navigationAction.path)
                        is NavigationAction.ShowDateEvents -> {
                            onNavigateToDateEvents(navigationAction.title, navigationAction.paths)
                        }
                    }
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when {
            uiState.isLoading && uiState.isDataMissing -> CircularProgressIndicator()
            uiState.isDataMissing -> MissingDataScreen(
                onDownloadClick = { viewModel.forceRefreshData() },
                error = uiState.downloadError,
                isLoading = uiState.isLoading
            )
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                MonthYearSelector(
                    yearMonth = uiState.selectedMonth,
                    yearList = uiState.availableYears,
                    onYearSelected = { viewModel.setYear(it) },
                    onMonthSelected = { viewModel.setMonth(it) },
                    onPreviousMonth = { viewModel.changeMonth(-1) },
                    onNextMonth = { viewModel.changeMonth(1) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                DaysOfWeekHeader()
                CalendarGrid(
                    days = uiState.daysInMonth,
                    onDayClick = { day ->
                        if (day.events.isNotEmpty()) {
                            showEventSelectionDialog = day.events
                        }
                    }
                )
                Spacer(modifier = Modifier.height(40.dp))
                LiturgicalYearInfoView(
                    mainInfo = uiState.liturgicalYearInfo
                )
            }
        }
    }
}