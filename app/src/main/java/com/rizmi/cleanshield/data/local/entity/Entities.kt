package com.rizmi.cleanshield.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "dns_events",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["status"]),
        Index(value = ["domain"])
    ]
)
data class DnsEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val domain: String,
    val queryType: String = "A",
    val clientIp: String = "10.255.0.2",
    val status: String, // BLOCKED, ALLOWED, FORWARDED, FAILED
    val blockReason: String? = null,
    val responseTimeMs: Long = 0,
    val upstreamResolver: String? = null
)

@Entity(tableName = "blocklist_sources")
data class BlocklistSourceEntity(
    @PrimaryKey(autoGenerate = true)
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

@Entity(
    tableName = "blocked_domains",
    indices = [
        Index(value = ["domain"], unique = true),
        Index(value = ["sourceId"])
    ]
)
data class BlockedDomainEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val domain: String,
    val sourceId: Long = 0
)

@Entity(
    tableName = "allowed_domains",
    indices = [
        Index(value = ["domain"], unique = true)
    ]
)
data class AllowedDomainEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val domain: String,
    val addedTimestamp: Long = System.currentTimeMillis(),
    val note: String? = null
)
