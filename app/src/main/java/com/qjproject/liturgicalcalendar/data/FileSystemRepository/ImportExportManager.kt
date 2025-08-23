// Ścieżka: C:\Users\blzej\Desktop\Aplikacja dla studenta\Aplikacja-dla-Konrada\app\src\main\java\com\qjproject\liturgicalcalendar\data\repository\FileSystemRepository\ImportExportManager.kt
// Opis: Zarządza procesami importu i eksportu danych aplikacji. Odpowiada za pakowanie danych do archiwum ZIP oraz ich rozpakowywanie i weryfikację.
package com.qjproject.liturgicalcalendar.data.repository.FileSystemRepository

import android.content.Context
import android.net.Uri
import android.os.Environment
import net.lingala.zip4j.ZipFile
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

internal class ImportExportManager(
    private val context: Context,
    private val internalStorageRoot: File
) {
    fun exportDataToZip(): Result<File> {
        return try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).apply { mkdirs() }
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val zipFile = File(downloadsDir, "Laudate_Export_$timestamp.zip")
            val zip = ZipFile(zipFile)

            File(internalStorageRoot, "data").takeIf { it.exists() }?.let { zip.addFolder(it) }
            File(internalStorageRoot, "Datowane").takeIf { it.exists() }?.let { zip.addFolder(it) }
            File(internalStorageRoot, "piesni.json").takeIf { it.exists() }?.let { zip.addFile(it) }
            File(internalStorageRoot, "kategorie.json").takeIf { it.exists() }?.let { zip.addFile(it) }

            Result.success(zipFile)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    fun importDataFromZip(uri: Uri): Result<Unit> {
        val tempUnzipDir = File(context.cacheDir, "import_unzip_temp")
        val tempZipFile = File(context.cacheDir, "import.zip")

        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tempZipFile).use { output -> input.copyTo(output) }
            } ?: return Result.failure(IOException("Nie można otworzyć strumienia z URI."))

            tempUnzipDir.deleteRecursively()
            tempUnzipDir.mkdirs()
            ZipFile(tempZipFile).extractAll(tempUnzipDir.absolutePath)

            val foundFiles = findRequiredFiles(tempUnzipDir)
                ?: return Result.failure(IllegalStateException("Plik ZIP nie zawiera wymaganych plików/folderów."))

            File(internalStorageRoot, "data").deleteRecursively()
            File(internalStorageRoot, "Datowane").deleteRecursively()
            File(internalStorageRoot, "piesni.json").delete()
            File(internalStorageRoot, "kategorie.json").delete()

            foundFiles.dataDir.copyRecursively(File(internalStorageRoot, "data"), true)
            foundFiles.datowaneDir.copyRecursively(File(internalStorageRoot, "Datowane"), true)
            foundFiles.songFile.copyTo(File(internalStorageRoot, "piesni.json"), true)

            findFileRecursively(tempUnzipDir, "kategorie.json")?.copyTo(File(internalStorageRoot, "kategorie.json"), true)

            return Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.failure(e)
        } finally {
            tempUnzipDir.deleteRecursively()
            tempZipFile.delete()
        }
    }

    private fun findFileRecursively(directory: File, fileName: String): File? {
        return directory.walkTopDown().find { it.isFile && it.name == fileName }
    }

    private fun findRequiredFiles(startDir: File): FoundImportFiles? {
        val queue: Queue<File> = LinkedList(listOf(startDir))
        while (queue.isNotEmpty()) {
            val currentDir = queue.poll()
            val dataDir = File(currentDir, "data")
            val datowaneDir = File(currentDir, "Datowane")
            val songFile = File(currentDir, "piesni.json")

            if (dataDir.isDirectory && datowaneDir.isDirectory && songFile.isFile) {
                return FoundImportFiles(dataDir, datowaneDir, songFile)
            }
            currentDir.listFiles { file -> file.isDirectory }?.let { queue.addAll(it) }
        }
        return null
    }
}