// Ścieżka: C:\Users\blzej\Desktop\Aplikacja dla studenta\Aplikacja-dla-Konrada\app\src\main\java\com\qjproject\liturgicalcalendar\data\repository\FileSystemRepository\DirectoryManager.kt
// Opis: Odpowiada za wszystkie operacje na systemie plików, takie jak tworzenie, usuwanie i zmiana nazw folderów i plików, a także zarządzanie kolejnością elementów w katalogach.
package com.qjproject.liturgicalcalendar.data.repository.FileSystemRepository

import android.util.Log
import com.qjproject.liturgicalcalendar.data.DayData
import com.qjproject.liturgicalcalendar.data.FileSystemItem
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

internal class DirectoryManager(
    private val internalStorageRoot: File,
    private val json: Json
) {
    fun getItems(path: String): List<FileSystemItem> {
        return try {
            val directory = File(internalStorageRoot, path)
            val allFiles = directory.listFiles() ?: return emptyList()

            val itemsOnDisk = allFiles
                .filter { it.name != ORDER_FILE_NAME }
                .map {
                    val itemName = if (it.isDirectory) it.name else it.nameWithoutExtension
                    val itemPath = it.absolutePath.removePrefix(internalStorageRoot.absolutePath + "/")
                    FileSystemItem(name = itemName, isDirectory = it.isDirectory, path = itemPath)
                }.toMutableList()

            val orderFile = File(directory, ORDER_FILE_NAME)
            if (orderFile.exists()) {
                try {
                    val storedOrder = json.decodeFromString<DirectoryOrder>(orderFile.readText()).order
                    val itemsOnDiskMap = itemsOnDisk.associateBy { it.name }
                    val orderedItems = storedOrder.mapNotNull { itemsOnDiskMap[it] }.toMutableList()
                    val remainingItems = itemsOnDisk.filterNot { storedOrder.contains(it.name) }
                    orderedItems.addAll(remainingItems.sortedWith(fileSystemItemNaturalComparator))
                    orderedItems
                } catch (e: Exception) {
                    Log.e("DirectoryManager", "Błąd odczytu pliku kolejności dla $path. Sortowanie naturalne.", e)
                    itemsOnDisk.sortedWith(fileSystemItemNaturalComparator)
                }
            } else {
                itemsOnDisk.sortedWith(fileSystemItemNaturalComparator)
            }
        } catch (e: Exception) {
            Log.e("DirectoryManager", "Błąd podczas pobierania elementów z $path", e)
            emptyList()
        }
    }

    fun saveOrder(path: String, orderedNames: List<String>): Result<Unit> {
        return try {
            val directory = File(internalStorageRoot, path)
            if (!directory.exists()) directory.mkdirs()
            val orderFile = File(directory, ORDER_FILE_NAME)
            val directoryOrder = DirectoryOrder(order = orderedNames)
            val jsonString = json.encodeToString(DirectoryOrder.serializer(), directoryOrder)
            orderFile.writeText(jsonString)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("DirectoryManager", "Błąd zapisu kolejności w $path", e)
            Result.failure(e)
        }
    }

    fun createFolder(path: String, folderName: String): Result<Unit> {
        return try {
            val newFolder = File(File(internalStorageRoot, path), folderName)
            if (newFolder.exists()) return Result.failure(IOException("Folder o tej nazwie już istnieje."))
            if (newFolder.mkdirs()) Result.success(Unit)
            else Result.failure(IOException("Nie udało się utworzyć folderu."))
        } catch (e: Exception) {
            Log.e("DirectoryManager", "Błąd tworzenia folderu '$folderName' w '$path'", e)
            Result.failure(e)
        }
    }

    fun createDayFile(path: String, fileName: String, url: String?): Result<String> {
        return try {
            val fullPath = File(internalStorageRoot, path).apply { mkdirs() }
            val newFile = File(fullPath, "$fileName.json")
            if (newFile.exists()) return Result.failure(IOException("Plik o tej nazwie już istnieje."))

            val dayData = DayData(
                urlCzytania = url?.ifBlank { null },
                tytulDnia = fileName,
                czyDatowany = false,
                czytania = emptyList(),
                piesniSugerowane = emptyList()
            )
            val jsonString = json.encodeToString(DayData.serializer(), dayData)
            newFile.writeText(jsonString)
            Result.success(newFile.name)
        } catch (e: Exception) {
            Log.e("DirectoryManager", "Błąd tworzenia pliku '$fileName' w '$path'", e)
            Result.failure(e)
        }
    }

    fun deleteItem(itemPath: String): Result<Unit> {
        return try {
            val fileToDelete = File(internalStorageRoot, itemPath)
            if (!fileToDelete.exists()) return Result.failure(FileNotFoundException("Element nie istnieje: $itemPath"))
            if (fileToDelete.deleteRecursively()) Result.success(Unit)
            else Result.failure(IOException("Nie udało się usunąć elementu: $itemPath"))
        } catch (e: Exception) {
            Log.e("DirectoryManager", "Błąd podczas usuwania $itemPath", e)
            Result.failure(e)
        }
    }

    fun renameItem(itemPath: String, newName: String): Result<String> {
        return try {
            val oldFile = File(internalStorageRoot, itemPath)
            if (!oldFile.exists()) return Result.failure(FileNotFoundException("Element nie istnieje: $itemPath"))

            val newFile = File(oldFile.parentFile, if (oldFile.isDirectory) newName else "$newName.json")
            if (newFile.exists()) return Result.failure(IOException("Element o nazwie '$newName' już istnieje."))

            if (oldFile.renameTo(newFile)) {
                if (newFile.isFile && newFile.extension == "json") {
                    try {
                        val dayData = json.decodeFromString<DayData>(newFile.readText())
                        val updatedDayData = dayData.copy(tytulDnia = newName)
                        newFile.writeText(json.encodeToString(DayData.serializer(), updatedDayData))
                    } catch (e: Exception) {
                        Log.w("DirectoryManager", "Nie udało się zaktualizować tytułu w pliku ${newFile.name}", e)
                    }
                }
                Result.success(newFile.absolutePath.removePrefix(internalStorageRoot.absolutePath + "/"))
            } else {
                Result.failure(IOException("Nie udało się zmienić nazwy."))
            }
        } catch (e: Exception) {
            Log.e("DirectoryManager", "Błąd zmiany nazwy dla $itemPath", e)
            Result.failure(e)
        }
    }
}