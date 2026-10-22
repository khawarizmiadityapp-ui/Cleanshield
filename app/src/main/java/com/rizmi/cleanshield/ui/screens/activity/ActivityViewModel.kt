package com.rizmi.cleanshield.ui.screens.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rizmi.cleanshield.CleanShieldApp
import com.rizmi.cleanshield.domain.model.DnsQueryEvent
import com.rizmi.cleanshield.domain.model.QueryStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ActivityViewModel : ViewModel() {

    private val activityRepo = CleanShieldApp.instance.activityLogRepository

    private val _selectedFilter = MutableStateFlow("ALL")
    val selectedFilter: StateFlow<String> = _selectedFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val rawEvents = activityRepo.getRecentEvents(300)

    val filteredEvents: StateFlow<List<DnsQueryEvent>> = combine(
        rawEvents,
        _selectedFilter,
        _searchQuery
    ) { events, filter, query ->
        events.filter { event ->
            val matchesFilter = when (filter) {
                "BLOCKED" -> event.status == QueryStatus.BLOCKED
                "ALLOWED" -> event.status == QueryStatus.ALLOWED || event.status == QueryStatus.FORWARDED
                "FAILED" -> event.status == QueryStatus.FAILED
                else -> true
            }

            val matchesSearch = query.isBlank() || event.domain.contains(query, ignoreCase = true)
            matchesFilter && matchesSearch
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setFilter(filter: String) {
        _selectedFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun clearHistory() {
        viewModelScope.launch {
            activityRepo.clearHistory()
        }
    }
}
