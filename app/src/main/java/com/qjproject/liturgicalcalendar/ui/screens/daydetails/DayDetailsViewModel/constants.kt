// Ścieżka: app/src/main/java/com/qjproject/liturgicalcalendar/ui/screens/daydetails/daydetailsviewmodel/constants.kt
// Opis: Plik ten zawiera stałe wartości i konfiguracje używane w obrębie komponentu DayDetails. Centralizuje definicje, które są niezmienne i współdzielone w różnych częściach logiki i UI tego ekranu.
package com.qjproject.liturgicalcalendar.ui.screens.daydetails.daydetailsviewmodel

val songMomentOrderMap: LinkedHashMap<String, String> = linkedMapOf(
    "wejscie" to "Wejście",
    "ofiarowanie" to "Ofiarowanie",
    "komunia" to "Komunia",
    "uwielbienie" to "Uwielbienie",
    "rozeslanie" to "Rozesłanie",
    "ogolne" to "Ogólne"
)