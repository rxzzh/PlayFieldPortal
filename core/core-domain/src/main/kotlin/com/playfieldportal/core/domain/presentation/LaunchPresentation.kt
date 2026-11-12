package com.playfieldportal.core.domain.presentation

/** Game presentation seam; implementations finish (or time out) before the game opens. */
fun interface LaunchPresentation {
    suspend fun awaitPresentation(title: String)
}
