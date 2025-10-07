package com.qjproject.liturgicalcalendar.data

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class NeumyManager(private val context: Context) {
    
    private val neumyDir = File(context.filesDir, "neumy")
    private val assetsNeumyPath = "neumy"
    
    init {
        if (!neumyDir.exists()) {
            neumyDir.mkdirs()
        }
    }
    
    /**
     * Sprawdza czy istnieje plik PDF dla danej pieśni (w assets lub w pamięci wewnętrznej)
     */
    fun hasPdfForSong(songTitle: String): Boolean {
        val sanitizedTitle = sanitizeSongTitle(songTitle)
        
        // Sprawdź w pamięci wewnętrznej (z sanityzowaną nazwą)
        val internalFile = File(neumyDir, "$sanitizedTitle.pdf")
        if (internalFile.exists()) {
            return true
        }
        
        // Sprawdź w assets (z oryginalną nazwą - ze spacjami)
        return try {
            val assetFiles = context.assets.list(assetsNeumyPath) ?: emptyArray()
            // W assets używamy oryginalnej nazwy (ze spacjami), nie sanityzowanej
            assetFiles.contains("$songTitle.pdf")
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Zwraca ścieżkę do pliku PDF dla danej pieśni
     */
    fun getPdfPathForSong(songTitle: String): String? {
        val sanitizedTitle = sanitizeSongTitle(songTitle)
        
        // Sprawdź w pamięci wewnętrznej (z sanityzowaną nazwą)
        val internalFile = File(neumyDir, "$sanitizedTitle.pdf")
        if (internalFile.exists()) {
            return internalFile.absolutePath
        }
        
        // Sprawdź w assets (z oryginalną nazwą - ze spacjami)
        return try {
            val assetFiles = context.assets.list(assetsNeumyPath) ?: emptyArray()
            // W assets używamy oryginalnej nazwy (ze spacjami), nie sanityzowanej
            if (assetFiles.contains("$songTitle.pdf")) {
                "assets://$assetsNeumyPath/$songTitle.pdf"
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Kopiuje wybrany plik PDF do folderu neumy z odpowiednią nazwą
     */
    fun savePdfForSong(songTitle: String, sourceUri: Uri): Result<String> {
        return try {
            val sanitizedTitle = sanitizeSongTitle(songTitle)
            val targetFile = File(neumyDir, "$sanitizedTitle.pdf")
            
            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                FileOutputStream(targetFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            
            Result.success(targetFile.absolutePath)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Usuwa plik PDF dla danej pieśni
     */
    fun deletePdfForSong(songTitle: String): Boolean {
        val sanitizedTitle = sanitizeSongTitle(songTitle)
        val pdfFile = File(neumyDir, "$sanitizedTitle.pdf")
        return if (pdfFile.exists()) {
            pdfFile.delete()
        } else {
            false
        }
    }
    
    /**
     * Czyści nazwę pieśni z niedozwolonych znaków dla nazwy pliku
     */
    private fun sanitizeSongTitle(title: String): String {
        return title
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .replace(Regex("\\s+"), "_")
            .trim('_')
    }
    
    /**
     * Czyści folder neumy z wszystkich plików PDF
     * Używane przed ponownym kopiowaniem plików podczas aktualizacji
     */
    private fun clearNeumyDirectory() {
        try {
            neumyDir.listFiles()?.forEach { file ->
                if (file.isFile && file.extension.lowercase() == "pdf") {
                    val deleted = file.delete()
                    if (deleted) {
                        android.util.Log.d("NeumyManager", "Usunięto stary plik: ${file.name}")
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("NeumyManager", "Błąd podczas czyszczenia katalogu neumów: ${e.message}")
        }
    }
    
    /**
     * Kopiuje wszystkie pliki PDF z folderu assets/neumy do pamięci wewnętrznej
     * Ta funkcja jest wywoływana podczas pierwszego uruchomienia aplikacji
     * Pliki są zapisywane z sanityzowanymi nazwami (spacje zamienione na podkreślniki)
     */
    fun copyAssetsToInternalStorage(): Result<Int> {
        return try {
            // Wyczyść stare pliki przed kopiowaniem nowych
            clearNeumyDirectory()
            
            var copiedCount = 0
            
            // Pobierz listę plików z assets/neumy
            val assetFiles = context.assets.list(assetsNeumyPath) ?: emptyArray()
            val pdfFiles = assetFiles.filter { it.endsWith(".pdf", ignoreCase = true) }
            
            for (originalFileName in pdfFiles) {
                try {
                    // Otwórz plik z assets (oryginalna nazwa ze spacjami)
                    val inputStream = context.assets.open("$assetsNeumyPath/$originalFileName")
                    
                    // Pobierz tytuł pieśni (nazwa bez rozszerzenia)
                    val songTitle = originalFileName.removeSuffix(".pdf")
                    // Sanityzuj nazwę dla pliku docelowego
                    val sanitizedFileName = "${sanitizeSongTitle(songTitle)}.pdf"
                    
                    // Utwórz plik docelowy w pamięci wewnętrznej z sanityzowaną nazwą
                    val targetFile = File(neumyDir, sanitizedFileName)
                    
                    // Kopiuj plik
                    FileOutputStream(targetFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                    copiedCount++
                    android.util.Log.d("NeumyManager", "Skopiowano: $originalFileName -> $sanitizedFileName")
                    
                    inputStream.close()
                } catch (e: Exception) {
                    // Loguj błąd ale kontynuuj kopiowanie innych plików
                    android.util.Log.e("NeumyManager", "Błąd podczas kopiowania pliku $originalFileName: ${e.message}")
                }
            }
            
            Result.success(copiedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Zwraca listę wszystkich dostępnych plików PDF z neumami
     * Priorytet: najpierw pamięć wewnętrzna, potem assets
     */
    fun getAllPdfFiles(): List<Pair<String, String>> {
        val allFiles = mutableListOf<Pair<String, String>>()
        val processedTitles = mutableSetOf<String>()
        
        // Najpierw dodaj pliki z pamięci wewnętrznej (sanityzowane nazwy)
        neumyDir.listFiles { file ->
            file.isFile && file.extension.lowercase() == "pdf"
        }?.forEach { file ->
            // Nazwa pliku jest już sanityzowana (podkreślniki zamiast spacji)
            val songTitle = file.nameWithoutExtension.replace("_", " ")
            allFiles.add(Pair(songTitle, file.absolutePath))
            processedTitles.add(songTitle)
        }
        
        // Potem dodaj pliki z assets (oryginalne nazwy ze spacjami)
        try {
            val assetFiles = context.assets.list(assetsNeumyPath) ?: emptyArray()
            assetFiles.filter { it.endsWith(".pdf", ignoreCase = true) }.forEach { fileName ->
                // W assets nazwy mają oryginalne spacje
                val songTitle = fileName.removeSuffix(".pdf")
                // Dodaj tylko jeśli nie ma już takiego pliku z pamięci wewnętrznej
                if (!processedTitles.contains(songTitle)) {
                    allFiles.add(Pair(songTitle, "assets://$assetsNeumyPath/$fileName"))
                    processedTitles.add(songTitle)
                }
            }
        } catch (e: Exception) {
            // Ignoruj błędy dostępu do assets
        }
        
        return allFiles
    }
}
