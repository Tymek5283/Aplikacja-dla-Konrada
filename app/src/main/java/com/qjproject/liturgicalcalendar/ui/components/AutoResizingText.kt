package com.qjproject.liturgicalcalendar.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow

@Composable
fun AutoResizingText(
    modifier: Modifier = Modifier,
    text: String,
    style: TextStyle,
    maxLines: Int = 2,
    textAlign: TextAlign
) {
    var resizedTextStyle by remember(text) {
        mutableStateOf(style)
    }

    var shouldShrink by remember(text) {
        mutableStateOf(true)
    }

    // --- POCZĄTEK ZMIANY ---
    // Odczytujemy minimalny rozmiar czcionki tutaj, w bezpiecznym kontekście @Composable.
    val minFontSize = MaterialTheme.typography.bodySmall.fontSize
    // --- KONIEC ZMIANY ---

    Text(
        text = text,
        modifier = modifier,
        style = resizedTextStyle,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        textAlign = textAlign,
        onTextLayout = { textLayoutResult ->
            if (textLayoutResult.didOverflowHeight && shouldShrink) {
                val newFontSize = resizedTextStyle.fontSize * 0.9f

                // --- POCZĄTEK ZMIANY ---
                // Używamy wcześniej odczytanej wartości `minFontSize`.
                if (newFontSize > minFontSize) {
                    resizedTextStyle = resizedTextStyle.copy(fontSize = newFontSize)
                } else {
                    shouldShrink = false
                }
                // --- KONIEC ZMIANY ---
            }
        }
    )
}