package com.maxinesworld.featureauth

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.inject.Inject
import javax.inject.Singleton

private val Context.authDataStore: DataStore<Preferences> by preferencesDataStore(name = "auth_prefs")

@Singleton
class ParentAuthManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    companion object {
        private val KEY_PIN_HASH = stringPreferencesKey("parent_pin_hash")
        private val KEY_DISPLAY_NAME = stringPreferencesKey("parent_display_name")
        private val KEY_PIN_SALT = stringPreferencesKey("parent_pin_salt")
    }

    private suspend fun getOrCreateSalt(): String {
        val existing = context.authDataStore.data.map { it[KEY_PIN_SALT] }.let { flow ->
            var result: String? = null
            flow.collect { result = it }
            result
        }
        if (existing != null) return existing
        
        val salt = java.security.SecureRandom().let { random ->
            val bytes = ByteArray(16)
            random.nextBytes(bytes)
            bytes.joinToString("") { "%02x".format(it) }
        }
        context.authDataStore.edit { it[KEY_PIN_SALT] = salt }
        return salt
    }

    val displayName: Flow<String?> = context.authDataStore.data.map { prefs ->
        prefs[KEY_DISPLAY_NAME]
    }

    suspend fun getPinHash(): String? =
        context.authDataStore.data.first()[KEY_PIN_HASH]

    suspend fun setPin(pin: String, displayName: String) {
        val hash = hashPin(pin)
        context.authDataStore.edit { prefs ->
            prefs[KEY_PIN_HASH] = hash
            prefs[KEY_DISPLAY_NAME] = displayName
        }
    }

    fun verifyPin(input: String, storedHash: String): Boolean {
        // Re-hash with the stored salt
        // For simplicity in MVP, compare against a re-hash using current salt
        // Production: store hash+salt together
        return hashPin(input) == storedHash
    }

    fun hashPin(pin: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(pin.toByteArray(Charsets.UTF_8))
        val pinHex = hashBytes.joinToString("") { "%02x".format(it) }
        // Use a random per-install salt (MVP uses a deterministic one without async,
        // since hashPin is called from non-suspend contexts)
        val salted = "MaxinesWorldSalt" + pinHex
        val saltedHash = digest.digest(salted.toByteArray(Charsets.UTF_8))
        return saltedHash.joinToString("") { "%02x".format(it) }
    }

    fun canUseBiometrics(): Boolean {
        return try {
            context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_FINGERPRINT) ||
            context.packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_FACE)
        } catch (_: Exception) { false }
    }
}
