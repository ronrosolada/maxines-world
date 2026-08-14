package com.maxinesworld.featureauth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

@Singleton
class ParentAuthManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        /**
         * Hardcoded parent PIN for this single-family deployment.
         * There is no per-device PIN setup; every install accepts this exact value.
         * Legacy stored PIN data is intentionally ignored.
         */
        const val DEFAULT_PIN = "123456"

        private val KEY_DISPLAY_NAME = stringPreferencesKey("parent_display_name")
        private val KEY_FAILED_ATTEMPTS = intPreferencesKey("pin_failed_attempts")
        private val KEY_LOCKED_UNTIL = longPreferencesKey("pin_locked_until_epoch_millis")

        /** Brute-force policy: lock after 5 consecutive failures, escalate. */
        const val MAX_ATTEMPTS_BEFORE_LOCK = 5
        const val BASE_LOCKOUT_MILLIS = 30_000L
        const val MAX_LOCKOUT_MILLIS = 300_000L
    }

    val displayName: Flow<String?> = context.authDataStore.data.map { it[KEY_DISPLAY_NAME] }


    /** Consecutive failed PIN attempts, persisted across process restarts. */
    suspend fun getFailedAttempts(): Int =
        context.authDataStore.data.first()[KEY_FAILED_ATTEMPTS] ?: 0

    /** Epoch millis until which PIN entry is locked, 0 when not locked. */
    suspend fun getLockedUntilEpochMillis(): Long =
        context.authDataStore.data.first()[KEY_LOCKED_UNTIL] ?: 0L

    /**
     * Records one failed attempt and applies the lockout policy.
     * Returns the new lockout deadline (epoch millis), or 0 if still unlocked.
     * Lockout escalates: 30s, 60s, 120s, 240s, capped at 300s per 5 failures.
     */
    suspend fun recordFailedAttempt(now: Long = System.currentTimeMillis()): Long {
        val attempts = getFailedAttempts() + 1
        var lockedUntil = 0L
        if (attempts >= MAX_ATTEMPTS_BEFORE_LOCK) {
            val lockLevel = attempts / MAX_ATTEMPTS_BEFORE_LOCK
            val duration = (BASE_LOCKOUT_MILLIS shl (lockLevel - 1))
                .coerceAtMost(MAX_LOCKOUT_MILLIS)
            lockedUntil = now + duration
        }
        context.authDataStore.edit { prefs ->
            prefs[KEY_FAILED_ATTEMPTS] = attempts
            if (lockedUntil > 0) prefs[KEY_LOCKED_UNTIL] = lockedUntil
        }
        return lockedUntil
    }

    /** Clears the failure counter and any active lockout (successful login). */
    suspend fun resetFailedAttempts() {
        context.authDataStore.edit { prefs ->
            prefs.remove(KEY_FAILED_ATTEMPTS)
            prefs.remove(KEY_LOCKED_UNTIL)
        }
    }

    suspend fun verifyPin(input: String): Boolean =
        input == DEFAULT_PIN

    suspend fun clearAll() {
        context.authDataStore.edit { it.clear() }
    }
}
