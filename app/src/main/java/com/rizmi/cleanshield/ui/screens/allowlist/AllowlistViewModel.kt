package com.rizmi.cleanshield.ui.screens.allowlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rizmi.cleanshield.CleanShieldApp
import com.rizmi.cleanshield.domain.matcher.DomainNormalizer
import com.rizmi.cleanshield.domain.model.AllowlistRule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AllowlistViewModel : ViewModel() {

    private val app = CleanShieldApp.instance
    private val blocklistRepo = app.blocklistRepository

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val rawAllowlist = blocklistRepo.getAllowlist()

    val filteredRules: StateFlow<List<AllowlistRule>> = combine(
        rawAllowlist,
        _searchQuery
    ) { rules, query ->
        if (query.isBlank()) rules
        else rules.filter { it.domain.contains(query, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalCount: StateFlow<Int> = blocklistRepo.getAllowlistCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _inputDomain = MutableStateFlow("")
    val inputDomain: StateFlow<String> = _inputDomain.asStateFlow()

    private val _inputError = MutableStateFlow<String?>(null)
    val inputError: StateFlow<String?> = _inputError.asStateFlow()

    fun onInputDomainChanged(text: String) {
        _inputDomain.value = text
        _inputError.value = null
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun addDomain() {
        val raw = _inputDomain.value.trim()
        val normalized = DomainNormalizer.normalize(raw)
        if (normalized == null) {
            _inputError.value = "Invalid domain name. Must be RFC compliant (e.g. example.com)"
            return
        }

        viewModelScope.launch {
            val success = blocklistRepo.addAllowedDomain(normalized)
            if (success) {
                app.domainMatcher.addAllowedDomain(normalized)
                _inputDomain.value = ""
                _inputError.value = null
            } else {
                _inputError.value = "Failed to add domain"
            }
        }
    }

    fun removeDomain(domain: String) {
        viewModelScope.launch {
            blocklistRepo.removeAllowedDomain(domain)
            app.domainMatcher.removeAllowedDomain(domain)
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            blocklistRepo.clearAllowlist()
            app.domainMatcher.clearAllowed()
        }
    }
}
