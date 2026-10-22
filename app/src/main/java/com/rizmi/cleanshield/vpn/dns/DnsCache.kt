package com.rizmi.cleanshield.vpn.dns

import java.util.concurrent.ConcurrentHashMap

/**
 * Thread-safe bounded in-memory DNS cache with TTL enforcement.
 */
class DnsCache(
    private val maxEntries: Int = 2000,
    private val defaultTtlSeconds: Long = 300L,
    private val minTtlSeconds: Long = 10L,
    private val maxTtlSeconds: Long = 3600L
) {
    data class CacheKey(val domain: String, val qType: Int)

    data class CacheEntry(
        val responseBytes: ByteArray,
        val expireAtMillis: Long
    ) {
        val isExpired: Boolean get() = System.currentTimeMillis() > expireAtMillis

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as CacheEntry
            return responseBytes.contentEquals(other.responseBytes) && expireAtMillis == other.expireAtMillis
        }

        override fun hashCode(): Int {
            var result = responseBytes.contentHashCode()
            result = 31 * result + expireAtMillis.hashCode()
            return result
        }
    }

    private val cache = ConcurrentHashMap<CacheKey, CacheEntry>()

    /**
     * Looks up a cached DNS response for domain and qType.
     * Rewrites the Transaction ID to match the requester's query.
     */
    fun get(domain: String, qType: Int, transactionId: Int): ByteArray? {
        val key = CacheKey(domain.lowercase(), qType)
        val entry = cache[key] ?: return null

        if (entry.isExpired) {
            cache.remove(key)
            return null
        }

        val cachedResponse = entry.responseBytes.clone()
        if (cachedResponse.size >= 2) {
            // Rewrite Transaction ID
            cachedResponse[0] = (transactionId shr 8).toByte()
            cachedResponse[1] = (transactionId and 0xFF).toByte()
        }
        return cachedResponse
    }

    /**
     * Caches a response. Extracts TTL from answers or uses default.
     */
    fun put(domain: String, qType: Int, responseBytes: ByteArray, ttlSeconds: Long = defaultTtlSeconds) {
        if (cache.size >= maxEntries) {
            pruneExpired()
            if (cache.size >= maxEntries) {
                // Remove first element if still full
                val iterator = cache.keys.iterator()
                if (iterator.hasNext()) {
                    cache.remove(iterator.next())
                }
            }
        }

        val boundedTtl = ttlSeconds.coerceIn(minTtlSeconds, maxTtlSeconds)
        val expireAt = System.currentTimeMillis() + (boundedTtl * 1000L)
        cache[CacheKey(domain.lowercase(), qType)] = CacheEntry(responseBytes.clone(), expireAt)
    }

    /**
     * Clears all cached entries.
     */
    fun clear() {
        cache.clear()
    }

    fun size(): Int = cache.size

    private fun pruneExpired() {
        val now = System.currentTimeMillis()
        val it = cache.entries.iterator()
        while (it.hasNext()) {
            if (it.next().value.expireAtMillis <= now) {
                it.remove()
            }
        }
    }
}
