package com.qjproject.liturgicalcalendar.ui.screens.search

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.qjproject.liturgicalcalendar.data.Category
import com.qjproject.liturgicalcalendar.data.Song
import com.qjproject.liturgicalcalendar.ui.theme.VeryDarkNavy

@Composable
fun SearchBar(query: String, onQueryChange: (String) -> Unit, placeholder: String) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Ikona wyszukiwania") },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Wyczyść")
                }
            }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.extraLarge,
        textStyle = MaterialTheme.typography.bodyLarge
    )
}

@Composable
fun NoResults() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Brak wyników.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun SearchResultsContent(
    categories: List<Category>,
    songs: List<Song>,
    isGlobalSearch: Boolean,
    onCategoryClick: (Category) -> Unit,
    onNoCategoryClick: () -> Unit,
    onSongClick: (Song) -> Unit,
    onSongLongClick: (Song) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(
            start = 8.dp,
            end = 8.dp,
            top = 8.dp,
            bottom = 80.dp
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories, key = { "category_${it.nazwa}" }) { category ->
            CategoryItem(
                category = category,
                onClick = { onCategoryClick(category) }
            )
        }

        if (isGlobalSearch && categories.isNotEmpty()) {
            item { Divider(modifier = Modifier.padding(vertical = 4.dp)) }
            item {
                CategoryItem(
                    category = Category("Brak kategorii", ""),
                    onClick = onNoCategoryClick
                )
            }
        }

        if (songs.isNotEmpty()) {
            items(songs, key = { song ->
                "song_${song.tytul}_${song.numerSiedl}_${song.numerSAK}_${song.numerDN}"
            }) { song ->
                SongResultItem(
                    song = song,
                    onClick = { onSongClick(song) },
                    onLongClick = { onSongLongClick(song) }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongResultItem(
    song: Song,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.MusicNote, contentDescription = "Pieśń", tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Text(song.tytul, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.width(8.dp))
            Text(song.kategoriaSkr, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun CategoryItem(
    category: Category,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = VeryDarkNavy)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Category,
                contentDescription = "Kategoria",
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = category.nazwa, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}