// Ścieżka: app/src/main/java/com/qjproject/liturgicalcalendar/ui/screens/calendar/CalendarScreen/CalendarScreenComponents.kt
// Opis: Zawiera wszystkie mniejsze, pomocnicze komponenty Composable dla ekranu kalendarza, takie jak komórki dni, nagłówki, dialogi.

package com.qjproject.liturgicalcalendar.ui.screens.calendar.CalendarScreen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.qjproject.liturgicalcalendar.ui.screens.calendar.CalendarRepository.model.CalendarDay
import com.qjproject.liturgicalcalendar.ui.screens.calendar.CalendarRepository.model.LiturgicalEventDetails
import com.qjproject.liturgicalcalendar.ui.screens.calendar.CalendarViewModel.CalendarViewModel
import com.qjproject.liturgicalcalendar.ui.theme.Gold
import com.qjproject.liturgicalcalendar.ui.theme.SaturatedNavy
import com.qjproject.liturgicalcalendar.ui.theme.VeryDarkNavy
import java.time.Month
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

private fun mapColor(colorName: String?): Color? {
    return when (colorName) {
        "Biały" -> Color.White.copy(alpha = 0.5f)
        "Czerwony" -> Color(0xFFb00024).copy(alpha = 0.8f)
        "Zielony" -> Color.Green.copy(alpha = 0.3f)
        "Fioletowy" -> Color(0xFF8711cf).copy(alpha = 0.7f)
        "Różowy" -> Color(0xFFEF78A1).copy(alpha = 0.8f)
        else -> null
    }
}

@Composable
internal fun MissingDataScreen(
    onDownloadClick: () -> Unit,
    error: String?,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Brak danych kalendarza",
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Do poprawnego działania kalendarza wymagane jest pobranie danych. Upewnij się, że masz połączenie z internetem.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            AnimatedVisibility(visible = error != null) {
                error?.let {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = onDownloadClick, enabled = !isLoading) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Pobierz dane")
                }
            }
        }
    }
}

@Composable
internal fun LiturgicalYearInfoView(mainInfo: String) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = VeryDarkNavy),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 8.dp)
        ) {
            Text(
                text = mainInfo,
                style = MaterialTheme.typography.titleLarge,
                color = SaturatedNavy,
                fontSize = 24.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
internal fun MonthYearSelector(
    yearMonth: YearMonth,
    yearList: List<Int>,
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
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Poprzedni miesiąc")
        }
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                val monthInNominative =
                    yearMonth.month.getDisplayName(TextStyle.FULL_STANDALONE, Locale("pl"))
                Text(
                    text = monthInNominative,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.clickable { isMonthMenuExpanded = true }
                )
                DropdownMenu(
                    expanded = isMonthMenuExpanded,
                    onDismissRequest = { isMonthMenuExpanded = false }
                ) {
                    Month.entries.forEachIndexed { index, month ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    month.getDisplayName(
                                        TextStyle.FULL_STANDALONE,
                                        Locale("pl")
                                    )
                                )
                            },
                            onClick = {
                                onMonthSelected(index)
                                isMonthMenuExpanded = false
                            }
                        )
                    }
                }
            }
        }
        IconButton(onClick = onNextMonth) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Następny miesiąc")
        }
    }
}

@Composable
internal fun DaysOfWeekHeader() {
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
internal fun CalendarGrid(days: List<CalendarDay?>, onDayClick: (CalendarDay) -> Unit) {
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
internal fun DayCell(day: CalendarDay, onClick: () -> Unit) {
    val textColor = MaterialTheme.colorScheme.onSurface
    var cellModifier = Modifier
        .aspectRatio(1f)
        .padding(4.dp)
        .clip(CircleShape)

    mapColor(day.dominantEventColorName)?.let {
        cellModifier = cellModifier.background(it)
    }

    if (day.isToday) {
        cellModifier = cellModifier.border(2.dp, Gold, CircleShape)
    }
    if (day.hasEvents) {
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

@Composable
internal fun EventSelectionDialog(
    events: List<LiturgicalEventDetails>,
    viewModel: CalendarViewModel,
    onDismiss: () -> Unit,
    onEventSelected: (LiturgicalEventDetails) -> Unit
) {
    val sortedEvents = remember(events) {
        events.sortedWith(viewModel.calendarRepo.eventComparator)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = VeryDarkNavy)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Wybierz wydarzenie",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                    color = SaturatedNavy
                )
                Spacer(Modifier.height(16.dp))
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                Spacer(Modifier.height(16.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(sortedEvents, key = { it.name }) { event ->
                        Text(
                            text = event.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onEventSelected(event) }
                                .padding(vertical = 12.dp),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Anuluj")
                    }
                }
            }
        }
    }
}