package com.qjproject.liturgicalcalendar.ui.screens.notes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.qjproject.liturgicalcalendar.ui.theme.SectionHeaderBlue
import com.qjproject.liturgicalcalendar.ui.theme.EditModeDimmedButton
import com.qjproject.liturgicalcalendar.ui.theme.TileBackground
import com.qjproject.liturgicalcalendar.ui.theme.VeryDarkNavy

@Composable
fun EditTitleDescriptionDialog(
    currentTitle: String,
    currentDescription: String,
    onDismiss: () -> Unit,
    onConfirm: (title: String, description: String) -> Unit
) {
    var title by remember { mutableStateOf(currentTitle) }
    var description by remember { mutableStateOf(currentDescription) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = TileBackground)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Edytuj tytuł i opis",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SectionHeaderBlue
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { 
                        Text(
                            text = "Tytuł",
                            color = SectionHeaderBlue
                        ) 
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = SectionHeaderBlue,
                        unfocusedBorderColor = SectionHeaderBlue.copy(alpha = 0.7f),
                        cursorColor = SectionHeaderBlue
                    ),
                    singleLine = false,
                    maxLines = 3
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { 
                        Text(
                            text = "Opis",
                            color = SectionHeaderBlue
                        ) 
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = SectionHeaderBlue,
                        unfocusedBorderColor = SectionHeaderBlue.copy(alpha = 0.7f),
                        cursorColor = SectionHeaderBlue
                    ),
                    singleLine = false,
                    maxLines = 5
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = SectionHeaderBlue
                        )
                    ) {
                        Text("Anuluj")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = { onConfirm(title, description) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EditModeDimmedButton,
                            contentColor = Color.White
                        ),
                        enabled = title.isNotBlank()
                    ) {
                        Text("Zapisz")
                    }
                }
            }
        }
    }
}
