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
        
        // Sprawdź w pamięci wewnętrznej
        val internalFile = File(neumyDir, "$sanitizedTitle.pdf")
        if (internalFile.exists()) {
            return true
        }
        
        // Sprawdź w assets
        return try {
            val assetFiles = context.assets.list(assetsNeumyPath) ?: emptyArray()
            assetFiles.contains("$sanitizedTitle.pdf")
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Zwraca ścieżkę do pliku PDF dla danej pieśni
     */
    fun getPdfPathForSong(songTitle: String): String? {
        val sanitizedTitle = sanitizeSongTitle(songTitle)
        
        // Sprawdź w pamięci wewnętrznej
        val internalFile = File(neumyDir, "$sanitizedTitle.pdf")
        if (internalFile.exists()) {
            return internalFile.absolutePath
        }
        
        // Sprawdź w assets
        return try {
            val assetFiles = context.assets.list(assetsNeumyPath) ?: emptyArray()
            if (assetFiles.contains("$sanitizedTitle.pdf")) {
                "assets://$assetsNeumyPath/$sanitizedTitle.pdf"
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
     * Zwraca listę wszystkich dostępnych plików PDF z neumami
     */
    fun getAllPdfFiles(): List<Pair<String, String>> {
        val allFiles = mutableListOf<Pair<String, String>>()
        
        // Dodaj pliki z assets
        try {
            val assetFiles = context.assets.list(assetsNeumyPath) ?: emptyArray()
            assetFiles.filter { it.endsWith(".pdf", ignoreCase = true) }.forEach { fileName ->
                val songTitle = fileName.removeSuffix(".pdf").replace("_", " ")
                allFiles.add(Pair(songTitle, "assets://$assetsNeumyPath/$fileName"))
            }
        } catch (e: Exception) {
            // Ignoruj błędy dostępu do assets
        }
        
        // Dodaj pliki z pamięci wewnętrznej (jeśli nie ma już takiego z assets)
        neumyDir.listFiles { file ->
            file.isFile && file.extension.lowercase() == "pdf"
        }?.forEach { file ->
            val songTitle = file.nameWithoutExtension.replace("_", " ")
            // Sprawdź czy już nie ma takiego pliku z assets
            if (!allFiles.any { it.first == songTitle }) {
                allFiles.add(Pair(songTitle, file.absolutePath))
            }
        }
        
        return allFiles
    }
}
