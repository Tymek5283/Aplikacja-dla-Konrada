package com.qjproject.liturgicalcalendar.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Manager odpowiedzialny za wykrywanie pierwszego uruchomienia aplikacji
 * i zarządzanie operacjami wykonywanymi jednorazowo po instalacji
 */
class FirstRunManager(private val context: Context) {
    
    companion object {
        private const val PREFS_NAME = "first_run_prefs"
        private const val KEY_FIRST_RUN_COMPLETED = "first_run_completed"
        private const val KEY_ASSETS_COPIED = "assets_neumy_copied"
    }
    
    private val sharedPrefs: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    /**
     * Sprawdza czy to pierwszy uruchomienie aplikacji
     */
    fun isFirstRun(): Boolean {
        return !sharedPrefs.getBoolean(KEY_FIRST_RUN_COMPLETED, false)
    }
    
    /**
     * Sprawdza czy pliki PDF z assets zostały już skopiowane
     */
    fun areAssetsCopied(): Boolean {
        return sharedPrefs.getBoolean(KEY_ASSETS_COPIED, false)
    }
    
    /**
     * Oznacza pierwszy uruchomienie jako zakończony
     */
    fun markFirstRunCompleted() {
        sharedPrefs.edit()
            .putBoolean(KEY_FIRST_RUN_COMPLETED, true)
            .apply()
    }
    
    /**
     * Oznacza kopiowanie plików z assets jako zakończone
     */
    fun markAssetsCopied() {
        sharedPrefs.edit()
            .putBoolean(KEY_ASSETS_COPIED, true)
            .apply()
    }
    
    /**
     * Resetuje ustawienia pierwszego uruchomienia (do celów testowych)
     */
    fun resetFirstRunStatus() {
        sharedPrefs.edit()
            .putBoolean(KEY_FIRST_RUN_COMPLETED, false)
            .putBoolean(KEY_ASSETS_COPIED, false)
            .apply()
    }
}
