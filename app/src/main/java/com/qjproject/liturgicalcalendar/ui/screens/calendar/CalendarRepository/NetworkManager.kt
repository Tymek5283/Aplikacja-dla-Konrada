// Ścieżka: C:\Users\blzej\Desktop\Aplikacja dla studenta\Aplikacja-dla-Konrada\app\src\main\java\com\qjproject\liturgicalcalendar\ui\screens\calendar\CalendarRepository\NetworkManager.kt
// Opis: Odpowiada za wszystkie operacje sieciowe, w tym pobieranie danych kalendarza w formacie ICS z zewnętrznego serwera oraz sprawdzanie dostępności połączenia internetowego.
package com.qjproject.liturgicalcalendar.ui.screens.calendar.CalendarRepository

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException

internal class NetworkManager(private val context: Context) {

    fun downloadIcsData(year: Int): Result<String> {
        if (!isNetworkAvailable()) return Result.failure(IOException("Brak połączenia z internetem."))

        var connection: HttpURLConnection? = null
        try {
            val url = URL("https://gcatholic.org/calendar/ics/$year-pl-PL.ics?v=3")
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000

            return if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val icsContent = connection.inputStream.bufferedReader().readText()
                Result.success(icsContent)
            } else {
                Result.failure(IOException("Serwer odpowiedział kodem ${connection.responseCode} dla roku $year."))
            }
        } catch (e: Exception) {
            val errorMessage = when (e) {
                is UnknownHostException -> "Brak połączenia z internetem."
                is SocketTimeoutException -> "Przekroczono limit czasu odpowiedzi serwera."
                else -> e.localizedMessage ?: e.javaClass.simpleName
            }
            return Result.failure(IOException(errorMessage))
        } finally {
            connection?.disconnect()
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }
}