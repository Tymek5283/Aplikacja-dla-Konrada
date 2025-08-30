package com.qjproject.liturgicalcalendar.data

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import kotlinx.serialization.Serializable

@Serializable
data class TextSpan(
    val text: String,
    val start: Int,
    val end: Int,
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderlined: Boolean = false,
    val colorHex: String? = null, // Format: "#RRGGBB"
    val isListItem: Boolean = false,
    val listLevel: Int = 0
)

@Serializable
data class RichTextContent(
    val plainText: String,
    val spans: List<TextSpan> = emptyList(),
    val listItems: List<ListItem> = emptyList()
)

@Serializable
data class ListItem(
    val text: String,
    val level: Int = 0,
    val startIndex: Int,
    val endIndex: Int
)

data class FormattingState(
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isUnderlined: Boolean = false,
    val selectedColor: Color? = null,
    val isList: Boolean = false,
    val currentListLevel: Int = 0
)

// Rozszerzenia do konwersji między formatami
fun TextSpan.toSpanStyle(): SpanStyle {
    return SpanStyle(
        fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
        fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal,
        textDecoration = if (isUnderlined) TextDecoration.Underline else TextDecoration.None,
        color = colorHex?.let { hex ->
            try {
                Color(android.graphics.Color.parseColor(hex))
            } catch (e: Exception) {
                Color.Unspecified
            }
        } ?: Color.Unspecified
    )
}

fun Color.toHexString(): String {
    val argb = this.toArgb()
    val red = (argb shr 16) and 0xFF
    val green = (argb shr 8) and 0xFF
    val blue = argb and 0xFF
    return "#%02X%02X%02X".format(red, green, blue)
}

// Konwersja do HTML dla przyszłej kompatybilności
fun RichTextContent.toHtml(): String {
    var html = plainText
    
    // Sortuj spany według pozycji od końca do początku, aby nie zaburzyć indeksów
    val sortedSpans = spans.sortedByDescending { it.start }
    
    for (span in sortedSpans) {
        val beforeText = html.substring(0, span.start)
        val spanText = html.substring(span.start, span.end)
        val afterText = html.substring(span.end)
        
        var formattedText = spanText
        
        if (span.isBold) formattedText = "<b>$formattedText</b>"
        if (span.isItalic) formattedText = "<i>$formattedText</i>"
        if (span.isUnderlined) formattedText = "<u>$formattedText</u>"
        if (span.colorHex != null) {
            formattedText = "<span style=\"color: ${span.colorHex}\">$formattedText</span>"
        }
        
        html = beforeText + formattedText + afterText
    }
    
    // Dodaj listy
    for (listItem in listItems) {
        val indent = "  ".repeat(listItem.level)
        html = html.replace(listItem.text, "$indent• ${listItem.text}")
    }
    
    return html
}

// Konwersja z HTML (dla przyszłej kompatybilności)
fun String.fromHtml(): RichTextContent {
    // Uproszczona implementacja - w przyszłości można rozszerzyć
    val plainText = this
        .replace(Regex("<[^>]*>"), "") // Usuń tagi HTML
        .replace("&nbsp;", " ")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&amp;", "&")
    
    return RichTextContent(plainText = plainText)
}
