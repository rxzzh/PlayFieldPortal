package com.playfieldportal.core.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.playfieldportal.core.data.datastore.pfpDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Confirm/tap launches a game directly by default. X opens Game Detail for editing;
 * turning direct launch off remains an explicit compatibility preference.
 */
@Singleton
class GameLaunchPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val directLaunchFlow: Flow<Boolean> = context.pfpDataStore.data
        .map { it[KEY_DIRECT_LAUNCH] ?: true }

    suspend fun setDirectLaunch(enabled: Boolean) =
        context.pfpDataStore.edit { it[KEY_DIRECT_LAUNCH] = enabled }

    companion object {
        private val KEY_DIRECT_LAUNCH = booleanPreferencesKey("pref_direct_game_launch")
    }
}
