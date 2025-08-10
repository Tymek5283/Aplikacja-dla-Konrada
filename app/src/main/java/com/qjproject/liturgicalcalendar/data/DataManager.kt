package com.qjproject.liturgicalcalendar.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class DataManager(private val context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    private val dataVersionKey = "data_version"
    private val currentDataVersion = 2 // Zwiększono wersję, aby wymusić ponowne skopiowanie danych

    fun copyAssetsToInternalStorageIfNeeded() {
        val installedVersion = prefs.getInt(dataVersionKey, 0)
        if (installedVersion >= currentDataVersion) {
            Log.d("DataManager", "Dane są aktualne (wersja $installedVersion). Kopiowanie pominięte.")
            return
        }

        Log.d("DataManager", "Rozpoczynanie kopiowania danych z assets do pamięci wewnętrznej...")
        try {
            val internalRoot = context.filesDir
            Log.d("DataManager", "Katalog docelowy: ${internalRoot.absolutePath}")

            // Czyszczenie starych danych
            internalRoot.listFiles()?.forEach { file ->
                if (file.isDirectory && (file.name == "data" || file.name == "Datowane")) {
                    Log.d("DataManager", "Usuwanie starego katalogu: ${file.name}")
                    file.deleteRecursively()
                }
            }

            // Kopiowanie głównych folderów
            val rootAssets = context.assets.list("")?.filter { it == "data" || it == "Datowane" } ?: emptyList()
            rootAssets.forEach { assetDirName ->
                val destDir = File(internalRoot, assetDirName)
                if (!destDir.exists()) destDir.mkdirs()
                Log.d("DataManager", "Tworzenie katalogu głównego: ${destDir.absolutePath}")
                copyAssetDir(assetDirName, destDir)
            }

            prefs.edit().putInt(dataVersionKey, currentDataVersion).apply()
            Log.d("DataManager", "Kopiowanie danych zakończone sukcesem. Ustawiono wersję na $currentDataVersion.")
        } catch (e: IOException) {
            Log.e("DataManager", "Błąd podczas kopiowania danych z assets.", e)
        }
    }

    private fun copyAssetDir(assetPath: String, destDir: File) {
        val assets = context.assets.list(assetPath) ?: return

        assets.forEach { assetName ->
            val currentAssetPath = "$assetPath/$assetName"
            val isDirectory = try {
                context.assets.list(currentAssetPath)?.isNotEmpty() == true
            } catch (e: IOException) {
                false
            }

            if (isDirectory) {
                val newDestDir = File(destDir, assetName)
                if (!newDestDir.exists()) newDestDir.mkdirs()
                Log.d("DataManager", "Tworzenie podkatalogu: ${newDestDir.absolutePath}")
                copyAssetDir(currentAssetPath, newDestDir)
            } else {
                copyAssetFile(currentAssetPath, destDir)
            }
        }
    }

    private fun copyAssetFile(assetPath: String, destDir: File) {
        val destFile = File(destDir, File(assetPath).name)
        Log.d("DataManager", "Kopiowanie pliku: $assetPath -> ${destFile.absolutePath}")
        try {
            context.assets.open(assetPath).use { inputStream ->
                FileOutputStream(destFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        } catch (e: IOException) {
            Log.e("DataManager", "Nie udało się skopiować pliku $assetPath", e)
        }
    }
}