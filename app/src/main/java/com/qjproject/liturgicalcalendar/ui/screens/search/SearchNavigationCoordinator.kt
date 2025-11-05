package com.qjproject.liturgicalcalendar.ui.screens.search

object SearchNavigationCoordinator {
    @Volatile private var pendingTag: String? = null
    @Volatile private var deferBackOnce: Boolean = false

    fun requestOpenTag(tag: String, deferBack: Boolean = true) {
        pendingTag = tag
        deferBackOnce = deferBack
    }

    fun consumePendingTag(): String? {
        val tag = pendingTag
        pendingTag = null
        return tag
    }

    fun consumeDeferBackFlag(): Boolean {
        val flag = deferBackOnce
        deferBackOnce = false
        return flag
    }
}
