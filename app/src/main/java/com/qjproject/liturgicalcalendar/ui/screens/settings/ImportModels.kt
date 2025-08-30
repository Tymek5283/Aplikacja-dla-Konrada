package com.qjproject.liturgicalcalendar.ui.screens.settings

import kotlinx.serialization.Serializable

@Serializable
data class ImportConfiguration(
    val includeDays: Boolean = true,
    val includeSongs: Boolean = true,
    val includeCategories: Boolean = true,
    val includeTags: Boolean = true,
    val includeNeumy: Boolean = true,
    val includeNotes: Boolean = true,
    val includeYears: Boolean = true
)

data class AvailableImportData(
    val hasDays: Boolean = false,
    val hasSongs: Boolean = false,
    val hasCategories: Boolean = false,
    val hasTags: Boolean = false,
    val hasNeumy: Boolean = false,
    val hasNotes: Boolean = false,
    val hasYears: Boolean = false
)

data class ImportPreviewState(
    val availableData: AvailableImportData,
    val configuration: ImportConfiguration,
    val isAnalyzing: Boolean = false,
    val analysisError: String? = null
)
