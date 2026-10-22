package com.rizmi.cleanshield

import com.rizmi.cleanshield.vpn.dns.DnsCache
import org.junit.Assert.*
import org.junit.Test

class DnsCacheTest {

    @Test
    fun testCachePutAndGetWithIdRewrite() {
        val cache = DnsCache()
        val originalResponse = byteArrayOf(0x12, 0x34, 0x81.toByte(), 0x80.toByte(), 0x00, 0x01)

        cache.put("example.com", 1, originalResponse, ttlSeconds = 60)

        // Request with new transaction ID 0xBEEF
        val cached = cache.get("example.com", 1, transactionId = 0xBEEF)
        assertNotNull(cached)
        assertEquals(0xBE.toByte(), cached!![0])
        assertEquals(0xEF.toByte(), cached[1])

        // Verify original data in response preserved
        assertEquals(0x81.toByte(), cached[2])
    }

    @Test
    fun testCacheCaseInsensitive() {
        val cache = DnsCache()
        val data = byteArrayOf(0x01, 0x02)
        cache.put("AdServer.Net", 1, data, ttlSeconds = 60)

        val cached = cache.get("adserver.net", 1, transactionId = 0x0102)
        assertNotNull(cached)
    }

    @Test
    fun testCacheExpiry() {
        // Minimal TTL test with 0 second TTL
        val cache = DnsCache(minTtlSeconds = 0)
        val data = byteArrayOf(0x01, 0x02)
        cache.put("expiring.com", 1, data, ttlSeconds = 0)

        // Wait small delay
        Thread.sleep(50)
        val cached = cache.get("expiring.com", 1, transactionId = 0x0102)
        assertNull(cached)
    }
}
