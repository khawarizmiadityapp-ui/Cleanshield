package com.rizmi.cleanshield.domain.repository

import com.rizmi.cleanshield.domain.model.AllowlistRule
import com.rizmi.cleanshield.domain.model.BlocklistPreview
import com.rizmi.cleanshield.domain.model.BlocklistSource
import com.rizmi.cleanshield.domain.model.DnsQueryEvent
import com.rizmi.cleanshield.domain.model.DnsResolverConfig
import com.rizmi.cleanshield.domain.model.ProtectionStats
import com.rizmi.cleanshield.domain.model.VpnProtectionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.io.InputStream

interface BlocklistRepository {
    fun getAllSources(): Flow<List<BlocklistSource>>
    fun getLatestSource(): Flow<BlocklistSource?>
    fun getTotalBlockedCount(): Flow<Int>
    suspend fun getActiveBlockedDomains(): List<String>
    suspend fun previewBlocklist(inputStream: InputStream, fileName: String): BlocklistPreview
    suspend fun applyBlocklist(preview: BlocklistPreview, uriString: String?, fileSizeBytes: Long): Result<Int>
    suspend fun clearBlocklists()
    suspend fun loadDefaultBlocklist(): Result<Int>
    suspend fun downloadAndApplyBlocklist(url: String, name: String): Result<Int>

    // Allowlist
    fun getAllowlist(): Flow<List<AllowlistRule>>
    fun getAllowlistCount(): Flow<Int>
    suspend fun getActiveAllowedDomains(): List<String>
    suspend fun addAllowedDomain(domain: String, note: String? = null): Boolean
    suspend fun removeAllowedDomain(domain: String): Boolean
    suspend fun clearAllowlist()
}

interface ActivityLogRepository {
    fun getRecentEvents(limit: Int = 100): Flow<List<DnsQueryEvent>>
    fun getEventsByStatus(status: String, limit: Int = 100): Flow<List<DnsQueryEvent>>
    fun getProtectionStats(): Flow<ProtectionStats>
    suspend fun logEvent(event: DnsQueryEvent)
    suspend fun clearHistory()
}

interface PreferencesRepository {
    val selectedResolver: Flow<DnsResolverConfig>
    val customResolverIp: Flow<String>
    val loggingEnabled: Flow<Boolean>
    val notificationsEnabled: Flow<Boolean>
    val protectionActive: Flow<Boolean>
    val activeSinceTimestamp: Flow<Long>

    suspend fun setResolver(resolverId: String, customIp: String? = null)
    suspend fun setLoggingEnabled(enabled: Boolean)
    suspend fun setNotificationsEnabled(enabled: Boolean)
    suspend fun setProtectionActive(active: Boolean, timestamp: Long = System.currentTimeMillis())
}

interface VpnStateRepository {
    val state: StateFlow<VpnProtectionState>
    fun updateState(status: com.rizmi.cleanshield.domain.model.VpnStatus, error: String? = null)
    fun setResolverName(name: String)
}
