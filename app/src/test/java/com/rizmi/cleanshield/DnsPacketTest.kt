package com.rizmi.cleanshield

import com.rizmi.cleanshield.vpn.dns.DnsPacket
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream

class DnsPacketTest {

    private fun createSampleDnsQuery(
        id: Int = 0xABCD,
        domain: String = "ads.example.com",
        qType: Int = DnsPacket.TYPE_A,
        recursionDesired: Boolean = true
    ): ByteArray {
        val out = ByteArrayOutputStream()
        // ID
        out.write((id shr 8) and 0xFF)
        out.write(id and 0xFF)
        // Flags (Query, Opcode=0, RD)
        val flags = if (recursionDesired) 0x0100 else 0x0000
        out.write((flags shr 8) and 0xFF)
        out.write(flags and 0xFF)
        // QDCOUNT = 1
        out.write(0)
        out.write(1)
        // ANCOUNT = 0
        out.write(0)
        out.write(0)
        // NSCOUNT = 0
        out.write(0)
        out.write(0)
        // ARCOUNT = 0
        out.write(0)
        out.write(0)

        // Labels
        for (label in domain.split('.')) {
            val bytes = label.toByteArray(Charsets.US_ASCII)
            out.write(bytes.size)
            out.write(bytes)
        }
        out.write(0) // Null terminator

        // QTYPE
        out.write((qType shr 8) and 0xFF)
        out.write(qType and 0xFF)
        // QCLASS = IN (1)
        out.write(0)
        out.write(1)

        return out.toByteArray()
    }

    @Test
    fun testParseValidDnsQuery() {
        val raw = createSampleDnsQuery(id = 0x1234, domain = "tracker.doubleclick.net", qType = DnsPacket.TYPE_A)
        val dns = DnsPacket.parse(raw, 0, raw.size)

        assertNotNull(dns)
        assertEquals(0x1234, dns!!.transactionId)
        assertTrue(dns.isQuery)
        assertTrue(dns.isRecursionDesired)
        assertEquals(1, dns.questionCount)
        assertEquals("tracker.doubleclick.net", dns.firstDomain)
        assertEquals("A", dns.firstType)
    }

    @Test
    fun testParseAaaaQuery() {
        val raw = createSampleDnsQuery(id = 0x5678, domain = "google.com", qType = DnsPacket.TYPE_AAAA)
        val dns = DnsPacket.parse(raw, 0, raw.size)

        assertNotNull(dns)
        assertEquals("AAAA", dns!!.firstType)
        assertEquals("google.com", dns.firstDomain)
    }

    @Test
    fun testBuildNxDomainResponse() {
        val queryBytes = createSampleDnsQuery(id = 0xBEEF, domain = "malicious-ads.org", qType = DnsPacket.TYPE_A, recursionDesired = true)
        val query = DnsPacket.parse(queryBytes, 0, queryBytes.size)!!

        val responseBytes = DnsPacket.buildNxDomainResponse(query)

        // Response should be parsable and valid
        assertTrue(responseBytes.size >= DnsPacket.HEADER_SIZE)

        // Check Transaction ID matches
        val respId = ((responseBytes[0].toInt() and 0xFF) shl 8) or (responseBytes[1].toInt() and 0xFF)
        assertEquals(0xBEEF, respId)

        // Check Flags
        val respFlags = ((responseBytes[2].toInt() and 0xFF) shl 8) or (responseBytes[3].toInt() and 0xFF)
        // QR=1 (bit 15)
        assertTrue((respFlags and 0x8000) != 0)
        // RA=1 (bit 7)
        assertTrue((respFlags and 0x0080) != 0)
        // RD=1 (bit 8)
        assertTrue((respFlags and 0x0100) != 0)
        // RCODE=3 (NXDOMAIN)
        assertEquals(DnsPacket.RCODE_NXDOMAIN, respFlags and 0x000F)

        // QDCOUNT=1
        val qdCount = ((responseBytes[4].toInt() and 0xFF) shl 8) or (responseBytes[5].toInt() and 0xFF)
        assertEquals(1, qdCount)

        // ANCOUNT=0
        val anCount = ((responseBytes[6].toInt() and 0xFF) shl 8) or (responseBytes[7].toInt() and 0xFF)
        assertEquals(0, anCount)
    }

    @Test
    fun testBuildServFailResponse() {
        val queryBytes = createSampleDnsQuery(id = 0x4321, domain = "timeout.com")
        val query = DnsPacket.parse(queryBytes, 0, queryBytes.size)!!

        val responseBytes = DnsPacket.buildServFailResponse(query)
        val respFlags = ((responseBytes[2].toInt() and 0xFF) shl 8) or (responseBytes[3].toInt() and 0xFF)
        assertEquals(DnsPacket.RCODE_SERVFAIL, respFlags and 0x000F)
    }

    @Test
    fun testMalformedAndTruncatedPackets() {
        // Less than header size
        assertNull(DnsPacket.parse(ByteArray(5), 0, 5))

        // Truncated question section
        val incomplete = ByteArray(14)
        incomplete[4] = 0 // QDCOUNT = 1
        incomplete[5] = 1
        assertNull(DnsPacket.parse(incomplete, 0, incomplete.size))
    }
}
