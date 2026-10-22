package com.rizmi.cleanshield

import android.app.Application
import com.rizmi.cleanshield.data.local.CleanShieldDatabase
import com.rizmi.cleanshield.data.local.PreferencesManager
import com.rizmi.cleanshield.data.repository.ActivityLogRepositoryImpl
import com.rizmi.cleanshield.data.repository.BlocklistRepositoryImpl
import com.rizmi.cleanshield.data.repository.PreferencesRepositoryImpl
import com.rizmi.cleanshield.data.repository.VpnStateRepositoryImpl
import com.rizmi.cleanshield.domain.matcher.DomainMatcher
import com.rizmi.cleanshield.domain.repository.ActivityLogRepository
import com.rizmi.cleanshield.domain.repository.BlocklistRepository
import com.rizmi.cleanshield.domain.repository.PreferencesRepository
import com.rizmi.cleanshield.domain.repository.VpnStateRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CleanShieldApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var database: CleanShieldDatabase
        private set
    lateinit var preferencesManager: PreferencesManager
        private set

    lateinit var blocklistRepository: BlocklistRepository
        private set
    lateinit var activityLogRepository: ActivityLogRepository
        private set
    lateinit var preferencesRepository: PreferencesRepository
        private set
    lateinit var vpnStateRepository: VpnStateRepository
        private set

    val domainMatcher = DomainMatcher()

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = CleanShieldDatabase.getInstance(this)
        preferencesManager = PreferencesManager(this)

        blocklistRepository = BlocklistRepositoryImpl(
            blocklistDao = database.blocklistDao(),
            allowlistDao = database.allowlistDao()
        )
        activityLogRepository = ActivityLogRepositoryImpl(database.dnsEventDao())
        preferencesRepository = PreferencesRepositoryImpl(preferencesManager)
        vpnStateRepository = VpnStateRepositoryImpl()

        // Load blocklists and allowlists into in-memory DomainMatcher
        loadDomainRules()
    }

    fun loadDomainRules() {
        applicationScope.launch(Dispatchers.IO) {
            val blocked = blocklistRepository.getActiveBlockedDomains()
            val allowed = blocklistRepository.getActiveAllowedDomains()

            // If empty on very first install, load default curated blocklist
            if (blocked.isEmpty()) {
                blocklistRepository.loadDefaultBlocklist()
                val reloadedBlocked = blocklistRepository.getActiveBlockedDomains()
                domainMatcher.setBlockedDomains(reloadedBlocked)
            } else {
                domainMatcher.setBlockedDomains(blocked)
            }

            domainMatcher.setAllowedDomains(allowed)
        }
    }

    companion object {
        lateinit var instance: CleanShieldApp
            private set
    }
}
