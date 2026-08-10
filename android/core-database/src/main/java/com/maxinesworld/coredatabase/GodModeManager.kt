package com.maxinesworld.coredatabase

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.godModeDataStore: DataStore<Preferences> by preferencesDataStore(name = "god_mode_prefs")

/**
 * Parent-controlled preview switch for the gamified reward surfaces.
 *
 * God mode is intentionally a projection, not a reward grant: it never inserts
 * lesson completions, badges, inventory rows, currency, or reward entitlements.
 * The UI that changes it lives behind the authenticated Parent Dashboard.
 */
@Singleton
class GodModeManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private val KEY_ENABLED = booleanPreferencesKey("enabled")
        const val GOD_MODE_REWARD_BREAK_ID = "god-mode-playground"
    }

    val enabled: Flow<Boolean> = context.godModeDataStore.data.map { preferences ->
        preferences[KEY_ENABLED] ?: false
    }

    suspend fun isEnabled(): Boolean = enabled.first()

    suspend fun setEnabled(value: Boolean) {
        context.godModeDataStore.edit { preferences ->
            preferences[KEY_ENABLED] = value
        }
    }
}
