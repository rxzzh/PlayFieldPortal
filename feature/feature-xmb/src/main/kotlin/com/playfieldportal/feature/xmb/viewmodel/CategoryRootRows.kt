package com.playfieldportal.feature.xmb.viewmodel

import com.playfieldportal.core.domain.model.Category

/** Last root rows per category, used only until its live data refresh arrives. Never reuse a
 * folder or filtered result as a category root; a category's changed definition misses the cache. */
internal class CategoryRootRows {
    private val rows = mutableMapOf<Category, List<XMBItem>>()
    fun rememberRoot(state: XMBUiState) {
        if (state.isInSubItem || state.gameQuery.isNotBlank() || state.currentItems.isEmpty()) return
        val category = state.categories.getOrNull(state.selectedCategoryIndex) ?: return
        rows[category] = state.currentItems
    }
    fun forCategory(category: Category?): List<XMBItem> = rows[category].orEmpty()
}
