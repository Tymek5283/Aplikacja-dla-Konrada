package com.qjproject.liturgicalcalendar.ui.screens.settings

data class ExportConfiguration(
    val includeSongs: Boolean = true,
    val includeDays: Boolean = true,
    val includeCategories: Boolean = true,
    val includeNeumy: Boolean = true,
    val includeYears: Boolean = true
)
