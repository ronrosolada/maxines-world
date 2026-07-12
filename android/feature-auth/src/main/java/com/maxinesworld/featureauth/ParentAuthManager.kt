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
    }

    val displayName: Flow<String?> = context.authDataStore.data.map { it[KEY_DISPLAY_NAME] }

    suspend fun getPinHash(): String? =
        context.authDataStore.data.first()[KEY_PIN_HASH]

    private suspend fun getOrCreateSalt(): String {
        context.authDataStore.data.first()[KEY_PIN_SALT]?.let { return it }
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
            .joinToString("") { "%02x".format(it) }
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
        val storedHash = getPinHash() ?: return false
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
