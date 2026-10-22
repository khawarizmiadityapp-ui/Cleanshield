package com.rizmi.cleanshield.domain.matcher

import java.util.concurrent.ConcurrentHashMap

/**
 * High-performance domain matcher supporting exact matches, subdomain inheritance,
 * and strict allowlist precedence.
 *
 * Matching rule:
 * - If "example.com" is blocked:
 *     - "example.com" -> BLOCKED
 *     - "ads.example.com" -> BLOCKED
 *     - "sub.ads.example.com" -> BLOCKED
 *     - "notexample.com" -> NOT BLOCKED
 *     - "example.com.tracker.org" -> NOT BLOCKED
 * - If "sub.example.com" is in allowlist:
 *     - "sub.example.com" -> ALLOWED (Allowlist takes precedence over blocklist)
 */
class DomainMatcher {

    // Concurrent sets backed by ConcurrentHashMap for thread-safe concurrent lookups
    private val blockedSet = ConcurrentHashMap.newKeySet<String>()
    private val allowedSet = ConcurrentHashMap.newKeySet<String>()

    sealed class Result {
        data class Blocked(val matchedRule: String) : Result()
        data class Allowed(val reason: String) : Result()
        data object Invalid : Result()
    }

    /**
     * Atomically replaces the current blocklist with the new set of normalized domains.
     */
    fun setBlockedDomains(domains: Collection<String>) {
        blockedSet.clear()
        for (d in domains) {
            DomainNormalizer.normalize(d)?.let { blockedSet.add(it) }
        }
    }

    /**
     * Atomically replaces the current allowlist with the new set of normalized domains.
     */
    fun setAllowedDomains(domains: Collection<String>) {
        allowedSet.clear()
        for (d in domains) {
            DomainNormalizer.normalize(d)?.let { allowedSet.add(it) }
        }
    }

    /**
     * Adds an allowed domain.
     */
    fun addAllowedDomain(domain: String): Boolean {
        val normalized = DomainNormalizer.normalize(domain) ?: return false
        return allowedSet.add(normalized)
    }

    /**
     * Removes an allowed domain.
     */
    fun removeAllowedDomain(domain: String): Boolean {
        val normalized = DomainNormalizer.normalize(domain) ?: return false
        return allowedSet.remove(normalized)
    }

    /**
     * Clears all blocked domains.
     */
    fun clearBlocked() {
        blockedSet.clear()
    }

    /**
     * Clears all allowed domains.
     */
    fun clearAllowed() {
        allowedSet.clear()
    }

    /**
     * Returns the total count of active blocked rules.
     */
    fun getBlockedCount(): Int = blockedSet.size

    /**
     * Returns the total count of active allowed rules.
     */
    fun getAllowedCount(): Int = allowedSet.size

    /**
     * Evaluates a domain against allowlist and blocklist.
     * Allowlist check is executed first for maximum safety.
     */
    fun evaluate(rawDomain: String?): Result {
        val normalized = DomainNormalizer.normalize(rawDomain) ?: return Result.Invalid

        // 1. Check Allowlist (exact or parent subdomain)
        val matchedAllow = findMatchingRule(normalized, allowedSet)
        if (matchedAllow != null) {
            return Result.Allowed("Allowlisted (matched: $matchedAllow)")
        }

        // 2. Check Blocklist (exact or parent subdomain)
        val matchedBlock = findMatchingRule(normalized, blockedSet)
        if (matchedBlock != null) {
            return Result.Blocked(matchedBlock)
        }

        return Result.Allowed("Not in blocklist")
    }

    /**
     * Checks if the domain itself or any parent domain matches any entry in the set.
     * E.g. for "ad.cdn.example.com", checks:
     * 1. "ad.cdn.example.com"
     * 2. "cdn.example.com"
     * 3. "example.com"
     * 4. "com" (will only match if "com" itself was blocked, which is standard)
     */
    private fun findMatchingRule(domain: String, set: Set<String>): String? {
        if (set.contains(domain)) {
            return domain
        }

        var currentIndex = 0
        while (currentIndex < domain.length) {
            val dotIndex = domain.indexOf('.', currentIndex)
            if (dotIndex == -1 || dotIndex == domain.length - 1) {
                break
            }
            val parentDomain = domain.substring(dotIndex + 1)
            if (set.contains(parentDomain)) {
                return parentDomain
            }
            currentIndex = dotIndex + 1
        }
        return null
    }
}
