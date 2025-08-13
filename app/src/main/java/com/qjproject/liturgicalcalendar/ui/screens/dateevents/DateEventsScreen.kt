package com.qjproject.liturgicalcalendar.ui.screens.dateevents

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.qjproject.liturgicalcalendar.ui.components.AutoResizingText
import java.io.File

data class EventItem(
    val fullPath: String,
    val displayName: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateEventsScreen(
    dateTitle: String,
    filePaths: List<String>,
    onNavigateToDay: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    val eventItems = filePaths.map { path ->
        val displayName = File(path).nameWithoutExtension
        EventItem(fullPath = path, displayName = displayName)
    }

    BackHandler(onBack = onNavigateBack)

    Scaffold(
        topBar = {
            Column {
                CenterAlignedTopAppBar(
                    title = {
                        AutoResizingText(
                            text = dateTitle,
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
                    )
                )
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(eventItems, key = { it.fullPath }) { item ->
                EventListItem(
                    item = item,
                    onClick = { onNavigateToDay(item.fullPath) }
                )
            }
        }
    }
}

@Composable
private fun EventListItem(
    item: EventItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Article,
                contentDescription = "Wydarzenie",
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = item.displayName.replace("_", " "),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}