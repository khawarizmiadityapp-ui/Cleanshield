package com.rizmi.cleanshield.data.local.dao

import androidx.room.*
import com.rizmi.cleanshield.data.local.entity.AllowedDomainEntity
import com.rizmi.cleanshield.data.local.entity.BlockedDomainEntity
import com.rizmi.cleanshield.data.local.entity.BlocklistSourceEntity
import com.rizmi.cleanshield.data.local.entity.DnsEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DnsEventDao {
    @Insert
    fun insert(event: DnsEventEntity): Long

    @Insert
    fun insertAll(events: List<DnsEventEntity>): List<Long>

    @Query("SELECT * FROM dns_events ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentEvents(limit: Int = 100): Flow<List<DnsEventEntity>>

    @Query("SELECT * FROM dns_events WHERE status = :status ORDER BY timestamp DESC LIMIT :limit")
    fun getEventsByStatus(status: String, limit: Int = 100): Flow<List<DnsEventEntity>>

    @Query("SELECT COUNT(*) FROM dns_events")
    fun getTotalCount(): Flow<Long>

    @Query("SELECT COUNT(*) FROM dns_events WHERE status = 'BLOCKED'")
    fun getBlockedCount(): Flow<Long>

    @Query("SELECT COUNT(*) FROM dns_events WHERE status IN ('ALLOWED', 'FORWARDED')")
    fun getForwardedCount(): Flow<Long>

    @Query("SELECT COUNT(*) FROM dns_events WHERE status = 'FAILED'")
    fun getFailedCount(): Flow<Long>

    @Query("SELECT COUNT(DISTINCT domain) FROM dns_events WHERE status = 'BLOCKED'")
    fun getUniqueBlockedDomainsCount(): Flow<Long>

    @Query("DELETE FROM dns_events")
    fun clearAll(): Int
}

@Dao
interface BlocklistDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertSource(source: BlocklistSourceEntity): Long

    @Query("SELECT * FROM blocklist_sources ORDER BY importTimestamp DESC")
    fun getAllSources(): Flow<List<BlocklistSourceEntity>>

    @Query("SELECT * FROM blocklist_sources ORDER BY importTimestamp DESC LIMIT 1")
    fun getLatestSource(): Flow<BlocklistSourceEntity?>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertDomains(domains: List<BlockedDomainEntity>): List<Long>

    @Query("SELECT domain FROM blocked_domains")
    fun getAllBlockedDomainsSync(): List<String>

    @Query("SELECT COUNT(*) FROM blocked_domains")
    fun getTotalBlockedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM blocked_domains")
    fun getTotalBlockedCountSync(): Int

    @Query("DELETE FROM blocked_domains WHERE sourceId = :sourceId")
    fun deleteDomainsForSource(sourceId: Long): Int

    @Query("DELETE FROM blocked_domains")
    fun clearAllBlockedDomains(): Int

    @Query("DELETE FROM blocklist_sources WHERE id = :sourceId")
    fun deleteSource(sourceId: Long): Int

    @Query("DELETE FROM blocklist_sources")
    fun clearAllSources(): Int
}

@Dao
interface AllowlistDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(allowed: AllowedDomainEntity): Long

    @Query("DELETE FROM allowed_domains WHERE domain = :domain")
    fun deleteByDomain(domain: String): Int

    @Query("SELECT * FROM allowed_domains ORDER BY addedTimestamp DESC")
    fun getAll(): Flow<List<AllowedDomainEntity>>

    @Query("SELECT domain FROM allowed_domains")
    fun getAllDomainsSync(): List<String>

    @Query("SELECT COUNT(*) FROM allowed_domains")
    fun getCount(): Flow<Int>

    @Query("DELETE FROM allowed_domains")
    fun clearAll(): Int
}
