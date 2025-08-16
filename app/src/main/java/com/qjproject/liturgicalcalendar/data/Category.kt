package com.qjproject.liturgicalcalendar.data

import kotlinx.serialization.Serializable

@Serializable
data class Category(
    val nazwa: String,
    val skrot: String
)