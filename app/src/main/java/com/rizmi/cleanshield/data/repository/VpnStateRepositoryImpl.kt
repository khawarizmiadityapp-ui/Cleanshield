package com.rizmi.cleanshield.data.repository

import com.rizmi.cleanshield.domain.model.VpnProtectionState
import com.rizmi.cleanshield.domain.model.VpnStatus
import com.rizmi.cleanshield.domain.repository.VpnStateRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class VpnStateRepositoryImpl : VpnStateRepository {

    private val _state = MutableStateFlow(VpnProtectionState())
    override val state: StateFlow<VpnProtectionState> = _state.asStateFlow()

    override fun updateState(status: VpnStatus, error: String?) {
        _state.update { current ->
            current.copy(
                status = status,
                activeSince = if (status == VpnStatus.RUNNING) {
                    if (current.activeSince == 0L) System.currentTimeMillis() else current.activeSince
                } else if (status == VpnStatus.STOPPED) {
                    0L
                } else {
                    current.activeSince
                },
                lastError = error
            )
        }
    }

    override fun setResolverName(name: String) {
        _state.update { it.copy(currentResolver = name) }
    }
}
