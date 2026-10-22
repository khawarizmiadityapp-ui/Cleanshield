package com.rizmi.cleanshield.data.parser

import com.rizmi.cleanshield.domain.matcher.DomainNormalizer
import com.rizmi.cleanshield.domain.model.BlocklistPreview
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

/**
 * Parses hosts files, Adblock Plus / AdGuard syntax, and plain domain list files line-by-line.
 * Supported formats:
 * - 0.0.0.0 ads.example.com (Hosts format)
 * - 127.0.0.1 tracker.example.com
 * - ||ads.example.com^ (Adblock Plus / AdGuard DNS format)
 * - ||ads.example.com^$third-party
 * - plain-domain.com (Plain domain per line)
 * - Comments starting with # or ! or [
 *
 * Implements strict memory and format validation.
 */
object BlocklistParser {

    private val IGNORED_HOSTS = setOf(
        "localhost",
        "localhost.localdomain",
        "local",
        "broadcasthost",
        "ip6-localhost",
        "ip6-loopback"
    )

    /**
     * Parses an input stream into a BlocklistPreview with validated domains and metrics.
     * Capped to avoid OOM on excessively large files.
     */
    fun parse(
        inputStream: InputStream,
        fileName: String,
        maxLines: Int = 500_000
    ): BlocklistPreview {
        val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
        val validDomains = LinkedHashSet<String>()
        val sampleDomains = mutableListOf<String>()

        var totalLines = 0
        var validCount = 0
        var invalidCount = 0
        var duplicateCount = 0

        reader.useLines { lines ->
            for (rawLine in lines) {
                totalLines++
                if (totalLines > maxLines) {
                    break
                }

                val trimmed = rawLine.trim()
                // Skip empty lines, metadata headers, cosmetic filters, and Adblock allowlist rules
                if (trimmed.isEmpty() ||
                    trimmed.startsWith("!") ||
                    trimmed.startsWith("[") ||
                    trimmed.startsWith("@@") ||
                    trimmed.contains("##") ||
                    trimmed.contains("#@#") ||
                    trimmed.contains("#?#")
                ) {
                    continue
                }

                // Strip inline comment (# or !)
                val lineWithoutComment = stripComment(trimmed).trim()
                if (lineWithoutComment.isEmpty()) {
                    continue
                }

                // Handle Adblock / AdGuard syntax: ||example.com^ or ||example.com^$options
                val candidateDomains = if (lineWithoutComment.startsWith("||")) {
                    val cleaned = lineWithoutComment
                        .removePrefix("||")
                        .removePrefix("*.")
                        .substringBefore('^')
                        .substringBefore('$')
                        .trim()
                    if (cleaned.isNotEmpty()) listOf(cleaned) else emptyList()
                } else {
                    // Tokens separated by whitespace
                    val tokens = lineWithoutComment.split(Regex("\\s+")).filter { it.isNotEmpty() }
                    if (tokens.isEmpty()) {
                        continue
                    }

                    // Check hosts format: IP followed by domain(s)
                    if (isIpAddress(tokens[0])) {
                        tokens.drop(1)
                    } else {
                        // Single domain per line
                        listOf(tokens[0].removePrefix("*."))
                    }
                }

                if (candidateDomains.isEmpty()) {
                    invalidCount++
                    continue
                }

                for (candidate in candidateDomains) {
                    val cleanCandidate = candidate
                        .removePrefix("||")
                        .removePrefix("*.")
                        .substringBefore('^')
                        .substringBefore('$')
                        .trim()

                    val normalized = DomainNormalizer.normalize(cleanCandidate)
                    if (normalized == null || IGNORED_HOSTS.contains(normalized)) {
                        invalidCount++
                    } else {
                        if (validDomains.contains(normalized)) {
                            duplicateCount++
                        } else {
                            validDomains.add(normalized)
                            validCount++
                            if (sampleDomains.size < 20) {
                                sampleDomains.add(normalized)
                            }
                        }
                    }
                }
            }
        }

        return BlocklistPreview(
            fileName = fileName,
            totalLines = totalLines,
            validCount = validCount,
            invalidCount = invalidCount,
            duplicateCount = duplicateCount,
            sampleValidDomains = sampleDomains,
            parsedDomains = validDomains
        )
    }

    private fun stripComment(line: String): String {
        val hashIdx = line.indexOf('#')
        val exclIdx = line.indexOf('!')
        val cutIdx = when {
            hashIdx != -1 && exclIdx != -1 -> minOf(hashIdx, exclIdx)
            hashIdx != -1 -> hashIdx
            exclIdx != -1 -> exclIdx
            else -> -1
        }
        return if (cutIdx != -1) line.substring(0, cutIdx) else line
    }

    private fun isIpAddress(token: String): Boolean {
        return token == "0.0.0.0" ||
                token == "127.0.0.1" ||
                token == "::1" ||
                token == "::" ||
                token.matches(Regex("^\\d{1,3}(\\.\\d{1,3}){3}$"))
    }
}
