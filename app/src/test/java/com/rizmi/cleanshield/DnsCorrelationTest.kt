package com.rizmi.cleanshield

import com.rizmi.cleanshield.domain.matcher.DomainMatcher
import com.rizmi.cleanshield.vpn.dns.DnsPacket
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayOutputStream

class DnsCorrelationTest {

    private lateinit var matcher: DomainMatcher

    @Before
    fun setup() {
        matcher = DomainMatcher()
        matcher.setBlockedDomains(listOf("ads.tracker.com", "telemetry.bad.org"))
        matcher.addAllowedDomain("sub.ads.tracker.com") // allowlist override
    }

    private fun buildQuery(id: Int, domain: String): ByteArray {
        val out = ByteArrayOutputStream()
        out.write((id shr 8) and 0xFF)
        out.write(id and 0xFF)
        out.write(0x01) // Flags: RD = 1
        out.write(0x00)
        out.write(0x00) // QDCOUNT = 1
        out.write(0x01)
        out.write(0x00) // ANCOUNT = 0
        out.write(0x00)
        out.write(0x00) // NSCOUNT = 0
        out.write(0x00)
        out.write(0x00) // ARCOUNT = 0
        out.write(0x00)

        for (label in domain.split('.')) {
            val bytes = label.toByteArray(Charsets.US_ASCII)
            out.write(bytes.size)
            out.write(bytes)
        }
        out.write(0)

        out.write(0x00) // QTYPE = A (1)
        out.write(0x01)
        out.write(0x00) // QCLASS = IN (1)
        out.write(0x01)

        return out.toByteArray()
    }

    @Test
    fun testBlockedDomainGeneratesCorrelatedNxDomain() {
        val queryId = 0x7788
        val raw = buildQuery(queryId, "ads.tracker.com")
        val parsed = DnsPacket.parse(raw, 0, raw.size)!!

        val evalResult = matcher.evaluate(parsed.firstDomain)
        assertTrue(evalResult is DomainMatcher.Result.Blocked)

        val response = DnsPacket.buildNxDomainResponse(parsed)
        val respId = ((response[0].toInt() and 0xFF) shl 8) or (response[1].toInt() and 0xFF)

        // Verifies exact transaction ID correlation
        assertEquals(queryId, respId)

        // Verifies NXDOMAIN response code (RCODE=3)
        val flags = ((response[2].toInt() and 0xFF) shl 8) or (response[3].toInt() and 0xFF)
        assertEquals(3, flags and 0x0F)
    }

    @Test
    fun testAllowedDomainPrecedenceAndServFailCorrelation() {
        val queryId = 0x99AA
        val raw = buildQuery(queryId, "sub.ads.tracker.com")
        val parsed = DnsPacket.parse(raw, 0, raw.size)!!

        val evalResult = matcher.evaluate(parsed.firstDomain)
        assertTrue(evalResult is DomainMatcher.Result.Allowed)

        // If upstream fails, SERVFAIL response must maintain correlation
        val servFail = DnsPacket.buildServFailResponse(parsed)
        val respId = ((servFail[0].toInt() and 0xFF) shl 8) or (servFail[1].toInt() and 0xFF)
        assertEquals(queryId, respId)

        val flags = ((servFail[2].toInt() and 0xFF) shl 8) or (servFail[3].toInt() and 0xFF)
        assertEquals(2, flags and 0x0F) // SERVFAIL = 2
    }
}
