package com.rizmi.cleanshield.ui.screens.dashboard

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rizmi.cleanshield.CleanShieldApp
import com.rizmi.cleanshield.domain.model.DnsQueryEvent
import com.rizmi.cleanshield.domain.model.ProtectionStats
import com.rizmi.cleanshield.domain.model.VpnProtectionState
import com.rizmi.cleanshield.domain.model.VpnStatus
import com.rizmi.cleanshield.vpn.VpnController
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

class DashboardViewModel : ViewModel() {

    private val app = CleanShieldApp.instance
    private val vpnRepo = app.vpnStateRepository
    private val activityRepo = app.activityLogRepository
    private val blocklistRepo = app.blocklistRepository

    val vpnState: StateFlow<VpnProtectionState> = vpnRepo.state

    val stats: StateFlow<ProtectionStats> = activityRepo.getProtectionStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProtectionStats())

    val totalBlockedRules: StateFlow<Int> = blocklistRepo.getTotalBlockedCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val recentEvents: StateFlow<List<DnsQueryEvent>> = activityRepo.getRecentEvents(5)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _sessionTimer = MutableStateFlow("00:00:00")
    val sessionTimer: StateFlow<String> = _sessionTimer.asStateFlow()

    init {
        // Live session duration timer tick
        viewModelScope.launch {
            while (true) {
                val state = vpnState.value
                if (state.status == VpnStatus.RUNNING && state.activeSince > 0) {
                    val durationMs = System.currentTimeMillis() - state.activeSince
                    val seconds = (durationMs / 1000) % 60
                    val minutes = (durationMs / (1000 * 60)) % 60
                    val hours = (durationMs / (1000 * 60 * 60))
                    _sessionTimer.value = String.format(Locale.ROOT, "%02d:%02d:%02d", hours, minutes, seconds)
                } else {
                    _sessionTimer.value = "00:00:00"
                }
                delay(1000)
            }
        }
    }

    /**
     * Toggles protection on/off.
     * Returns Intent if VPN permission is required from user.
     */
    fun toggleProtection(context: Context): Intent? {
        val currentState = vpnState.value.status
        if (currentState == VpnStatus.RUNNING || currentState == VpnStatus.STARTING) {
            VpnController.stopProtection(context)
            return null
        } else {
            val prepareIntent = VpnController.checkVpnPermission(context)
            if (prepareIntent != null) {
                return prepareIntent
            }
            VpnController.startProtection(context)
            return null
        }
    }

    fun startProtectionDirectly(context: Context) {
        VpnController.startProtection(context)
    }
}
