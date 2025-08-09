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
    private val currentDataVersion = 1

    fun copyAssetsToInternalStorageIfNeeded() {
        val installedVersion = prefs.getInt(dataVersionKey, 0)
        if (installedVersion >= currentDataVersion) {
            Log.d("DataManager", "Dane są aktualne (wersja $installedVersion). Kopiowanie pominięte.")
            return
        }

        Log.d("DataManager", "Wykryto nową wersję danych (lub pierwszy raz). Rozpoczynanie kopiowania...")
        try {
            val internalRoot = context.filesDir
            internalRoot.listFiles()?.forEach { file ->
                if (file.isDirectory && (file.name == "data" || file.name == "Datowane")) {
                    file.deleteRecursively()
                }
            }

            val rootAssets = context.assets.list("")?.filter { it == "data" || it == "Datowane" } ?: emptyList()
            rootAssets.forEach { assetDir ->
                copyAssetDir(assetDir, internalRoot)
            }

            prefs.edit().putInt(dataVersionKey, currentDataVersion).apply()
            Log.d("DataManager", "Kopiowanie danych zakończone sukcesem. Ustawiono wersję na $currentDataVersion.")
        } catch (e: IOException) {
            Log.e("DataManager", "Błąd podczas kopiowania danych z assets.", e)
        }
    }

    private fun copyAssetDir(path: String, destDir: File) {
        val assets = context.assets.list(path) ?: return
        val newDir = File(destDir, path.substringAfterLast('/'))
        if (!newDir.exists()) {
            newDir.mkdirs()
        }

        assets.forEach { assetName ->
            val assetPath = "$path/$assetName"
            val isDirectory = try {
                context.assets.list(assetPath)?.isNotEmpty() ?: false
            } catch (e: IOException) {
                false
            }

            if (isDirectory) {
                copyAssetDir(assetPath, newDir)
            } else {
                copyAssetFile(assetPath, newDir)
            }
        }
    }

    private fun copyAssetFile(assetPath: String, destDir: File) {
        val destFile = File(destDir, File(assetPath).name)
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