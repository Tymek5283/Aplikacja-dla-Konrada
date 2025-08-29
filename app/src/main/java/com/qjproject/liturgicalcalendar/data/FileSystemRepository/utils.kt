// Ścieżka: C:\Users\blzej\Desktop\Aplikacja dla studenta\Aplikacja-dla-Konrada\app\src\main\java\com\qjproject\liturgicalcalendar\data\repository\FileSystemRepository\utils.kt
// Opis: Plik pomocniczy zawierający stałe, klasy danych oraz komparatory używane w całym module repozytorium do zarządzania plikami.
package com.qjproject.liturgicalcalendar.data.repository.FileSystemRepository

import com.qjproject.liturgicalcalendar.data.FileSystemItem
import kotlinx.serialization.Serializable
import java.io.File

internal const val ORDER_FILE_NAME = ".directory_order.json"

@Serializable
internal data class DirectoryOrder(val order: List<String>)

internal val fileSystemItemNaturalComparator = compareBy<FileSystemItem> { !it.isDirectory }
    .then(Comparator { a, b ->
        val numA = a.name.takeWhile { it.isDigit() }.toIntOrNull()
        val numB = b.name.takeWhile { it.isDigit() }.toIntOrNull()

        if (numA != null && numB != null) {
            val numCompare = numA.compareTo(numB)
            if (numCompare != 0) numCompare else a.name.compareTo(b.name, ignoreCase = true)
        } else if (numA != null) {
            -1
        } else if (numB != null) {
            1
        } else {
            a.name.compareTo(b.name, ignoreCase = true)
        }
    })