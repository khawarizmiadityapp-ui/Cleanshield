package com.rizmi.cleanshield.vpn.packet

/**
 * Handles parsing and serialization of UDP packets (RFC 768).
 */
class UdpPacket private constructor(
    val sourcePort: Int,
    val destPort: Int,
    val length: Int,
    val checksum: Int,
    val payloadOffset: Int,
    val payloadLength: Int,
    val rawBytes: ByteArray
) {
    fun getPayload(): ByteArray {
        val payload = ByteArray(payloadLength)
        System.arraycopy(rawBytes, payloadOffset, payload, 0, payloadLength)
        return payload
    }

    companion object {
        const val HEADER_LENGTH = 8

        /**
         * Parses a UDP packet from the payload of an IP packet.
         */
        fun parse(data: ByteArray, offset: Int, length: Int): UdpPacket? {
            if (length < HEADER_LENGTH) return null

            val srcPort = ((data[offset].toInt() and 0xFF) shl 8) or (data[offset + 1].toInt() and 0xFF)
            val dstPort = ((data[offset + 2].toInt() and 0xFF) shl 8) or (data[offset + 3].toInt() and 0xFF)
            val udpLength = ((data[offset + 4].toInt() and 0xFF) shl 8) or (data[offset + 5].toInt() and 0xFF)
            val checksum = ((data[offset + 6].toInt() and 0xFF) shl 8) or (data[offset + 7].toInt() and 0xFF)

            if (udpLength < HEADER_LENGTH || udpLength > length) return null

            val payloadLength = udpLength - HEADER_LENGTH

            return UdpPacket(
                sourcePort = srcPort,
                destPort = dstPort,
                length = udpLength,
                checksum = checksum,
                payloadOffset = offset + HEADER_LENGTH,
                payloadLength = payloadLength,
                rawBytes = data
            )
        }

        /**
         * Builds a UDP packet byte array including header and checksum.
         */
        fun buildUdpPacket(
            srcIp: ByteArray,
            dstIp: ByteArray,
            srcPort: Int,
            dstPort: Int,
            payload: ByteArray
        ): ByteArray {
            val totalLength = HEADER_LENGTH + payload.size
            val packet = ByteArray(totalLength)

            // Source Port
            packet[0] = (srcPort shr 8).toByte()
            packet[1] = (srcPort and 0xFF).toByte()
            // Destination Port
            packet[2] = (dstPort shr 8).toByte()
            packet[3] = (dstPort and 0xFF).toByte()
            // Length
            packet[4] = (totalLength shr 8).toByte()
            packet[5] = (totalLength and 0xFF).toByte()
            // Checksum (initially 0)
            packet[6] = 0x00.toByte()
            packet[7] = 0x00.toByte()

            // Payload
            System.arraycopy(payload, 0, packet, HEADER_LENGTH, payload.size)

            // Compute Checksum
            val checksum = PacketChecksum.calculateUdpChecksum(srcIp, dstIp, packet, 0, totalLength)
            packet[6] = (checksum shr 8).toByte()
            packet[7] = (checksum and 0xFF).toByte()

            return packet
        }
    }
}
