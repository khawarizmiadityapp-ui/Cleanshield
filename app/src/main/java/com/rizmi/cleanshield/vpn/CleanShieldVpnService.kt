package com.rizmi.cleanshield.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.rizmi.cleanshield.CleanShieldApp
import com.rizmi.cleanshield.MainActivity
import com.rizmi.cleanshield.domain.matcher.DomainMatcher
import com.rizmi.cleanshield.domain.model.DnsQueryEvent
import com.rizmi.cleanshield.domain.model.QueryStatus
import com.rizmi.cleanshield.domain.model.VpnStatus
import com.rizmi.cleanshield.vpn.dns.DnsPacket
import com.rizmi.cleanshield.vpn.forwarder.DnsForwarder
import com.rizmi.cleanshield.vpn.packet.IpPacket
import com.rizmi.cleanshield.vpn.packet.UdpPacket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class CleanShieldVpnService : VpnService() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)

    private var vpnInterface: ParcelFileDescriptor? = null
    private var packetLoopJob: Job? = null
    private var dnsForwarder: DnsForwarder? = null

    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    private val app by lazy { CleanShieldApp.instance }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        registerNetworkCallback()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: VpnController.ACTION_START

        when (action) {
            VpnController.ACTION_STOP -> {
                stopProtectionService()
                return START_NOT_STICKY
            }
            VpnController.ACTION_START -> {
                startProtectionService()
                return START_STICKY
            }
        }
        return START_NOT_STICKY
    }

    private fun startProtectionService() {
        app.vpnStateRepository.updateState(VpnStatus.STARTING)

        val notification = buildForegroundNotification("CleanShield Protection Starting...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            } catch (e: Exception) {
                startForeground(NOTIFICATION_ID, notification)
            }
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        serviceScope.launch {
            try {
                // Initialize forwarder with user's selected resolver
                val resolver = app.preferencesRepository.selectedResolver.first()
                dnsForwarder = DnsForwarder(this@CleanShieldVpnService, resolver = resolver)
                app.vpnStateRepository.setResolverName("${resolver.name} (${resolver.primaryIp})")

                // Establish TUN interface
                val pfd = establishTunInterface()
                if (pfd == null) {
                    app.vpnStateRepository.updateState(VpnStatus.ERROR, "Failed to establish TUN interface")
                    stopSelf()
                    return@launch
                }
                vpnInterface = pfd

                // Update notification and state
                updateNotification("Active: Blocking Ads & Trackers")
                app.vpnStateRepository.updateState(VpnStatus.RUNNING)
                app.preferencesRepository.setProtectionActive(true)

                // Start packet processing loop
                startPacketProcessing(pfd)
            } catch (e: Exception) {
                app.vpnStateRepository.updateState(VpnStatus.ERROR, e.message ?: "Unknown startup error")
                stopProtectionService()
            }
        }
    }

    private fun establishTunInterface(): ParcelFileDescriptor? {
        val builder = Builder()
            .setSession("CleanShield")
            .setMtu(1500)
            .addAddress("10.255.0.2", 32)
            .addDnsServer("10.255.0.1")
            .addRoute("10.255.0.1", 32)
            .setBlocking(true)

        // Intercept common hardcoded public DNS servers (anti-bypass)
        val hardcodedDnsServers = listOf(
            "8.8.8.8", "8.8.4.4",         // Google DNS
            "1.1.1.1", "1.0.0.1",         // Cloudflare DNS
            "9.9.9.9", "149.112.112.112", // Quad9
            "208.67.222.222", "208.67.220.220" // OpenDNS
        )
        for (dnsIp in hardcodedDnsServers) {
            try {
                builder.addRoute(dnsIp, 32)
            } catch (_: Exception) {}
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            builder.setMetered(false)
        }

        return builder.establish()
    }

    private fun startPacketProcessing(pfd: ParcelFileDescriptor) {
        packetLoopJob?.cancel()
        packetLoopJob = serviceScope.launch(Dispatchers.IO) {
            val inStream = FileInputStream(pfd.fileDescriptor)
            val outStream = FileOutputStream(pfd.fileDescriptor)
            val packetBuffer = ByteArray(4096)

            while (isActive) {
                try {
                    val length = inStream.read(packetBuffer)
                    if (length <= 0) continue

                    val ipPacket = IpPacket.parse(packetBuffer, length) ?: continue

                    // Process UDP DNS traffic
                    if (ipPacket.isUdp) {
                        val udpPacket = UdpPacket.parse(
                            packetBuffer,
                            ipPacket.payloadOffset,
                            ipPacket.payloadLength
                        ) ?: continue

                        if (udpPacket.destPort == 53) {
                            processDnsQuery(ipPacket, udpPacket, outStream)
                        }
                    }
                } catch (e: IOException) {
                    if (!isActive) break
                } catch (e: Exception) {
                    // Suppress per-packet unhandled exception to keep TUN loop resilient
                }
            }
        }
    }

    private suspend fun processDnsQuery(
        ipPacket: IpPacket,
        udpPacket: UdpPacket,
        outStream: FileOutputStream
    ) {
        val udpPayload = udpPacket.getPayload()
        val dnsPacket = DnsPacket.parse(udpPayload, 0, udpPayload.size) ?: return

        if (!dnsPacket.isQuery || dnsPacket.firstDomain == null) return

        val domain = dnsPacket.firstDomain!!
        val queryType = dnsPacket.firstType
        val startTime = System.currentTimeMillis()

        val matchResult = app.domainMatcher.evaluate(domain)

        when (matchResult) {
            is DomainMatcher.Result.Blocked -> {
                // Generate NXDOMAIN DNS response packet
                val nxDomainBytes = DnsPacket.buildNxDomainResponse(dnsPacket)

                // Package in UDP response (swap ports & IPs)
                val udpResponse = UdpPacket.buildUdpPacket(
                    srcIp = ipPacket.destIpBytes,
                    dstIp = ipPacket.sourceIpBytes,
                    srcPort = udpPacket.destPort,
                    dstPort = udpPacket.sourcePort,
                    payload = nxDomainBytes
                )

                // Package in IPv4 response
                val ipResponse = IpPacket.buildIpv4Packet(
                    srcIp = ipPacket.destIpBytes,
                    dstIp = ipPacket.sourceIpBytes,
                    protocol = IpPacket.PROTOCOL_UDP,
                    payload = udpResponse
                )

                // Write packet back to TUN
                synchronized(outStream) {
                    try {
                        outStream.write(ipResponse)
                    } catch (_: IOException) {}
                }

                val latency = System.currentTimeMillis() - startTime
                recordLog(
                    domain = domain,
                    queryType = queryType,
                    status = QueryStatus.BLOCKED,
                    reason = "Matched rule: ${matchResult.matchedRule}",
                    responseTimeMs = latency
                )
            }

            is DomainMatcher.Result.Allowed, is DomainMatcher.Result.Invalid -> {
                val forwarder = dnsForwarder
                val responseBytes = forwarder?.forward(dnsPacket)

                val latency = System.currentTimeMillis() - startTime

                if (responseBytes != null) {
                    val udpResponse = UdpPacket.buildUdpPacket(
                        srcIp = ipPacket.destIpBytes,
                        dstIp = ipPacket.sourceIpBytes,
                        srcPort = udpPacket.destPort,
                        dstPort = udpPacket.sourcePort,
                        payload = responseBytes
                    )

                    val ipResponse = IpPacket.buildIpv4Packet(
                        srcIp = ipPacket.destIpBytes,
                        dstIp = ipPacket.sourceIpBytes,
                        protocol = IpPacket.PROTOCOL_UDP,
                        payload = udpResponse
                    )

                    synchronized(outStream) {
                        try {
                            outStream.write(ipResponse)
                        } catch (_: IOException) {}
                    }

                    recordLog(
                        domain = domain,
                        queryType = queryType,
                        status = QueryStatus.FORWARDED,
                        reason = if (matchResult is DomainMatcher.Result.Allowed) matchResult.reason else null,
                        responseTimeMs = latency,
                        resolver = forwarder.resolver.primaryIp
                    )
                } else {
                    // Send SERVFAIL if upstream failed
                    val servFailBytes = DnsPacket.buildServFailResponse(dnsPacket)
                    val udpResponse = UdpPacket.buildUdpPacket(
                        srcIp = ipPacket.destIpBytes,
                        dstIp = ipPacket.sourceIpBytes,
                        srcPort = udpPacket.destPort,
                        dstPort = udpPacket.sourcePort,
                        payload = servFailBytes
                    )
                    val ipResponse = IpPacket.buildIpv4Packet(
                        srcIp = ipPacket.destIpBytes,
                        dstIp = ipPacket.sourceIpBytes,
                        protocol = IpPacket.PROTOCOL_UDP,
                        payload = udpResponse
                    )
                    synchronized(outStream) {
                        try {
                            outStream.write(ipResponse)
                        } catch (_: IOException) {}
                    }

                    recordLog(
                        domain = domain,
                        queryType = queryType,
                        status = QueryStatus.FAILED,
                        reason = "Upstream resolver timeout or failure",
                        responseTimeMs = latency
                    )
                }
            }
        }
    }

    private fun recordLog(
        domain: String,
        queryType: String,
        status: QueryStatus,
        reason: String?,
        responseTimeMs: Long,
        resolver: String? = null
    ) {
        serviceScope.launch {
            val loggingEnabled = app.preferencesRepository.loggingEnabled.first()
            if (loggingEnabled) {
                app.activityLogRepository.logEvent(
                    DnsQueryEvent(
                        domain = domain,
                        queryType = queryType,
                        clientIp = "10.255.0.2",
                        status = status,
                        blockReason = reason,
                        responseTimeMs = responseTimeMs,
                        upstreamResolver = resolver
                    )
                )
            }
        }
    }

    private fun stopProtectionService() {
        app.vpnStateRepository.updateState(VpnStatus.STOPPING)

        packetLoopJob?.cancel()
        packetLoopJob = null

        try {
            vpnInterface?.close()
        } catch (_: Exception) {}
        vpnInterface = null

        dnsForwarder?.clearCache()
        dnsForwarder = null

        app.vpnStateRepository.updateState(VpnStatus.STOPPED)
        serviceScope.launch {
            app.preferencesRepository.setProtectionActive(false)
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onRevoke() {
        stopProtectionService()
        super.onRevoke()
    }

    override fun onDestroy() {
        unregisterNetworkCallback()
        stopProtectionService()
        serviceJob.cancel()
        super.onDestroy()
    }

    private fun registerNetworkCallback() {
        try {
            connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    dnsForwarder?.clearCache()
                }

                override fun onLost(network: Network) {
                    dnsForwarder?.clearCache()
                }
            }
            connectivityManager?.registerNetworkCallback(request, networkCallback!!)
        } catch (_: Exception) {}
    }

    private fun unregisterNetworkCallback() {
        try {
            networkCallback?.let { connectivityManager?.unregisterNetworkCallback(it) }
        } catch (_: Exception) {}
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "CleanShield VPN Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows status of CleanShield local DNS ad blocker"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(contentText: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, CleanShieldVpnService::class.java).apply {
            action = VpnController.ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("CleanShield Active")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(openAppPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(NOTIFICATION_ID, buildForegroundNotification(text))
    }

    companion object {
        private const val CHANNEL_ID = "cleanshield_vpn_channel"
        private const val NOTIFICATION_ID = 1001
    }
}
