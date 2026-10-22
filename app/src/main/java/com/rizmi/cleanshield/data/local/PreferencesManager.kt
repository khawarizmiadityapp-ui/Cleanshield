package com.rizmi.cleanshield.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.rizmi.cleanshield.domain.model.DnsResolverConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "cleanshield_prefs")

class PreferencesManager(private val context: Context) {

    private object PreferencesKeys {
        val SELECTED_RESOLVER_ID = stringPreferencesKey("selected_resolver_id")
        val CUSTOM_RESOLVER_IP = stringPreferencesKey("custom_resolver_ip")
        val LOGGING_ENABLED = booleanPreferencesKey("logging_enabled")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val PROTECTION_ACTIVE = booleanPreferencesKey("protection_active")
        val ACTIVE_SINCE_TIMESTAMP = longPreferencesKey("active_since_timestamp")
    }

    val selectedResolverId: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.SELECTED_RESOLVER_ID] ?: DnsResolverConfig.ADGUARD.id
    }

    val customResolverIp: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.CUSTOM_RESOLVER_IP] ?: ""
    }

    val loggingEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.LOGGING_ENABLED] ?: true
    }

    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: true
    }

    val protectionActive: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.PROTECTION_ACTIVE] ?: false
    }

    val activeSinceTimestamp: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.ACTIVE_SINCE_TIMESTAMP] ?: 0L
    }

    suspend fun setSelectedResolver(resolverId: String, customIp: String? = null) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.SELECTED_RESOLVER_ID] = resolverId
            if (customIp != null) {
                prefs[PreferencesKeys.CUSTOM_RESOLVER_IP] = customIp
            }
        }
    }

    suspend fun setLoggingEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.LOGGING_ENABLED] = enabled
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun setProtectionActive(active: Boolean, timestamp: Long = System.currentTimeMillis()) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.PROTECTION_ACTIVE] = active
            if (active) {
                prefs[PreferencesKeys.ACTIVE_SINCE_TIMESTAMP] = timestamp
            } else {
                prefs[PreferencesKeys.ACTIVE_SINCE_TIMESTAMP] = 0L
            }
        }
    }
}
