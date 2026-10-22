package com.rizmi.cleanshield.data.repository

import com.rizmi.cleanshield.data.local.PreferencesManager
import com.rizmi.cleanshield.domain.model.DnsResolverConfig
import com.rizmi.cleanshield.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class PreferencesRepositoryImpl(
    private val preferencesManager: PreferencesManager
) : PreferencesRepository {

    override val customResolverIp: Flow<String> = preferencesManager.customResolverIp
    override val loggingEnabled: Flow<Boolean> = preferencesManager.loggingEnabled
    override val notificationsEnabled: Flow<Boolean> = preferencesManager.notificationsEnabled
    override val protectionActive: Flow<Boolean> = preferencesManager.protectionActive
    override val activeSinceTimestamp: Flow<Long> = preferencesManager.activeSinceTimestamp

    override val selectedResolver: Flow<DnsResolverConfig> = combine(
        preferencesManager.selectedResolverId,
        preferencesManager.customResolverIp
    ) { id, customIp ->
        when (id) {
            DnsResolverConfig.CLOUDFLARE.id -> DnsResolverConfig.CLOUDFLARE
            DnsResolverConfig.GOOGLE.id -> DnsResolverConfig.GOOGLE
            DnsResolverConfig.QUAD9.id -> DnsResolverConfig.QUAD9
            DnsResolverConfig.ADGUARD.id -> DnsResolverConfig.ADGUARD
            "custom" -> {
                val ip = customIp.ifBlank { "94.140.14.14" }
                DnsResolverConfig("custom", "Custom Resolver ($ip)", ip, 53, "User defined DNS server", isCustom = true)
            }
            else -> DnsResolverConfig.ADGUARD
        }
    }

    override suspend fun setResolver(resolverId: String, customIp: String?) {
        preferencesManager.setSelectedResolver(resolverId, customIp)
    }

    override suspend fun setLoggingEnabled(enabled: Boolean) {
        preferencesManager.setLoggingEnabled(enabled)
    }

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        preferencesManager.setNotificationsEnabled(enabled)
    }

    override suspend fun setProtectionActive(active: Boolean, timestamp: Long) {
        preferencesManager.setProtectionActive(active, timestamp)
    }
}
