package com.qjproject.liturgicalcalendar.ui.screens.calendar

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.time.Month
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarScreen(
    onDayClick: (CalendarDay) -> Unit
) {
    val context = LocalContext.current
    val viewModel: CalendarViewModel = viewModel(factory = CalendarViewModelFactory(context))
    val uiState by viewModel.uiState.collectAsState()

    // --- POCZĄTEK POPRAWKI: Powrót do prostego centrowania z modyfikatorem offset ---
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center // Najpierw centrujemy cały blok
    ) {
        Column(
            modifier = Modifier
                .wrapContentHeight()
                .offset(y = (-50).dp), // Następnie przesuwamy go w górę o 50dp
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MonthYearSelector(
                yearMonth = uiState.selectedMonth,
                yearList = uiState.yearList,
                isPreviousMonthEnabled = uiState.isPreviousMonthEnabled,
                isNextMonthEnabled = uiState.isNextMonthEnabled,
                onYearSelected = { viewModel.setYear(it) },
                onMonthSelected = { viewModel.setMonth(it) },
                onPreviousMonth = { viewModel.changeMonth(-1) },
                onNextMonth = { viewModel.changeMonth(1) }
            )
            Spacer(modifier = Modifier.height(8.dp))
            DaysOfWeekHeader()
            Divider()
            CalendarGrid(
                days = uiState.daysInMonth,
                onDayClick = onDayClick
            )
        }
    }
    // --- KONIEC POPRAWKI ---
}

@Composable
private fun MonthYearSelector(
    yearMonth: YearMonth,
    yearList: List<Int>,
    isPreviousMonthEnabled: Boolean,
    isNextMonthEnabled: Boolean,
    onYearSelected: (Int) -> Unit,
    onMonthSelected: (Int) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    var isYearMenuExpanded by remember { mutableStateOf(false) }
    var isMonthMenuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        IconButton(onClick = onPreviousMonth, enabled = isPreviousMonthEnabled) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Poprzedni miesiąc")
        }
        Spacer(modifier = Modifier.weight(1f))
        Box {
            Text(
                text = yearMonth.year.toString(),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.clickable { isYearMenuExpanded = true }
            )
            DropdownMenu(
                expanded = isYearMenuExpanded,
                onDismissRequest = { isYearMenuExpanded = false }
            ) {
                yearList.forEach { year ->
                    DropdownMenuItem(
                        text = { Text(year.toString()) },
                        onClick = {
                            onYearSelected(year)
                            isYearMenuExpanded = false
                        }
                    )
                }
            }
        }
        Text(" - ", style = MaterialTheme.typography.titleMedium)
        Box {
            val monthInNominative = yearMonth.month.getDisplayName(TextStyle.FULL_STANDALONE, Locale("pl"))
            Text(
                text = monthInNominative,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.clickable { isMonthMenuExpanded = true }
            )
            DropdownMenu(
                expanded = isMonthMenuExpanded,
                onDismissRequest = { isMonthMenuExpanded = false }
            ) {
                Month.values().forEachIndexed { index, month ->
                    DropdownMenuItem(
                        text = { Text(month.getDisplayName(TextStyle.FULL_STANDALONE, Locale("pl"))) },
                        onClick = {
                            onMonthSelected(index)
                            isMonthMenuExpanded = false
                        }
                    )
                }
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = onNextMonth, enabled = isNextMonthEnabled) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Następny miesiąc")
        }
    }
}

@Composable
private fun DaysOfWeekHeader() {
    val daysOfWeek = listOf("Pn", "Wt", "Śr", "Cz", "Pt", "So", "Nd")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        daysOfWeek.forEach { day ->
            Text(text = day, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun CalendarGrid(days: List<CalendarDay?>, onDayClick: (CalendarDay) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = Modifier.padding(horizontal = 16.dp),
        userScrollEnabled = false
    ) {
        items(days) { day ->
            if (day != null) {
                DayCell(day = day, onClick = { onDayClick(day) })
            } else {
                Box(modifier = Modifier.aspectRatio(1f))
            }
        }
    }
}

@Composable
private fun DayCell(day: CalendarDay, onClick: () -> Unit) {
    val textColor = if (day.hasData) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    var cellModifier = Modifier
        .aspectRatio(1f)
        .padding(4.dp)

    if (day.isToday) {
        cellModifier = cellModifier.border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
    }
    if (day.hasData) {
        cellModifier = cellModifier.clickable(onClick = onClick)
    }
    Box(
        modifier = cellModifier,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.dayOfMonth.toString(),
            color = textColor,
            textAlign = TextAlign.Center
        )
    }
}