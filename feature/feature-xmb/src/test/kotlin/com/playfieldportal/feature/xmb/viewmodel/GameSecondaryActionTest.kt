package com.playfieldportal.feature.xmb.viewmodel

import com.playfieldportal.core.domain.model.BuiltInCategory
import com.playfieldportal.core.domain.model.Category
import com.playfieldportal.core.domain.model.CategoryType
import org.junit.Assert.*
import org.junit.Test

class GameSecondaryActionTest {
    private val game = XMBItem("game", "PSP game", gameId = 42L, isRealGame = true)
    private fun state(item: XMBItem = game) = XMBUiState(
        categories = listOf(Category(BuiltInCategory.GAMES, "Game", "ic_games", type = CategoryType.BUILT_IN, position = 0, isGamingCategory = true)),
        currentItems = listOf(item), selectedItemIndex = 0, showBootSequence = false,
    )

    @Test fun `X targets details in root favorites console and collection game lists`() {
        for (s in listOf(state(), state().copy(selectedPlatformId = "psp"), state().copy(selectedCollectionId = 7L))) {
            assertEquals(42L, s.gameDetailsTarget)
            assertTrue(s.canUseSecondaryAction)
            assertEquals("Details", s.sortActionLabel)
        }
    }
    @Test fun `Android games use the same details action`() {
        val s = state(game.copy(packageName = "com.example.game", isAndroidApp = true))
        assertEquals(42L, s.gameDetailsTarget)
        assertEquals("Details", s.sortActionLabel)
    }
    @Test fun `a game card or placeholder cannot fall back to X filtering`() {
        for (item in listOf(XMBItem("card", "PSP", platformId = "psp"), XMBItem("empty", "Empty"))) {
            val s = state(item)
            assertNull(s.gameDetailsTarget)
            assertFalse(s.canUseSecondaryAction)
        }
    }
    @Test fun `ordinary apps are not mistaken for games and retain sorting`() {
        val s = state(XMBItem("app", "Chrome", packageName = "com.android.chrome")).copy(
            categories = listOf(Category("network", "Network", "ic_network", type = CategoryType.BUILT_IN, position = 0)),
        )
        assertNull(s.gameDetailsTarget)
        assertTrue(s.canUseSecondaryAction)
        assertEquals("Sort", s.sortActionLabel)
    }
}
