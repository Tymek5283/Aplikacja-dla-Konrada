// Ścieżka: app/src/main/java/com/qjproject/liturgicalcalendar/ui/screens/daydetails/daydetailsscreen/DayDetailsDialogs.kt
// Opis: Plik ten centralizuje wszystkie okna dialogowe używane na ekranie DayDetailsScreen, takie jak potwierdzenia, formularze dodawania/edycji czy modale informacyjne.
package com.qjproject.liturgicalcalendar.ui.screens.daydetails.daydetailsscreen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.qjproject.liturgicalcalendar.data.Reading
import com.qjproject.liturgicalcalendar.data.Song
import com.qjproject.liturgicalcalendar.data.SuggestedSong
import com.qjproject.liturgicalcalendar.ui.screens.daydetails.daydetailsviewmodel.DayDetailsViewModel
import com.qjproject.liturgicalcalendar.ui.theme.EditModeSubtleBlue
import com.qjproject.liturgicalcalendar.ui.theme.SaturatedNavy
import com.qjproject.liturgicalcalendar.ui.theme.VeryDarkNavy

@Composable
internal fun ConfirmExitDialog(onDismiss: () -> Unit, onDiscard: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = VeryDarkNavy)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    "Odrzucić zmiany?",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                    color = SaturatedNavy
                )
                Spacer(Modifier.height(16.dp))
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                Spacer(Modifier.height(16.dp))
                Text("Czy na pewno chcesz wyjść bez zapisywania zmian?", color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDiscard) {
                        Text("Tak", color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = onDismiss) { Text("Nie") }
                }
            }
        }
    }
}

@Composable
internal fun ConfirmDeleteDialog(description: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = VeryDarkNavy)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    "Potwierdź usunięcie",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                    color = SaturatedNavy
                )
                Spacer(Modifier.height(16.dp))
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                Spacer(Modifier.height(16.dp))

                val (prefix, itemName) = remember(description) {
                    val parts = description.split(":", limit = 2)
                    if (parts.size == 2) {
                        Pair(parts[0].trim() + ":", parts[1].trim())
                    } else {
                        Pair("", description.trim())
                    }
                }

                Text(
                    buildAnnotatedString {
                        append("Czy na pewno chcesz usunąć ")
                        append(prefix)
                        if (prefix.isNotEmpty()) {
                            append(" ")
                        }
                        withStyle(style = SpanStyle(color = SaturatedNavy, fontWeight = FontWeight.Bold)) {
                            append(itemName)
                        }
                        append("?")
                    },
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Anuluj") }; Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Usuń") }
                }
            }
        }
    }
}

