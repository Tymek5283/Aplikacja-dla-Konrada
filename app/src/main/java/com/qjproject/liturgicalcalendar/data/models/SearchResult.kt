package com.qjproject.liturgicalcalendar.data.models

import com.qjproject.liturgicalcalendar.data.Song

sealed class SearchResult {
    data class DayResult(
        val title: String,
        val path: String
    ) : SearchResult()

    data class SongResult(
        val song: Song
    ) : SearchResult()
}