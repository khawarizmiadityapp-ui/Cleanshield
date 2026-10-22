package com.rizmi.cleanshield.vpn.dns

import java.io.ByteArrayOutputStream
import java.util.Locale

/**
 * Parses DNS queries and builds RFC 1035 compliant DNS response packets.
 */
class DnsPacket private constructor(
    val transactionId: Int,
    val flags: Int,
    val isQuery: Boolean,
    val isRecursionDesired: Boolean,
    val questionCount: Int,
    val questions: List<Question>,
    val rawBytes: ByteArray,
    val questionSectionBytes: ByteArray
) {
    data class Question(
        val name: String,
        val type: Int,
        val qClass: Int
    ) {
        val typeName: String
            get() = when (type) {
                1 -> "A"
                28 -> "AAAA"
                5 -> "CNAME"
                15 -> "MX"
                16 -> "TXT"
                65 -> "HTTPS"
                255 -> "ANY"
                else -> "TYPE_$type"
            }
    }

    val firstDomain: String?
        get() = questions.firstOrNull()?.name

    val firstType: String
        get() = questions.firstOrNull()?.typeName ?: "A"

    companion object {
        const val HEADER_SIZE = 12

        const val RCODE_NOERROR = 0
        const val RCODE_SERVFAIL = 2
        const val RCODE_NXDOMAIN = 3

        const val TYPE_A = 1
        const val TYPE_AAAA = 28
        const val TYPE_CNAME = 5
        const val TYPE_HTTPS = 65

        /**
         * Parses a raw DNS payload. Returns null if invalid or malformed.
         */
        fun parse(data: ByteArray, offset: Int, length: Int): DnsPacket? {
            if (length < HEADER_SIZE) return null

            val id = ((data[offset].toInt() and 0xFF) shl 8) or (data[offset + 1].toInt() and 0xFF)
            val flags = ((data[offset + 2].toInt() and 0xFF) shl 8) or (data[offset + 3].toInt() and 0xFF)
            val qdCount = ((data[offset + 4].toInt() and 0xFF) shl 8) or (data[offset + 5].toInt() and 0xFF)

            val isQuery = (flags and 0x8000) == 0
            val isRd = (flags and 0x0100) != 0

            var currentOffset = offset + HEADER_SIZE
            val endOffset = offset + length
            val questions = mutableListOf<Question>()

            val questionStart = currentOffset

            // Parse Question section
            for (q in 0 until qdCount) {
                val nameBuilder = StringBuilder()
                val parsedNameOffset = parseDomainName(data, offset, currentOffset, endOffset, nameBuilder)
                if (parsedNameOffset == -1) return null

                currentOffset = parsedNameOffset

                // Check remaining bytes for QTYPE (2) + QCLASS (2)
                if (currentOffset + 4 > endOffset) return null

                val qType = ((data[currentOffset].toInt() and 0xFF) shl 8) or (data[currentOffset + 1].toInt() and 0xFF)
                val qClass = ((data[currentOffset + 2].toInt() and 0xFF) shl 8) or (data[currentOffset + 3].toInt() and 0xFF)
                currentOffset += 4

                questions.add(Question(nameBuilder.toString().lowercase(Locale.ROOT), qType, qClass))
            }

            val questionSectionLength = currentOffset - questionStart
            val questionBytes = ByteArray(questionSectionLength)
            System.arraycopy(data, questionStart, questionBytes, 0, questionSectionLength)

            return DnsPacket(
                transactionId = id,
                flags = flags,
                isQuery = isQuery,
                isRecursionDesired = isRd,
                questionCount = qdCount,
                questions = questions,
                rawBytes = data.copyOfRange(offset, offset + length),
                questionSectionBytes = questionBytes
            )
        }

        /**
         * Parses a domain name from DNS packet (handles labels and compression pointers).
         * Returns the offset immediately after the domain name in the stream, or -1 on error.
         */
        private fun parseDomainName(
            data: ByteArray,
            packetStart: Int,
            startOffset: Int,
            endOffset: Int,
            nameBuilder: StringBuilder
        ): Int {
            var curr = startOffset
            var jumped = false
            var returnOffset = -1
            var jumpsCount = 0

            while (curr < endOffset) {
                val len = data[curr].toInt() and 0xFF

                if (len == 0) {
                    // Null terminator
                    curr++
                    if (!jumped) {
                        returnOffset = curr
                    }
                    break
                }

                if ((len and 0xC0) == 0xC0) {
                    // Compression pointer
                    if (curr + 1 >= endOffset) return -1
                    if (!jumped) {
                        returnOffset = curr + 2
                    }
                    jumped = true
                    jumpsCount++
                    if (jumpsCount > 10) return -1 // Prevent pointer loop cycles

                    val pointerOffset = packetStart + (((len and 0x3F) shl 8) or (data[curr + 1].toInt() and 0xFF))
                    if (pointerOffset < packetStart || pointerOffset >= endOffset) return -1
                    curr = pointerOffset
                    continue
                }

                // Normal label
                curr++
                if (curr + len > endOffset) return -1

                if (nameBuilder.isNotEmpty()) {
                    nameBuilder.append('.')
                }
                nameBuilder.append(String(data, curr, len, Charsets.US_ASCII))
                curr += len
            }

            return if (returnOffset != -1) returnOffset else if (curr <= endOffset) curr else -1
        }

        /**
         * Builds an RFC 1035 NXDOMAIN response packet matching the query.
         */
        fun buildNxDomainResponse(query: DnsPacket): ByteArray {
            val response = ByteArrayOutputStream(HEADER_SIZE + query.questionSectionBytes.size)

            // 1. Transaction ID (2 bytes)
            response.write((query.transactionId shr 8) and 0xFF)
            response.write(query.transactionId and 0xFF)

            // 2. Flags: QR=1 (Response), AA=0, TC=0, RD=preserve, RA=1, RCODE=3 (NXDOMAIN)
            val rdBit = if (query.isRecursionDesired) 0x0100 else 0x0000
            val flags = 0x8180 or rdBit or RCODE_NXDOMAIN // 0x8183 with RD=1
            response.write((flags shr 8) and 0xFF)
            response.write(flags and 0xFF)

            // 3. QDCOUNT: 1 question
            response.write(0)
            response.write(1)

            // 4. ANCOUNT: 0 answers
            response.write(0)
            response.write(0)

            // 5. NSCOUNT: 0 authority
            response.write(0)
            response.write(0)

            // 6. ARCOUNT: 0 additional
            response.write(0)
            response.write(0)

            // 7. Question section (copied verbatim)
            response.write(query.questionSectionBytes)

            return response.toByteArray()
        }

        /**
         * Builds an RFC 1035 SERVFAIL response packet for upstream errors/timeouts.
         */
        fun buildServFailResponse(query: DnsPacket): ByteArray {
            val response = ByteArrayOutputStream(HEADER_SIZE + query.questionSectionBytes.size)

            // Transaction ID
            response.write((query.transactionId shr 8) and 0xFF)
            response.write(query.transactionId and 0xFF)

            // Flags: QR=1, RA=1, RCODE=2 (Server Failure)
            val rdBit = if (query.isRecursionDesired) 0x0100 else 0x0000
            val flags = 0x8180 or rdBit or RCODE_SERVFAIL
            response.write((flags shr 8) and 0xFF)
            response.write(flags and 0xFF)

            // QDCOUNT = 1
            response.write(0)
            response.write(1)

            // ANCOUNT = 0
            response.write(0)
            response.write(0)

            // NSCOUNT = 0
            response.write(0)
            response.write(0)

            // ARCOUNT = 0
            response.write(0)
            response.write(0)

            response.write(query.questionSectionBytes)
            return response.toByteArray()
        }
    }
}
