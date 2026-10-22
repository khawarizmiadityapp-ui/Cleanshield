package com.rizmi.cleanshield

import com.rizmi.cleanshield.domain.matcher.DomainNormalizer
import org.junit.Assert.*
import org.junit.Test

class DomainNormalizerTest {

    @Test
    fun testValidDomains() {
        assertEquals("example.com", DomainNormalizer.normalize("example.com"))
        assertEquals("example.com", DomainNormalizer.normalize("EXAMPLE.COM"))
        assertEquals("example.com", DomainNormalizer.normalize("  example.com.  "))
        assertEquals("sub.ads.example.com", DomainNormalizer.normalize("sub.ads.example.com"))
        assertEquals("ad-server.net", DomainNormalizer.normalize("ad-server.net"))
        assertEquals("analytics_tracker.co.id", DomainNormalizer.normalize("analytics_tracker.co.id"))
    }

    @Test
    fun testUrlStripping() {
        assertEquals("example.com", DomainNormalizer.normalize("https://example.com/ads/script.js"))
        assertEquals("example.com", DomainNormalizer.normalize("http://example.com:8080/path?query=1"))
    }

    @Test
    fun testRejectIpAddresses() {
        assertNull(DomainNormalizer.normalize("0.0.0.0"))
        assertNull(DomainNormalizer.normalize("127.0.0.1"))
        assertNull(DomainNormalizer.normalize("192.168.1.1"))
        assertNull(DomainNormalizer.normalize("256.1.1.1")) // not a valid domain label
        assertNull(DomainNormalizer.normalize("::1"))
    }

    @Test
    fun testRejectMalformedInputs() {
        assertNull(DomainNormalizer.normalize(""))
        assertNull(DomainNormalizer.normalize("   "))
        assertNull(DomainNormalizer.normalize(null))
        assertNull(DomainNormalizer.normalize("invalid..domain"))
        assertNull(DomainNormalizer.normalize("-badstart.com"))
        assertNull(DomainNormalizer.normalize("badend-.com"))
        assertNull(DomainNormalizer.normalize("domain with spaces.com"))
        assertNull(DomainNormalizer.normalize("!special_chars!.com"))
    }

    @Test
    fun testLengthBounds() {
        val veryLongLabel = "a".repeat(64) + ".com"
        assertNull(DomainNormalizer.normalize(veryLongLabel))

        val maxValidLabel = "a".repeat(63) + ".com"
        assertNotNull(DomainNormalizer.normalize(maxValidLabel))

        val excessiveTotalLength = "a".repeat(50) + "." + "b".repeat(50) + "." + "c".repeat(50) + "." + "d".repeat(50) + "." + "e".repeat(60)
        assertNull(DomainNormalizer.normalize(excessiveTotalLength))
    }
}
