package com.rizmi.cleanshield

import com.rizmi.cleanshield.data.parser.BlocklistParser
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream

class BlocklistParserTest {

    @Test
    fun testParseHostsAndPlainDomains() {
        val sampleContent = """
            # This is a comment header
            # Another comment
            
            0.0.0.0 ads.doubleclick.net # inline comment
            127.0.0.1 tracker.telemetry.io
            plain-ad-domain.com
            
            # Localhost entries should be ignored
            127.0.0.1 localhost
            0.0.0.0 localhost.localdomain
            
            # Duplicates
            0.0.0.0 ads.doubleclick.net
            plain-ad-domain.com
            
            # Invalid lines
            invalid..domain
            0.0.0.0 -badstart.com
        """.trimIndent()

        val inputStream = ByteArrayInputStream(sampleContent.toByteArray(Charsets.UTF_8))
        val preview = BlocklistParser.parse(inputStream, "test_hosts.txt")

        assertEquals("test_hosts.txt", preview.fileName)
        assertEquals(3, preview.validCount) // ads.doubleclick.net, tracker.telemetry.io, plain-ad-domain.com
        assertEquals(2, preview.duplicateCount) // ads.doubleclick.net, plain-ad-domain.com
        assertTrue(preview.invalidCount >= 4) // localhost, localhost.localdomain, invalid..domain, -badstart.com

        assertTrue(preview.parsedDomains.contains("ads.doubleclick.net"))
        assertTrue(preview.parsedDomains.contains("tracker.telemetry.io"))
        assertTrue(preview.parsedDomains.contains("plain-ad-domain.com"))
        assertFalse(preview.parsedDomains.contains("localhost"))
    }

    @Test
    fun testEmptyFile() {
        val inputStream = ByteArrayInputStream("".toByteArray(Charsets.UTF_8))
        val preview = BlocklistParser.parse(inputStream, "empty.txt")

        assertEquals(0, preview.totalLines)
        assertEquals(0, preview.validCount)
        assertEquals(0, preview.duplicateCount)
        assertEquals(0, preview.invalidCount)
        assertTrue(preview.parsedDomains.isEmpty())
    }

    @Test
    fun testParseAdblockPlusSyntax() {
        val sampleContent = """
            [Adblock Plus]
            ! Version: 202610220508
            ! Title: OISD / AdGuard Sample
            ! Description: Sample filter
            
            ||doubleclick.net^
            ||pagead2.googlesyndication.com^${'$'}third-party
            ||sub.tracker.io^
            
            ! Cosmetic filter to ignore
            example.com##.ad-banner
            example.com#?#div[class="ad"]
            
            ! Comments
            ! Another comment
        """.trimIndent()

        val inputStream = ByteArrayInputStream(sampleContent.toByteArray(Charsets.UTF_8))
        val preview = BlocklistParser.parse(inputStream, "oisd_test.txt")

        assertEquals(3, preview.validCount)
        assertTrue(preview.parsedDomains.contains("doubleclick.net"))
        assertTrue(preview.parsedDomains.contains("pagead2.googlesyndication.com"))
        assertTrue(preview.parsedDomains.contains("sub.tracker.io"))
    }
}
