package com.playfieldportal.feature.xmb.viewmodel

/** Bind touch activation to the row that was drawn, even if Room reorders before dispatch.
 * A UMD slot keeps its row id when its inserted game changes, so id alone is insufficient. */
internal fun List<XMBItem>.indexOfLaunchTarget(target: XMBItem): Int = indexOfFirst {
    it.id == target.id && it.gameId == target.gameId &&
        it.packageName == target.packageName && it.shortcutId == target.shortcutId &&
        it.launchIntentUri == target.launchIntentUri && it.platformId == target.platformId
}
