package com.rizmi.cleanshield.vpn.packet

/**
 * Computes RFC 1071 Internet Checksum for IPv4 headers and UDP/TCP pseudo-headers.
 */
object PacketChecksum {

    /**
     * Calculates 16-bit one's complement sum over byte array.
     */
    fun calculateChecksum(data: ByteArray, offset: Int, length: Int): Int {
        var sum = 0L
        var i = offset
        val end = offset + length

        while (i < end - 1) {
            val byte1 = data[i].toInt() and 0xFF
            val byte2 = data[i + 1].toInt() and 0xFF
            val word = (byte1 shl 8) or byte2
            sum += word
            i += 2
        }

        // Odd byte padding
        if (i < end) {
            val byte1 = data[i].toInt() and 0xFF
            val word = byte1 shl 8
            sum += word
        }

        while (sum shr 16 > 0) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }

        return (sum.inv() and 0xFFFF).toInt()
    }

    /**
     * Calculates UDP checksum including IPv4 pseudo-header.
     */
    fun calculateUdpChecksum(
        srcIp: ByteArray,
        dstIp: ByteArray,
        udpPacket: ByteArray,
        udpOffset: Int,
        udpLength: Int
    ): Int {
        var sum = 0L

        // Pseudo-header:
        // Src IP (4 bytes)
        for (i in 0 until 4 step 2) {
            val word = ((srcIp[i].toInt() and 0xFF) shl 8) or (srcIp[i + 1].toInt() and 0xFF)
            sum += word
        }
        // Dst IP (4 bytes)
        for (i in 0 until 4 step 2) {
            val word = ((dstIp[i].toInt() and 0xFF) shl 8) or (dstIp[i + 1].toInt() and 0xFF)
            sum += word
        }
        // Zero (1 byte) + Protocol UDP=17 (1 byte)
        sum += 17
        // UDP length (2 bytes)
        sum += udpLength

        // UDP Header & Data (checksum field temporarily set to 0)
        var i = udpOffset
        val end = udpOffset + udpLength
        while (i < end - 1) {
            // Skip existing checksum at offset + 6
            val word = if (i == udpOffset + 6) {
                0
            } else {
                val b1 = udpPacket[i].toInt() and 0xFF
                val b2 = udpPacket[i + 1].toInt() and 0xFF
                (b1 shl 8) or b2
            }
            sum += word
            i += 2
        }

        if (i < end) {
            val b1 = udpPacket[i].toInt() and 0xFF
            sum += (b1 shl 8)
        }

        while (sum shr 16 > 0) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }

        val result = (sum.inv() and 0xFFFF).toInt()
        return if (result == 0) 0xFFFF else result
    }
}
