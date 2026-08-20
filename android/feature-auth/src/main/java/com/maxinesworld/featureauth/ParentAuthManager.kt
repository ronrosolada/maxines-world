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
import java.security.MessageDigest
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

private val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

@Singleton
class ParentAuthManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val KEY_PIN_HASH = stringPreferencesKey("parent_pin_hash")
        private val KEY_PIN_SALT = stringPreferencesKey("parent_pin_salt")
        private val KEY_DISPLAY_NAME = stringPreferencesKey("parent_display_name")
        private val KEY_FAILED_ATTEMPTS = intPreferencesKey("pin_failed_attempts")
        private val KEY_LOCKED_UNTIL = longPreferencesKey("pin_locked_until_epoch_millis")

        const val DEFAULT_PARENT_NAME = "Parent"

        /** Brute-force policy: lock after 5 consecutive failures, escalate. */
        const val MAX_ATTEMPTS_BEFORE_LOCK = 5
        const val BASE_LOCKOUT_MILLIS = 30_000L
        const val MAX_LOCKOUT_MILLIS = 300_000L
    }

    val displayName: Flow<String?> = context.authDataStore.data.map { it[KEY_DISPLAY_NAME] }

    /** Returns true only after a caregiver has configured a PIN on this install. */
    suspend fun hasPin(): Boolean =
        context.authDataStore.data.first()[KEY_PIN_HASH] != null

    /** Returns the configured PIN hash; fresh installs intentionally have none. */
    suspend fun getPinHash(): String =
        context.authDataStore.data.first()[KEY_PIN_HASH]
            ?: throw IllegalStateException("Parent PIN is not configured")

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

    /** Invalidates the PIN and lockout counters without touching child data. */
    suspend fun resetPinOnly() {
        context.authDataStore.edit { prefs ->
            prefs.remove(KEY_PIN_HASH)
            prefs.remove(KEY_PIN_SALT)
            prefs.remove(KEY_FAILED_ATTEMPTS)
            prefs.remove(KEY_LOCKED_UNTIL)
        }
    }

    private fun createSalt(): String = ByteArray(16).also { SecureRandom().nextBytes(it) }
        .joinToString("") { "%02x".format(it) }

    private suspend fun getOrCreateSalt(): String {
        context.authDataStore.data.first()[KEY_PIN_SALT]?.let { return it }
        val salt = createSalt()
        context.authDataStore.edit { it[KEY_PIN_SALT] = salt }
        return salt
    }

    suspend fun setPin(pin: String, displayName: String) {
        val salt = getOrCreateSalt()
        val hash = hashPin(pin, salt)
        context.authDataStore.edit { prefs ->
            prefs[KEY_PIN_HASH] = hash
            prefs[KEY_DISPLAY_NAME] = displayName
        }
    }

    suspend fun verifyPin(input: String): Boolean {
        val storedHash = runCatching { getPinHash() }.getOrNull() ?: return false
        val salt = try { getOrCreateSalt() } catch (_: Exception) { return false }
        val inputHash = try { hashPin(input, salt) } catch (_: Exception) { return false }
        return MessageDigest.isEqual(
            inputHash.toByteArray(Charsets.UTF_8),
            storedHash.toByteArray(Charsets.UTF_8)
        )
    }

    private fun hashPin(pin: String, saltHex: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val salt = saltHex.toByteArray(Charsets.UTF_8)
        digest.update(salt)
        digest.update(pin.toByteArray(Charsets.UTF_8))
        // Double-hash for basic stretching
        val first = digest.digest()
        val second = MessageDigest.getInstance("SHA-256").digest(first)
        return second.joinToString("") { "%02x".format(it) }
    }

    suspend fun clearAll() {
        context.authDataStore.edit { it.clear() }
    }
}
