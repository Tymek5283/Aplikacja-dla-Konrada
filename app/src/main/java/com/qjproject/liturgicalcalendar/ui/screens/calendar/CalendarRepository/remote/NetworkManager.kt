// Ścieżka: app/src/main/java/com/qjproject/liturgicalcalendar/ui/screens/calendar/CalendarRepository/remote/NetworkManager.kt
// Opis: Zarządza operacjami sieciowymi. Odpowiada za pobieranie danych kalendarza w formacie ICS z serwera gcatholic.org.

package com.qjproject.liturgicalcalendar.ui.screens.calendar.CalendarRepository.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.URL

internal class NetworkManager(private val context: Context) {

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }

    suspend fun downloadIcsData(year: Int): Result<String> = withContext(Dispatchers.IO) {
        if (!isNetworkAvailable()) {
            return@withContext Result.failure(IOException("Brak połączenia z internetem."))
        }
        val urlString = "https://gcatholic.org/calendar/ics/$year-pl-PL.ics?v=3"
        try {
            val url = URL(urlString)
            val content = url.readText(Charsets.UTF_8)
            Log.d("NetworkManager", "Successfully downloaded ICS for year $year.")
            Result.success(content)
        } catch (e: Exception) {
            Log.e("NetworkManager", "Error downloading ICS for year $year", e)
            Result.failure(e)
        }
    }
}