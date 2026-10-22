package com.rizmi.cleanshield

import com.rizmi.cleanshield.vpn.packet.IpPacket
import com.rizmi.cleanshield.vpn.packet.PacketChecksum
import com.rizmi.cleanshield.vpn.packet.UdpPacket
import org.junit.Assert.*
import org.junit.Test
import java.net.InetAddress

class IpPacketTest {

    @Test
    fun testIpAndUdpPacketBuildAndParse() {
        val srcIp = byteArrayOf(10, 255.toByte(), 0, 1)
        val dstIp = byteArrayOf(10, 255.toByte(), 0, 2)
        val payload = "DNS_PAYLOAD_TEST".toByteArray(Charsets.UTF_8)

        val udpBytes = UdpPacket.buildUdpPacket(
            srcIp = srcIp,
            dstIp = dstIp,
            srcPort = 53,
            dstPort = 54321,
            payload = payload
        )

        val ipBytes = IpPacket.buildIpv4Packet(
            srcIp = srcIp,
            dstIp = dstIp,
            protocol = IpPacket.PROTOCOL_UDP,
            payload = udpBytes
        )

        // Parse IP packet
        val parsedIp = IpPacket.parse(ipBytes, ipBytes.size)
        assertNotNull(parsedIp)
        assertEquals(4, parsedIp!!.version)
        assertEquals(20, parsedIp.headerLength)
        assertEquals(ipBytes.size, parsedIp.totalLength)
        assertEquals(IpPacket.PROTOCOL_UDP, parsedIp.protocol)
        assertTrue(parsedIp.isUdp)
        assertFalse(parsedIp.isTcp)
        assertEquals(InetAddress.getByAddress(srcIp), parsedIp.sourceIp)
        assertEquals(InetAddress.getByAddress(dstIp), parsedIp.destIp)

        // Validate IP checksum (should verify to 0)
        val checksumVerify = PacketChecksum.calculateChecksum(ipBytes, 0, 20)
        assertEquals(0, checksumVerify)

        // Parse UDP packet
        val parsedUdp = UdpPacket.parse(ipBytes, parsedIp.payloadOffset, parsedIp.payloadLength)
        assertNotNull(parsedUdp)
        assertEquals(53, parsedUdp!!.sourcePort)
        assertEquals(54321, parsedUdp.destPort)
        assertEquals(udpBytes.size, parsedUdp.length)

        val extractedPayload = parsedUdp.getPayload()
        assertArrayEquals(payload, extractedPayload)
    }

    @Test
    fun testMalformedIpPacket() {
        // Less than 20 bytes
        assertNull(IpPacket.parse(ByteArray(10), 10))

        // Invalid version (IPv6 byte = 0x60)
        val ipv6Header = ByteArray(40)
        ipv6Header[0] = 0x60.toByte()
        assertNull(IpPacket.parse(ipv6Header, ipv6Header.size))
    }
}
