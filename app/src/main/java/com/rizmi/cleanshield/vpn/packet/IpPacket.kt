package com.rizmi.cleanshield.vpn.packet

import java.net.InetAddress

/**
 * Handles parsing and serialization of IPv4 packets.
 */
class IpPacket private constructor(
    val version: Int,
    val headerLength: Int,
    val totalLength: Int,
    val protocol: Int,
    val identification: Int,
    val ttl: Int,
    val sourceIpBytes: ByteArray,
    val destIpBytes: ByteArray,
    val payloadOffset: Int,
    val payloadLength: Int,
    val rawBytes: ByteArray
) {
    val sourceIp: InetAddress by lazy { InetAddress.getByAddress(sourceIpBytes) }
    val destIp: InetAddress by lazy { InetAddress.getByAddress(destIpBytes) }

    val isUdp: Boolean get() = protocol == PROTOCOL_UDP
    val isTcp: Boolean get() = protocol == PROTOCOL_TCP

    companion object {
        const val PROTOCOL_TCP = 6
        const val PROTOCOL_UDP = 17

        /**
         * Parses an IPv4 packet from a byte array.
         * Returns null if the packet is malformed, truncated, or not IPv4.
         */
        fun parse(packet: ByteArray, length: Int): IpPacket? {
            if (length < 20) return null

            val version = (packet[0].toInt() shr 4) and 0x0F
            if (version != 4) {
                // For MVP we focus on IPv4 DNS interception
                return null
            }

            val ihl = packet[0].toInt() and 0x0F
            val headerLength = ihl * 4
            if (headerLength < 20 || headerLength > length) return null

            val totalLength = ((packet[2].toInt() and 0xFF) shl 8) or (packet[3].toInt() and 0xFF)
            if (totalLength < headerLength || totalLength > length) return null

            val identification = ((packet[4].toInt() and 0xFF) shl 8) or (packet[5].toInt() and 0xFF)
            val ttl = packet[8].toInt() and 0xFF
            val protocol = packet[9].toInt() and 0xFF

            val srcIp = ByteArray(4)
            System.arraycopy(packet, 12, srcIp, 0, 4)

            val dstIp = ByteArray(4)
            System.arraycopy(packet, 16, dstIp, 0, 4)

            val payloadLength = totalLength - headerLength

            return IpPacket(
                version = version,
                headerLength = headerLength,
                totalLength = totalLength,
                protocol = protocol,
                identification = identification,
                ttl = ttl,
                sourceIpBytes = srcIp,
                destIpBytes = dstIp,
                payloadOffset = headerLength,
                payloadLength = payloadLength,
                rawBytes = packet
            )
        }

        /**
         * Builds an IPv4 packet encapsulating the given payload, setting the given
         * source and destination IPs and protocol.
         */
        fun buildIpv4Packet(
            srcIp: ByteArray,
            dstIp: ByteArray,
            protocol: Int,
            payload: ByteArray,
            identification: Int = 0x1234,
            ttl: Int = 64
        ): ByteArray {
            val headerLength = 20
            val totalLength = headerLength + payload.size
            val packet = ByteArray(totalLength)

            // Version (4) + IHL (5 words = 20 bytes)
            packet[0] = 0x45.toByte()
            // DSCP / ECN
            packet[1] = 0x00.toByte()
            // Total length (16 bits)
            packet[2] = (totalLength shr 8).toByte()
            packet[3] = (totalLength and 0xFF).toByte()
            // Identification
            packet[4] = (identification shr 8).toByte()
            packet[5] = (identification and 0xFF).toByte()
            // Flags (Don't Fragment: 0x4000)
            packet[6] = 0x40.toByte()
            packet[7] = 0x00.toByte()
            // TTL
            packet[8] = ttl.toByte()
            // Protocol (UDP = 17)
            packet[9] = protocol.toByte()
            // Header checksum initialized to 0
            packet[10] = 0x00.toByte()
            packet[11] = 0x00.toByte()

            // Source IP
            System.arraycopy(srcIp, 0, packet, 12, 4)
            // Destination IP
            System.arraycopy(dstIp, 0, packet, 16, 4)

            // Calculate IPv4 Header Checksum
            val checksum = PacketChecksum.calculateChecksum(packet, 0, headerLength)
            packet[10] = (checksum shr 8).toByte()
            packet[11] = (checksum and 0xFF).toByte()

            // Copy Payload
            System.arraycopy(payload, 0, packet, headerLength, payload.size)

            return packet
        }
    }
}
