package com.maxinesworld.featurelessonplayer

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.lessonNarrationDataStore by preferencesDataStore(name = "lesson_narration_prefs")

object NarrationPreferences {
    private val enabledKey = booleanPreferencesKey("narration_enabled")

    fun enabled(context: Context): Flow<Boolean> =
        context.lessonNarrationDataStore.data.map { preferences ->
            preferences[enabledKey] ?: true
        }

    suspend fun setEnabled(context: Context, enabled: Boolean) {
        context.lessonNarrationDataStore.edit { preferences ->
            preferences[enabledKey] = enabled
        }
    }
}
