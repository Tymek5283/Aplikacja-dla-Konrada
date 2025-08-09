package com.qjproject.liturgicalcalendar.ui.screens.daydetails

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.qjproject.liturgicalcalendar.data.DayData
import com.qjproject.liturgicalcalendar.data.FileSystemRepository
import com.qjproject.liturgicalcalendar.ui.components.AutoResizingText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailsScreen(
    dayId: String?,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { FileSystemRepository(context) }
    var dayData by remember { mutableStateOf<DayData?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(dayId) {
        isLoading = true
        dayId?.let {
            dayData = repository.getDayData(it)
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        AutoResizingText(
                            text = dayData?.tytulDnia ?: "Ładowanie...",
                            style = MaterialTheme.typography.titleLarge,
                            textAlign = TextAlign.Center
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Wróć")
                        }
                    },
                    actions = {
                        Spacer(Modifier.width(68.dp))
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                        navigationIconContentColor = MaterialTheme.colorScheme.primary
                    ),
                    windowInsets = WindowInsets(top = 0.dp, bottom = 0.dp)
                )
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator()
            } else if (dayData != null) {
                Text(text = "Treść dla: ${dayData!!.tytulDnia}")
            } else {
                Text(text = "Nie udało się załadować danych.")
            }
        }
    }
}