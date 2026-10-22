package com.rizmi.cleanshield.vpn

import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.core.content.ContextCompat

object VpnController {

    const val ACTION_START = "com.rizmi.cleanshield.START_VPN"
    const val ACTION_STOP = "com.rizmi.cleanshield.STOP_VPN"

    /**
     * Checks if the system VPN permission has been granted.
     * Returns null if permission is already granted, or an Intent to request it.
     */
    fun checkVpnPermission(context: Context): Intent? {
        return VpnService.prepare(context)
    }

    /**
     * Starts the CleanShield VPN Service.
     */
    fun startProtection(context: Context) {
        val intent = Intent(context, CleanShieldVpnService::class.java).apply {
            action = ACTION_START
        }
        ContextCompat.startForegroundService(context, intent)
    }

    /**
     * Stops the CleanShield VPN Service.
     */
    fun stopProtection(context: Context) {
        val intent = Intent(context, CleanShieldVpnService::class.java).apply {
            action = ACTION_STOP
        }
        context.startService(intent)
    }
}
