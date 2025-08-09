package com.qjproject.liturgicalcalendar.ui.screens.browse

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Article
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.qjproject.liturgicalcalendar.data.FileSystemItem
import com.qjproject.liturgicalcalendar.ui.components.AutoResizingText

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BrowseScreen(
    viewModel: BrowseViewModel,
    onNavigateToDay: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    BackHandler(enabled = uiState.isBackArrowVisible) {
        viewModel.onBackPress()
    }
    
    // Usunięto Scaffold, ponieważ nagłówek jest teraz w MainTabsScreen
    Column {
        // Logika zagnieżdżonego nagłówka jest teraz w MainTabsScreen
        // i jest kontrolowana przez ViewModel, więc nie jest tu potrzebna
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(uiState.items, key = { it.name }) { item ->
                BrowseItem(
                    item = item,
                    onClick = {
                        if (item.isDirectory) {
                            viewModel.onDirectoryClick(item.name)
                        } else {
                            val fullPath = (uiState.currentPath + item.name).joinToString("/")
                            onNavigateToDay(fullPath)
                        }
                    },
                    modifier = Modifier.animateItemPlacement(
                        animationSpec = tween(durationMillis = 250)
                    )
                )
            }
        }
    }
}

@Composable
fun BrowseItem(
    item: FileSystemItem,
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
                imageVector = if (item.isDirectory) Icons.Outlined.Folder else Icons.Outlined.Article,
                contentDescription = if (item.isDirectory) "Folder" else "Dzień",
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = item.name.replace("_", " "),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}