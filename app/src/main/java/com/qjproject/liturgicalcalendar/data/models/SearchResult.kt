package com.qjproject.liturgicalcalendar.data.models

import com.qjproject.liturgicalcalendar.data.Song

sealed class SearchResult {
    data class SongResult(
        val song: Song
    ) : SearchResult()
}