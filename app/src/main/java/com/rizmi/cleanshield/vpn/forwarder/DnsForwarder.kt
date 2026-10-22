package com.rizmi.cleanshield.vpn.forwarder

import android.net.VpnService
import com.rizmi.cleanshield.domain.model.DnsResolverConfig
import com.rizmi.cleanshield.vpn.dns.DnsCache
import com.rizmi.cleanshield.vpn.dns.DnsPacket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException

/**
 * Forwards allowed DNS queries to upstream resolver via protected DatagramSocket.
 * Implements DNS caching, timeout handling, and retry logic.
 */
class DnsForwarder(
    private val vpnService: VpnService,
    private val dnsCache: DnsCache = DnsCache(),
    var resolver: DnsResolverConfig = DnsResolverConfig.ADGUARD,
    private val timeoutMs: Int = 2500
) {
    /**
     * Resolves a DNS query using cache or upstream UDP forwarding.
     * Returns null if forwarding fails after retries.
     */
    suspend fun forward(dnsPacket: DnsPacket): ByteArray? = withContext(Dispatchers.IO) {
        val firstQuestion = dnsPacket.questions.firstOrNull()

        // 1. Check in-memory DNS cache
        if (firstQuestion != null) {
            val cached = dnsCache.get(firstQuestion.name, firstQuestion.type, dnsPacket.transactionId)
            if (cached != null) {
                return@withContext cached
            }
        }

        // 2. Forward to upstream with retry (up to 2 attempts)
        val upstreamIp = try {
            InetAddress.getByName(resolver.primaryIp)
        } catch (e: Exception) {
            return@withContext null
        }

        var socket: DatagramSocket? = null
        try {
            socket = DatagramSocket()
            // CRITICAL: Protect socket from VPN routing loop
            val protected = vpnService.protect(socket)
            if (!protected) {
                return@withContext null
            }
            socket.soTimeout = timeoutMs

            val sendPacket = DatagramPacket(
                dnsPacket.rawBytes,
                dnsPacket.rawBytes.size,
                upstreamIp,
                resolver.port
            )

            val receiveBuffer = ByteArray(4096)
            val receivePacket = DatagramPacket(receiveBuffer, receiveBuffer.size)

            var attempts = 0
            val maxAttempts = 2

            while (attempts < maxAttempts) {
                attempts++
                try {
                    socket.send(sendPacket)
                    socket.receive(receivePacket)

                    val responseLength = receivePacket.length
                    if (responseLength >= DnsPacket.HEADER_SIZE) {
                        val responseData = ByteArray(responseLength)
                        System.arraycopy(receiveBuffer, 0, responseData, 0, responseLength)

                        // Cache if valid response
                        if (firstQuestion != null) {
                            dnsCache.put(firstQuestion.name, firstQuestion.type, responseData)
                        }

                        return@withContext responseData
                    }
                } catch (e: SocketTimeoutException) {
                    if (attempts >= maxAttempts) {
                        return@withContext null
                    }
                } catch (e: Exception) {
                    return@withContext null
                }
            }

            null
        } catch (e: Exception) {
            null
        } finally {
            try {
                socket?.close()
            } catch (_: Exception) {}
        }
    }

    fun clearCache() {
        dnsCache.clear()
    }
}
