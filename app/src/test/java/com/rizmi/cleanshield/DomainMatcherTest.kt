package com.rizmi.cleanshield

import com.rizmi.cleanshield.domain.matcher.DomainMatcher
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class DomainMatcherTest {

    private lateinit var matcher: DomainMatcher

    @Before
    fun setup() {
        matcher = DomainMatcher()
        matcher.setBlockedDomains(
            listOf(
                "doubleclick.net",
                "google-analytics.com",
                "adservice.google.com",
                "tracking.example.org"
            )
        )
    }

    @Test
    fun testExactMatchBlocked() {
        val res = matcher.evaluate("doubleclick.net")
        assertTrue(res is DomainMatcher.Result.Blocked)
        assertEquals("doubleclick.net", (res as DomainMatcher.Result.Blocked).matchedRule)
    }

    @Test
    fun testSubdomainInheritanceBlocked() {
        val res1 = matcher.evaluate("ad.doubleclick.net")
        assertTrue(res1 is DomainMatcher.Result.Blocked)
        assertEquals("doubleclick.net", (res1 as DomainMatcher.Result.Blocked).matchedRule)

        val res2 = matcher.evaluate("deep.nested.sub.tracking.example.org")
        assertTrue(res2 is DomainMatcher.Result.Blocked)
        assertEquals("tracking.example.org", (res2 as DomainMatcher.Result.Blocked).matchedRule)
    }

    @Test
    fun testSimilarDomainsNotBlocked() {
        // "notdoubleclick.net" should NOT match "doubleclick.net"
        val res1 = matcher.evaluate("notdoubleclick.net")
        assertTrue(res1 is DomainMatcher.Result.Allowed)

        // "doubleclick.net.evil.com" should NOT match "doubleclick.net"
        val res2 = matcher.evaluate("doubleclick.net.evil.com")
        assertTrue(res2 is DomainMatcher.Result.Allowed)

        // "mydoubleclick.net" should NOT match
        val res3 = matcher.evaluate("mydoubleclick.net")
        assertTrue(res3 is DomainMatcher.Result.Allowed)
    }

    @Test
    fun testAllowlistPrecedenceOverBlocklist() {
        // Before allowlisting, adservice.google.com is blocked
        val initialRes = matcher.evaluate("adservice.google.com")
        assertTrue(initialRes is DomainMatcher.Result.Blocked)

        // Add to allowlist
        matcher.addAllowedDomain("adservice.google.com")

        val allowRes = matcher.evaluate("adservice.google.com")
        assertTrue(allowRes is DomainMatcher.Result.Allowed)

        // Subdomain of allowlisted domain should also be allowed
        matcher.addAllowedDomain("example.org")
        val allowSubRes = matcher.evaluate("tracking.example.org")
        assertTrue(allowSubRes is DomainMatcher.Result.Allowed)
    }

    @Test
    fun testAddAndRemoveAllowedDomain() {
        matcher.addAllowedDomain("temp-allowed.com")
        assertTrue(matcher.getAllowedCount() > 0)
        matcher.removeAllowedDomain("temp-allowed.com")
        assertEquals(0, matcher.getAllowedCount())
    }

    @Test
    fun testInvalidDomainReturnsInvalid() {
        val res = matcher.evaluate("...malformed...")
        assertTrue(res is DomainMatcher.Result.Invalid)
    }
}
