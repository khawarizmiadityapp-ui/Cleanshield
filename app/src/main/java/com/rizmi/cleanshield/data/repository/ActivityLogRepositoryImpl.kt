package com.rizmi.cleanshield.data.repository

import com.rizmi.cleanshield.data.local.dao.DnsEventDao
import com.rizmi.cleanshield.data.local.entity.DnsEventEntity
import com.rizmi.cleanshield.domain.model.DnsQueryEvent
import com.rizmi.cleanshield.domain.model.ProtectionStats
import com.rizmi.cleanshield.domain.model.QueryStatus
import com.rizmi.cleanshield.domain.repository.ActivityLogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ActivityLogRepositoryImpl(
    private val dnsEventDao: DnsEventDao
) : ActivityLogRepository {

    override fun getRecentEvents(limit: Int): Flow<List<DnsQueryEvent>> {
        return dnsEventDao.getRecentEvents(limit).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getEventsByStatus(status: String, limit: Int): Flow<List<DnsQueryEvent>> {
        return dnsEventDao.getEventsByStatus(status, limit).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getProtectionStats(): Flow<ProtectionStats> {
        return combine(
            dnsEventDao.getTotalCount(),
            dnsEventDao.getBlockedCount(),
            dnsEventDao.getForwardedCount(),
            dnsEventDao.getFailedCount(),
            dnsEventDao.getUniqueBlockedDomainsCount()
        ) { total, blocked, forwarded, failed, uniqueBlocked ->
            ProtectionStats(
                totalQueries = total,
                blockedQueries = blocked,
                forwardedQueries = forwarded,
                failedQueries = failed,
                uniqueBlockedDomains = uniqueBlocked
            )
        }
    }

    override suspend fun logEvent(event: DnsQueryEvent): Unit = withContext(Dispatchers.IO) {
        dnsEventDao.insert(
            DnsEventEntity(
                timestamp = event.timestamp,
                domain = event.domain,
                queryType = event.queryType,
                clientIp = event.clientIp,
                status = event.status.name,
                blockReason = event.blockReason,
                responseTimeMs = event.responseTimeMs,
                upstreamResolver = event.upstreamResolver
            )
        )
        Unit
    }

    override suspend fun clearHistory(): Unit = withContext(Dispatchers.IO) {
        dnsEventDao.clearAll()
        Unit
    }

    private fun DnsEventEntity.toDomain() = DnsQueryEvent(
        id = id,
        timestamp = timestamp,
        domain = domain,
        queryType = queryType,
        clientIp = clientIp,
        status = try { QueryStatus.valueOf(status) } catch (e: Exception) { QueryStatus.ALLOWED },
        blockReason = blockReason,
        responseTimeMs = responseTimeMs,
        upstreamResolver = upstreamResolver
    )
}
