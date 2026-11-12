package com.playfieldportal.core.domain.presentation

/** Shared launch seam; implementations finish (or time out) before an external app opens. */
fun interface LaunchPresentation {
    suspend fun awaitPresentation(title: String)
}
