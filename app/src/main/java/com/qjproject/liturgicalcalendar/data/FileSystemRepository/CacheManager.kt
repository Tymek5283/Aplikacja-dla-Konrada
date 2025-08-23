// Ścieżka: C:\Users\blzej\Desktop\Aplikacja dla studenta\Aplikacja-dla-Konrada\app\src\main\java\com\qjproject\liturgicalcalendar\data\repository\FileSystemRepository\CacheManager.kt
// Opis: Zarządza buforowaniem list pieśni i kategorii w pamięci, aby zminimalizować operacje odczytu z dysku i przyspieszyć działanie aplikacji.
package com.qjproject.liturgicalcalendar.data.repository.FileSystemRepository

import com.qjproject.liturgicalcalendar.data.Category
import com.qjproject.liturgicalcalendar.data.Song

internal class CacheManager {
    var songListCache: List<Song>? = null
        private set
    var categoryListCache: List<Category>? = null
        private set

    fun setSongCache(songs: List<Song>) {
        songListCache = songs
    }

    fun setCategoryCache(categories: List<Category>) {
        categoryListCache = categories
    }

    fun invalidateSongCache() {
        songListCache = null
    }

    fun invalidateCategoryCache() {
        categoryListCache = null
    }
}