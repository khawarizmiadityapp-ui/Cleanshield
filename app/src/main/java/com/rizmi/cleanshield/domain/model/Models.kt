package com.rizmi.cleanshield.domain.model

enum class QueryStatus {
    BLOCKED,
    ALLOWED,
    FORWARDED,
    FAILED
}

data class DnsQueryEvent(
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val domain: String,
    val queryType: String = "A",
    val clientIp: String = "10.255.0.2",
    val status: QueryStatus,
    val blockReason: String? = null,
    val responseTimeMs: Long = 0,
    val upstreamResolver: String? = null
)

data class ProtectionStats(
    val totalQueries: Long = 0,
    val blockedQueries: Long = 0,
    val forwardedQueries: Long = 0,
    val failedQueries: Long = 0,
    val uniqueBlockedDomains: Long = 0,
    val activeBlocklistRules: Int = 0,
    val activeAllowlistRules: Int = 0,
    val activeSinceTimestamp: Long = 0
) {
    val blockPercentage: Double
        get() = if (totalQueries <= 0) 0.0 else (blockedQueries.toDouble() / totalQueries.toDouble()) * 100.0

    val allowedQueries: Long
        get() = forwardedQueries
}

enum class VpnStatus {
    STOPPED,
    STARTING,
    RUNNING,
    STOPPING,
    ERROR
}

data class VpnProtectionState(
    val status: VpnStatus = VpnStatus.STOPPED,
    val activeSince: Long = 0,
    val lastError: String? = null,
    val currentResolver: String = "AdGuard DNS (94.140.14.14)"
)

data class DnsResolverConfig(
    val id: String,
    val name: String,
    val primaryIp: String,
    val port: Int = 53,
    val description: String,
    val isCustom: Boolean = false
) {
    companion object {
        val ADGUARD = DnsResolverConfig("adguard", "AdGuard DNS", "94.140.14.14", 53, "Default ad-blocking upstream (Recommended)")
        val CLOUDFLARE = DnsResolverConfig("cf", "Cloudflare DNS", "1.1.1.1", 53, "Fast and privacy-focused")
        val GOOGLE = DnsResolverConfig("google", "Google Public DNS", "8.8.8.8", 53, "Reliable global infrastructure")
        val QUAD9 = DnsResolverConfig("quad9", "Quad9", "9.9.9.9", 53, "Blocks malicious domains upstream")

        val DEFAULT_LIST = listOf(ADGUARD, CLOUDFLARE, GOOGLE, QUAD9)
    }
}

data class OnlineBlocklistPreset(
    val id: String,
    val name: String,
    val description: String,
    val url: String,
    val estimatedRules: String,
    val isRecommended: Boolean = false
) {
    companion object {
        val PRESETS = listOf(
            OnlineBlocklistPreset(
                id = "oisd_small",
                name = "OISD Basic",
                description = "Zero false-positive mobile ad & telemetry blocker",
                url = "https://small.oisd.nl",
                estimatedRules = "~40,000 rules",
                isRecommended = true
            ),
            OnlineBlocklistPreset(
                id = "adguard_mobile",
                name = "AdGuard Mobile Filter",
                description = "Standard mobile ad-blocking list from AdGuard",
                url = "https://adguardteam.github.io/HostlistsRegistry/assets/filter_1.txt",
                estimatedRules = "~45,000 rules"
            ),
            OnlineBlocklistPreset(
                id = "steven_black",
                name = "StevenBlack Unified",
                description = "Comprehensive adware, spyware & tracker list",
                url = "https://raw.githubusercontent.com/StevenBlack/hosts/master/hosts",
                estimatedRules = "~60,000 rules"
            ),
            OnlineBlocklistPreset(
                id = "peter_lowe",
                name = "Peter Lowe's List",
                description = "Lightweight, ultra-fast ad server list",
                url = "https://pgl.yoyo.org/adservers/serverlist.php?hostformat=hosts&showintro=0&mimetype=plaintext",
                estimatedRules = "~4,000 rules"
            )
        )
    }
}

data class BlocklistSource(
    val id: Long = 0,
    val name: String,
    val uriString: String? = null,
    val fileSizeBytes: Long = 0,
    val totalLines: Int = 0,
    val validCount: Int = 0,
    val invalidCount: Int = 0,
    val duplicateCount: Int = 0,
    val importTimestamp: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)

data class BlocklistPreview(
    val fileName: String,
    val totalLines: Int,
    val validCount: Int,
    val invalidCount: Int,
    val duplicateCount: Int,
    val sampleValidDomains: List<String>,
    val parsedDomains: Set<String>
)

data class AllowlistRule(
    val id: Long = 0,
    val domain: String,
    val addedTimestamp: Long = System.currentTimeMillis(),
    val note: String? = null
)