@Composable
internal fun AddEditReadingDialog(existingReading: Reading? = null, onDismiss: () -> Unit, onConfirm: (Reading) -> Unit) {
    var typ by remember { mutableStateOf(existingReading?.typ ?: "") }
    var sigla by remember { mutableStateOf(existingReading?.sigla ?: "") }
    var opis by remember { mutableStateOf(existingReading?.opis ?: "") }
    var tekst by remember { mutableStateOf(existingReading?.tekst ?: "") }
    val isTypValid by remember { derivedStateOf { typ.isNotBlank() } }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = VeryDarkNavy)
        ) {
            Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                Text(
                    if (existingReading == null) "Dodaj nowe czytanie" else "Edytuj czytanie",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp),
                    color = SaturatedNavy
                )
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(typ, { typ = it }, label = { Text("Typ*") }, isError = !isTypValid, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(sigla, { sigla = it }, label = { Text("Sigla") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(opis, { opis = it }, label = { Text("Opis") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(tekst, { tekst = it }, label = { Text("Tekst") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Anuluj") }; Spacer(Modifier.width(8.dp))
                    Button(onClick = { onConfirm(existingReading?.copy(typ = typ, sigla = sigla, opis = opis, tekst = tekst) ?: Reading(typ, sigla, opis, tekst)); onDismiss() }, enabled = isTypValid) { Text("Zapisz") }
                }
            }
        }
    }
}

@Composable
internal fun AddEditSongDialog(
    moment: String,
    existingSong: SuggestedSong? = null,
    viewModel: DayDetailsViewModel,
    error: String?,
    onDismiss: () -> Unit,
    onConfirm: (title: String, siedl: String, sak: String, dn: String, opis: String, moment: String, originalSong: SuggestedSong?) -> Unit
) {
    var piesn by remember { mutableStateOf(existingSong?.piesn ?: "") }
    var opis by remember { mutableStateOf(existingSong?.opis ?: "") }
    var numerSiedl by remember { mutableStateOf(existingSong?.numer ?: "") }
    var numerSak by remember { mutableStateOf("") }
    var numerDn by remember { mutableStateOf("") }
    val isPiesnValid by remember { derivedStateOf { piesn.isNotBlank() } }

    val titleSearchResults by viewModel.songTitleSearchResults.collectAsState()
    val siedlSearchResults by viewModel.siedlSearchResults.collectAsState()
    val sakSearchResults by viewModel.sakSearchResults.collectAsState()
    val dnSearchResults by viewModel.dnSearchResults.collectAsState()
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(existingSong) {
        if (existingSong != null) {
            viewModel.getFullSong(existingSong) { fullSong ->
                if (fullSong != null) {
                    numerSak = fullSong.numerSAK
                    numerDn = fullSong.numerDN
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.clearAllSearchResults()
        focusRequester.requestFocus()
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = VeryDarkNavy)
        ) {
            Column(
                Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    if (existingSong == null) "Dodaj nową pieśń" else "Edytuj pieśń",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp),
                    color = SaturatedNavy
                )
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                Spacer(Modifier.height(16.dp))

                val onSuggestionClick: (Song) -> Unit = { song ->
                    piesn = song.tytul
                    numerSiedl = song.numerSiedl
                    numerSak = song.numerSAK
                    numerDn = song.numerDN
                    viewModel.clearAllSearchResults()
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column {
                        OutlinedTextField(
                            value = piesn,
                            onValueChange = {
                                piesn = it
                                viewModel.searchSongsByTitle(it)
                            },
                            label = { Text("Tytuł pieśni*") },
                            isError = !isPiesnValid,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester)
                        )
                        SearchSuggestionList(
                            results = titleSearchResults,
                            onSuggestionClick = onSuggestionClick,
                            viewModel = viewModel
                        )
                    }

                    Column {
                        OutlinedTextField(
                            value = numerSiedl,
                            onValueChange = {
                                numerSiedl = it
                                viewModel.searchSongsBySiedl(it)
                            },
                            label = { Text("Numer Siedlecki") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        SearchSuggestionList(
                            results = siedlSearchResults,
                            onSuggestionClick = onSuggestionClick,
                            viewModel = viewModel
                        )
                    }

                    Column {
                        OutlinedTextField(
                            value = numerSak,
                            onValueChange = {
                                numerSak = it
                                viewModel.searchSongsBySak(it)
                            },
                            label = { Text("Numer ŚAK") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        SearchSuggestionList(
                            results = sakSearchResults,
                            onSuggestionClick = onSuggestionClick,
                            viewModel = viewModel
                        )
                    }

                    Column {
                        OutlinedTextField(
                            value = numerDn,
                            onValueChange = {
                                numerDn = it
                                viewModel.searchSongsByDn(it)
                            },
                            label = { Text("Numer DN") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        SearchSuggestionList(
                            results = dnSearchResults,
                            onSuggestionClick = onSuggestionClick,
                            viewModel = viewModel
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                OutlinedTextField(opis, { opis = it }, label = { Text("Opis") }, modifier = Modifier.fillMaxWidth())

                if (error != null) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = {
                        viewModel.clearAllSearchResults()
                        onDismiss()
                    }) { Text("Anuluj") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onConfirm(piesn, numerSiedl, numerSak, numerDn, opis, moment, existingSong)
                        },
                        enabled = isPiesnValid
                    ) { Text("Zapisz") }
                }
            }
        }
    }
}

@Composable
private fun SearchSuggestionList(
    results: List<Song>,
    onSuggestionClick: (Song) -> Unit,
    viewModel: DayDetailsViewModel
) {
    AnimatedVisibility(visible = results.isNotEmpty()) {
        LazyColumn(
            modifier = Modifier
                .heightIn(max = 240.dp)
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            items(
                items = results,
                key = { song -> "${song.tytul}-${song.numerSiedl}" }
            ) { song ->
                Text(
                    text = viewModel.formatSongSuggestion(song),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSuggestionClick(song) }
                        .padding(12.dp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}


@Composable
internal fun SongDetailsModal(
    suggestedSong: SuggestedSong,
    fullSong: Song,
    onDismiss: () -> Unit,
    onShowContent: (song: Song, startInEdit: Boolean) -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 48.dp),
            colors = CardDefaults.cardColors(containerColor = VeryDarkNavy)
        ) {
            Column(Modifier.padding(24.dp)) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Text(
                        suggestedSong.piesn,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                        color = SaturatedNavy
                    )
                    IconButton(onClick = onDismiss, Modifier.size(24.dp)) { Icon(Icons.Default.Close, "Zamknij") }
                }
                Spacer(Modifier.height(16.dp))
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                Spacer(Modifier.height(16.dp))
                Column(
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .weight(1f, fill = false)
                ) {
                    SongNumberInfo("Siedlecki:", fullSong.numerSiedl)
                    SongNumberInfo("ŚAK:", fullSong.numerSAK)
                    SongNumberInfo("DN:", fullSong.numerDN)
                    
                    // Wyświetlanie rzeczywistej kategorii pieśni zamiast momentu liturgicznego
                    if (fullSong.kategoria.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Row {
                            Text(
                                text = "Kategoria:",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.width(80.dp),
                                color = SaturatedNavy
                            )
                            Text(
                                text = fullSong.kategoria,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }


                    if (suggestedSong.opis.isNotBlank()) {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            buildAnnotatedString {
                                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = SaturatedNavy)) { append("Opis:\n") }
                                append(suggestedSong.opis)
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = {
                        onShowContent(fullSong, false)
                        onDismiss()
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Treść")
                }
            }
        }
    }
}

@Composable
private fun SongNumberInfo(label: String, number: String) {
    val displayValue = number.ifBlank { "-" }
    Row {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.width(80.dp),
            color = SaturatedNavy
        )
        Text(
            text = displayValue,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
        )
    }
}