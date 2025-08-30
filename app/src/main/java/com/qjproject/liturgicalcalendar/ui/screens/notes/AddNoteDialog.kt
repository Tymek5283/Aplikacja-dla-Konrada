// Ścieżka: app/src/main/java/com/qjproject/liturgicalcalendar/ui/screens/notes/AddNoteDialog.kt
// Opis: Dialog do dodawania nowych notatek z polami tytułu i opisu
package com.qjproject.liturgicalcalendar.ui.screens.notes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.qjproject.liturgicalcalendar.ui.theme.SaturatedNavy
import com.qjproject.liturgicalcalendar.ui.theme.TileBackground
import com.qjproject.liturgicalcalendar.ui.theme.VeryDarkNavy

@Composable
fun AddNoteDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, description: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var titleError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = VeryDarkNavy)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Dodaj notatkę",
                    style = MaterialTheme.typography.titleLarge,
                    color = SaturatedNavy
                )
                
                Spacer(Modifier.height(2.dp))
                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))
                Spacer(Modifier.height(2.dp))

                OutlinedTextField(
                    value = title,
                    onValueChange = { 
                        title = it
                        titleError = false
                    },
                    label = { Text("Tytuł *", color = Color.White.copy(alpha = 0.7f)) },
                    isError = titleError,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = SaturatedNavy,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                        cursorColor = SaturatedNavy
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (titleError) {
                    Text(
                        text = "Tytuł jest wymagany",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Opis", color = Color.White.copy(alpha = 0.7f)) },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = SaturatedNavy,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                        cursorColor = SaturatedNavy
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    maxLines = 4
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color.White.copy(alpha = 0.7f)
                        )
                    ) {
                        Text("Anuluj")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (title.trim().isEmpty()) {
                                titleError = true
                            } else {
                                onConfirm(title.trim(), description.trim())
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = TileBackground,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Dodaj")
                    }
                }
            }
        }
    }
}
