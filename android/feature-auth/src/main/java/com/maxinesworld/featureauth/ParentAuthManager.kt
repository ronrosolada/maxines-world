package com.maxinesworld.featureauth

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
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
        private const val PBKDF2_ITERATIONS = 120_000
        private const val PBKDF2_KEY_LENGTH = 256
    }

    val displayName: Flow<String?> = context.authDataStore.data.map { it[KEY_DISPLAY_NAME] }

    /**
     * Creates or retrieves a per-install random 16-byte salt.
     * Generated once with SecureRandom and persisted in DataStore.
     */
    private suspend fun getOrCreateSalt(): String {
        val existing = context.authDataStore.data.first()[KEY_PIN_SALT]
        if (existing != null) return existing
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
            .joinToString("") { "%02x".format(it) }
        context.authDataStore.edit { it[KEY_PIN_SALT] = salt }
        return salt
    }

    /**
     * Hashes a PIN using PBKDF2-HMAC-SHA256 with the per-install salt.
     * 120,000 iterations — ~100ms on modern devices, sufficient to
     * make brute-force of a 6-digit PIN impractical from a leaked hash.
     */
    private fun hashPin(pin: String, saltHex: String): String {
        val salt = saltHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        val spec = PBEKeySpec(pin.toCharArray(), salt, PBKDF2_ITERATIONS, PBKDF2_KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded.joinToString("") { "%02x".format(it) }
    }

    suspend fun getPinHash(): String? =
        context.authDataStore.data.first()[KEY_PIN_HASH]

    suspend fun setPin(pin: String, displayName: String) {
        val salt = getOrCreateSalt()
        val hash = hashPin(pin, salt)
        context.authDataStore.edit { prefs ->
            prefs[KEY_PIN_HASH] = hash
            prefs[KEY_DISPLAY_NAME] = displayName
        }
    }

    /**
     * Verifies a PIN attempt against the stored hash.
     * Uses MessageDigest.isEqual for constant-time comparison to
     * prevent timing attacks on PIN length/correctness.
     */
    suspend fun verifyPin(input: String): Boolean {
        val storedHash = getPinHash() ?: return false
        val salt = getOrCreateSalt()
        val inputHash = hashPin(input, salt)
        return MessageDigest.isEqual(
            inputHash.toByteArray(Charsets.UTF_8),
            storedHash.toByteArray(Charsets.UTF_8)
        )
    }

    /**
     * Clears all authentication data — used for parent "Delete all data" action.
     */
    suspend fun clearAll() {
        context.authDataStore.edit { it.clear() }
    }
}
