package com.playfieldportal.feature.xmb.viewmodel

import com.playfieldportal.core.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class CategoryRootRowsTest {
    private val games = Category("games", "Game", "ic_games", type = CategoryType.BUILT_IN, position = 0, isGamingCategory = true)
    private val root = listOf(XMBItem("favorites", "Favorites"))
    private fun state() = XMBUiState(categories = listOf(games), currentItems = root)
    @Test fun `returning to a category has rows immediately without waiting for Room`() {
        val cache = CategoryRootRows()
        cache.rememberRoot(state())
        assertSame(root, cache.forCategory(games))
    }
    @Test fun `folder and filtered rows cannot overwrite the root snapshot`() {
        val cache = CategoryRootRows()
        cache.rememberRoot(state())
        cache.rememberRoot(state().copy(selectedPlatformId = "psp", currentItems = listOf(XMBItem("game", "Game"))))
        cache.rememberRoot(state().copy(gameQuery = "different", currentItems = emptyList()))
        assertSame(root, cache.forCategory(games))
    }
    @Test fun `a changed category definition and first visit do not reuse unrelated rows`() {
        val cache = CategoryRootRows()
        cache.rememberRoot(state())
        assertTrue(cache.forCategory(games.copy(isGamingCategory = false)).isEmpty())
        assertTrue(cache.forCategory(null).isEmpty())
    }
}
