package com.rizmi.cleanshield.domain.matcher

import java.util.Locale

/**
 * Normalizes and validates domain names according to RFC 1035 / RFC 1123 rules.
 * Rejects IP addresses, invalid characters, empty strings, and malformed entries.
 */
object DomainNormalizer {

    private val IPV4_REGEX = Regex(
        "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
    )

    private val LABEL_REGEX = Regex("^[a-z0-9]([a-z0-9\\-_]{0,61}[a-z0-9])?$")

    /**
     * Normalizes a raw string into a clean, lowercased domain name.
     * Returns null if the domain is invalid, an IP address, or empty.
     */
    fun normalize(rawInput: String?): String? {
        if (rawInput.isNullOrBlank()) return null

        var domain = rawInput.trim().lowercase(Locale.ROOT)

        // Strip common prefixes if a URL was pasted
        if (domain.startsWith("http://")) {
            domain = domain.removePrefix("http://")
        } else if (domain.startsWith("https://")) {
            domain = domain.removePrefix("https://")
        }

        // Strip path, query params, or port if present
        val slashIdx = domain.indexOf('/')
        if (slashIdx != -1) {
            domain = domain.substring(0, slashIdx)
        }
        val colonIdx = domain.indexOf(':')
        if (colonIdx != -1) {
            domain = domain.substring(0, colonIdx)
        }

        // Strip trailing dot (DNS root label)
        domain = domain.trimEnd('.')

        if (domain.isEmpty() || domain.length > 253) {
            return null
        }

        // Reject if it is an IPv4 or IPv6 address (IPs should not be treated as domains)
        if (IPV4_REGEX.matches(domain) || domain.contains(':') || domain == "0.0.0.0" || domain == "127.0.0.1") {
            return null
        }

        // Validate labels
        val labels = domain.split('.')
        if (labels.isEmpty()) return null

        // RFC 1123 and ICANN: TLD (rightmost label) cannot be all-numeric
        if (labels.last().all { it.isDigit() }) {
            return null
        }

        for (label in labels) {
            if (label.isEmpty() || label.length > 63) {
                return null
            }
            if (!LABEL_REGEX.matches(label)) {
                return null
            }
        }

        return domain
    }

    /**
     * Checks if a domain is valid.
     */
    fun isValid(rawInput: String?): Boolean {
        return normalize(rawInput) != null
    }
}
