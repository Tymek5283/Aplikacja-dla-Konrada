package com.qjproject.liturgicalcalendar.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.BasicRichTextEditor

@Composable
fun CustomRichTextEditor(
    htmlContent: String,
    onContentChange: (String) -> Unit,
    richTextState: RichTextState,
    onSelectionChange: (TextRange) -> Unit,
    modifier: Modifier = Modifier
) {
    // Inicjalizuj stan z HTML
    LaunchedEffect(htmlContent) {
        if (richTextState.toHtml() != htmlContent) {
            richTextState.setHtml(htmlContent)
        }
    }
    
    // Obsługa zmian zaznaczenia
    LaunchedEffect(richTextState.selection) {
        onSelectionChange(richTextState.selection)
    }
    
    // Wrapper Box z tłem Surface dla zapewnienia spójności wizualnej
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(8.dp)
            )
            .clip(RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 12.dp)
    ) {
        BasicRichTextEditor(
            state = richTextState,
            modifier = Modifier.fillMaxSize(),
            textStyle = TextStyle(
                color = Color.White,
                fontSize = 16.sp,
                lineHeight = 24.sp
            ),
            cursorBrush = SolidColor(Color.White),
            decorationBox = { innerTextField ->
                if (richTextState.annotatedString.text.isEmpty()) {
                    Text(
                        text = "Napisz swoją notatkę...",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 16.sp
                    )
                }
                innerTextField()
            }
        )
    }
    
    // Obsługa zmian tekstu - konwersja do HTML
    LaunchedEffect(richTextState.annotatedString) {
        onContentChange(richTextState.toHtml())
    }
}

// Funkcje pomocnicze do konwersji HTML
fun String.toHtml(): String = this

fun String.fromHtml(): String = this
