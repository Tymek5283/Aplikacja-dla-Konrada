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
    // Zwiększ tę wersję, jeśli zaktualizujesz pliki w assets, aby wymusić ponowne kopiowanie
    private val currentDataVersion = 1

    fun copyAssetsToInternalStorageIfNeeded() {
        val installedVersion = prefs.getInt(dataVersionKey, 0)
        if (installedVersion >= currentDataVersion) {
            Log.d("DataManager", "Dane są aktualne (wersja $installedVersion). Kopiowanie pominięte.")
            return
        }

        Log.d("DataManager", "Wykryto nową wersję danych (lub pierwszy raz). Rozpoczynanie kopiowania...")
        try {
            // Wyczyść stare dane przed kopiowaniem nowych
            val internalRoot = context.filesDir
            internalRoot.listFiles()?.forEach { file ->
                if (file.isDirectory && (file.name == "data" || file.name == "Datowane")) {
                    file.deleteRecursively()
                }
            }

            // Kopiuj nowe dane
            val rootAssets = context.assets.list("")?.filter { it == "data" || it == "Datowane" } ?: emptyList()
            rootAssets.forEach { assetDir ->
                copyAssetDir(assetDir, internalRoot)
            }

            // Zaktualizuj wersję danych w SharedPreferences
            prefs.edit().putInt(dataVersionKey, currentDataVersion).apply()
            Log.d("DataManager", "Kopiowanie danych zakończone sukcesem. Ustawiono wersję na $currentDataVersion.")
        } catch (e: IOException) {
            Log.e("DataManager", "Błąd podczas kopiowania danych z assets.", e)
            // W przypadku błędu, nie aktualizujemy wersji, aby spróbować ponownie przy następnym uruchomieniu
        }
    }

    private fun copyAssetDir(path: String, destDir: File) {
        val assets = context.assets.list(path) ?: return
        val newDir = File(destDir, path)
        if (!newDir.exists()) {
            newDir.mkdirs()
        }

        assets.forEach { assetName ->
            val assetPath = "$path/$assetName"
            // Sprawdzamy, czy element jest katalogiem przez próbę wylistowania jego zawartości
            val isDirectory = try {
                context.assets.list(assetPath)?.isNotEmpty() ?: false
            } catch (e: IOException) {
                false
            }

            if (isDirectory) {
                copyAssetDir(assetPath, destDir)
            } else {
                copyAssetFile(assetPath, destDir)
            }
        }
    }

    private fun copyAssetFile(assetPath: String, destDir: File) {
        val relativePath = File(assetPath).parent
        val targetDir = if (relativePath != null) File(destDir, relativePath) else destDir
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        val destFile = File(targetDir, File(assetPath).name)
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