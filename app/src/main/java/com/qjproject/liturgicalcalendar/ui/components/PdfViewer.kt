package com.qjproject.liturgicalcalendar.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun PdfViewerDialog(
    pdfPath: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pages by remember { mutableStateOf<List<PdfPageInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()

    LaunchedEffect(pdfPath) {
        scope.launch {
            try {
                val loadedPages = loadPdfPages(context, pdfPath)
                pages = loadedPages
                isLoading = false
            } catch (e: Exception) {
                error = "Błąd podczas ładowania PDF: ${e.message}"
                isLoading = false
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header z przyciskiem zamknięcia
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Neumy",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Zamknij",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f))

                // Zawartość PDF
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    when {
                        isLoading -> {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                        error != null -> {
                            Column(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = error!!,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = onDismiss) {
                                    Text("Zamknij")
                                }
                            }
                        }
                        pages.isNotEmpty() -> {
                            ZoomablePdfContent(
                                pages = pages,
                                listState = listState,
                                pdfPath = pdfPath,
                                context = context
                            )
                        }
                        else -> {
                            Text(
                                text = "Brak zawartości do wyświetlenia",
                                modifier = Modifier.align(Alignment.Center),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}

data class PdfPageInfo(
    val bitmap: Bitmap,
    val width: Int,
    val height: Int,
    val aspectRatio: Float
)

private suspend fun loadPdfPages(context: Context, pdfPath: String): List<PdfPageInfo> {
    return withContext(Dispatchers.IO) {
        val pages = mutableListOf<PdfPageInfo>()
        
        val fileDescriptor = if (pdfPath.startsWith("assets://")) {
            // Obsługa plików z assets
            val assetPath = pdfPath.removePrefix("assets://")
            val inputStream = context.assets.open(assetPath)
            val tempFile = File.createTempFile("temp_pdf", ".pdf", context.cacheDir)
            tempFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
        } else {
            // Obsługa plików z pamięci wewnętrznej
            val file = File(pdfPath)
            if (!file.exists()) {
                throw Exception("Plik PDF nie istnieje")
            }
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        }
        
        val pdfRenderer = PdfRenderer(fileDescriptor)

        try {
            for (i in 0 until pdfRenderer.pageCount) {
                val page = pdfRenderer.openPage(i)
                
                // Zachowaj oryginalne wymiary strony
                val originalWidth = page.width
                val originalHeight = page.height
                val aspectRatio = originalWidth.toFloat() / originalHeight.toFloat()
                
                // Początkowa rozdzielczość - będzie renderowana ponownie przy zoom
                val baseWidth = 1200 // Wyższa jakość bazowa
                val baseHeight = (baseWidth / aspectRatio).toInt()
                
                val bitmap = Bitmap.createBitmap(baseWidth, baseHeight, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                
                pages.add(PdfPageInfo(
                    bitmap = bitmap,
                    width = originalWidth,
                    height = originalHeight,
                    aspectRatio = aspectRatio
                ))
                
                page.close()
            }
        } finally {
            pdfRenderer.close()
            fileDescriptor.close()
        }

        pages
    }
}

private suspend fun renderPageAtScale(
    context: Context,
    pdfPath: String,
    pageIndex: Int,
    scale: Float
): Bitmap? {
    return withContext(Dispatchers.IO) {
        try {
            val fileDescriptor = if (pdfPath.startsWith("assets://")) {
                val assetPath = pdfPath.removePrefix("assets://")
                val inputStream = context.assets.open(assetPath)
                val tempFile = File.createTempFile("temp_pdf", ".pdf", context.cacheDir)
                tempFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
                ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            } else {
                val file = File(pdfPath)
                if (!file.exists()) return@withContext null
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            }
            
            val pdfRenderer = PdfRenderer(fileDescriptor)
            val page = pdfRenderer.openPage(pageIndex)
            
            // Renderuj w wysokiej rozdzielczości odpowiedniej dla poziomu zoom
            val targetWidth = (page.width * scale * 2f).toInt().coerceAtMost(4000) // Max 4000px
            val targetHeight = (page.height * scale * 2f).toInt().coerceAtMost(6000) // Max 6000px
            
            val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            
            page.close()
            pdfRenderer.close()
            fileDescriptor.close()
            
            bitmap
        } catch (e: Exception) {
            null
        }
    }
}

@Composable
private fun ZoomablePdfContent(
    pages: List<PdfPageInfo>,
    listState: androidx.compose.foundation.lazy.LazyListState,
    pdfPath: String,
    context: Context
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }
    val scope = rememberCoroutineScope()
    
    // Cache dla renderowanych stron w wysokiej rozdzielczości
    var highResPages by remember { mutableStateOf<Map<Int, Bitmap>>(emptyMap()) }
    
    // Re-render pages when scale changes significantly
    LaunchedEffect(scale) {
        if (scale > 1.5f) {
            scope.launch {
                val newHighResPages = mutableMapOf<Int, Bitmap>()
                pages.forEachIndexed { index, _ ->
                    val highResBitmap = renderPageAtScale(context, pdfPath, index, scale)
                    if (highResBitmap != null) {
                        newHighResPages[index] = highResBitmap
                    }
                }
                highResPages = newHighResPages
            }
        } else {
            // Wyczyść cache przy małym zoom
            highResPages = emptyMap()
        }
    }
    
    val transformableState = rememberTransformableState { zoomChange, offsetChange, _ ->
        val newScale = (scale * zoomChange).coerceIn(1f, 5f)
        
        // Oblicz nowy offset z rzeczywistymi ograniczeniami na podstawie rozmiarów zawartości
        val newOffset = if (newScale > 1f && containerSize != androidx.compose.ui.geometry.Size.Zero) {
            // Oblicz rzeczywiste wymiary powiększonej zawartości
            val scaledContentWidth = containerSize.width * newScale
            val scaledContentHeight = containerSize.height * newScale
            
            // Maksymalne przesunięcie = (powiększona zawartość - kontener) / 2
            val maxOffsetX = kotlin.math.max(0f, (scaledContentWidth - containerSize.width) / 2f)
            val maxOffsetY = kotlin.math.max(0f, (scaledContentHeight - containerSize.height) / 2f)
            
            Offset(
                x = (offset.x + offsetChange.x).coerceIn(-maxOffsetX, maxOffsetX),
                y = (offset.y + offsetChange.y).coerceIn(-maxOffsetY, maxOffsetY)
            )
        } else {
            // Przy zoom 1x wycentruj
            Offset.Zero
        }
        
        scale = newScale
        offset = newOffset
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .transformable(state = transformableState)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        // Double tap to toggle zoom between 1x and 2x
                        scale = if (scale > 1f) 1f else 2f
                        // Reset offset when returning to 1x
                        if (scale <= 1f) {
                            offset = Offset.Zero
                        }
                    }
                )
            }
    ) {
        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y,
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin.Center
                )
                .layout { measurable, constraints ->
                    val placeable: Placeable = measurable.measure(constraints)
                    // Zapisz rozmiar kontenera dla obliczeń offset
                    containerSize = androidx.compose.ui.geometry.Size(
                        placeable.width.toFloat(),
                        placeable.height.toFloat()
                    )
                    layout(placeable.width, placeable.height) {
                        placeable.placeRelative(0, 0)
                    }
                }
        ) {
            itemsIndexed(pages) { index, pageInfo ->
                val bitmapToUse = if (scale > 1.5f && highResPages.containsKey(index)) {
                    highResPages[index]!!
                } else {
                    pageInfo.bitmap
                }
                
                EnhancedImageCard(
                    bitmap = bitmapToUse,
                    pageNumber = index + 1,
                    isHighRes = highResPages.containsKey(index)
                )
            }
        }
        
        // Wskaźnik powiększenia w prawym górnym rogu
        if (scale > 1f) {
            Card(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black.copy(alpha = 0.7f)
                )
            ) {
                Text(
                    text = "${String.format("%.1f", scale)}x",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun EnhancedImageCard(
    bitmap: Bitmap,
    pageNumber: Int,
    isHighRes: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Strona $pageNumber",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Black
                )
                
                if (isHighRes) {
                    Text(
                        text = "HD",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Strona $pageNumber",
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.FillWidth
            )
        }
    }
}
