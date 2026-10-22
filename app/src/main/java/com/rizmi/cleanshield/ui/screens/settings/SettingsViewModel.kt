package com.rizmi.cleanshield.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rizmi.cleanshield.CleanShieldApp
import com.rizmi.cleanshield.domain.model.DnsResolverConfig
import com.rizmi.cleanshield.domain.model.VpnStatus
import com.rizmi.cleanshield.vpn.VpnController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel : ViewModel() {

    private val app = CleanShieldApp.instance
    private val prefRepo = app.preferencesRepository
    private val vpnRepo = app.vpnStateRepository

    val selectedResolver: StateFlow<DnsResolverConfig> = prefRepo.selectedResolver
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DnsResolverConfig.CLOUDFLARE)

    val customResolverIp: StateFlow<String> = prefRepo.customResolverIp
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val loggingEnabled: StateFlow<Boolean> = prefRepo.loggingEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val notificationsEnabled: StateFlow<Boolean> = prefRepo.notificationsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    private val _customIpInput = MutableStateFlow("")
    val customIpInput: StateFlow<String> = _customIpInput.asStateFlow()

    private val _ipError = MutableStateFlow<String?>(null)
    val ipError: StateFlow<String?> = _ipError.asStateFlow()

    init {
        viewModelScope.launch {
            prefRepo.customResolverIp.collect { ip ->
                if (_customIpInput.value.isEmpty()) {
                    _customIpInput.value = ip
                }
            }
        }
    }

    fun onCustomIpChanged(ip: String) {
        _customIpInput.value = ip
        _ipError.value = null
    }

    fun selectResolver(resolver: DnsResolverConfig) {
        viewModelScope.launch {
            if (resolver.isCustom) {
                val ip = _customIpInput.value.trim()
                if (!isValidIp(ip)) {
                    _ipError.value = "Enter a valid IPv4 address (e.g. 1.1.1.1)"
                    return@launch
                }
                prefRepo.setResolver("custom", ip)
            } else {
                prefRepo.setResolver(resolver.id)
            }
        }
    }

    fun setLoggingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefRepo.setLoggingEnabled(enabled)
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefRepo.setNotificationsEnabled(enabled)
        }
    }

    fun restartProtection(context: Context) {
        viewModelScope.launch {
            val isRunning = vpnRepo.state.value.status == VpnStatus.RUNNING
            if (isRunning) {
                VpnController.stopProtection(context)
                kotlinx.coroutines.delay(500)
                VpnController.startProtection(context)
            }
        }
    }

    private fun isValidIp(ip: String): Boolean {
        return ip.matches(Regex("^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"))
    }
}
