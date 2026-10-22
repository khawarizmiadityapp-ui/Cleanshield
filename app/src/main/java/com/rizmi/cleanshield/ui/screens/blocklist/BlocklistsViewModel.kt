package com.rizmi.cleanshield.ui.screens.blocklist

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rizmi.cleanshield.CleanShieldApp
import com.rizmi.cleanshield.domain.model.BlocklistPreview
import com.rizmi.cleanshield.domain.model.BlocklistSource
import com.rizmi.cleanshield.domain.model.OnlineBlocklistPreset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BlocklistsViewModel : ViewModel() {

    private val app = CleanShieldApp.instance
    private val blocklistRepo = app.blocklistRepository

    val sources: StateFlow<List<BlocklistSource>> = blocklistRepo.getAllSources()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val latestSource: StateFlow<BlocklistSource?> = blocklistRepo.getLatestSource()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val totalBlockedCount: StateFlow<Int> = blocklistRepo.getTotalBlockedCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _pendingPreview = MutableStateFlow<BlocklistPreview?>(null)
    val pendingPreview: StateFlow<BlocklistPreview?> = _pendingPreview.asStateFlow()

    private var pendingUri: Uri? = null
    private var pendingFileSize: Long = 0

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    fun previewFile(uri: Uri, contentResolver: ContentResolver) {
        viewModelScope.launch {
            _isLoading.value = true
            _userMessage.value = null
            try {
                var fileName = "imported_hosts.txt"
                var fileSize = 0L

                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (cursor.moveToFirst()) {
                        if (nameIdx != -1) fileName = cursor.getString(nameIdx)
                        if (sizeIdx != -1) fileSize = cursor.getLong(sizeIdx)
                    }
                }

                val inputStream = contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    _userMessage.value = "Failed to open selected file"
                    _isLoading.value = false
                    return@launch
                }

                val preview = blocklistRepo.previewBlocklist(inputStream, fileName)
                pendingUri = uri
                pendingFileSize = fileSize
                _pendingPreview.value = preview
            } catch (e: Exception) {
                _userMessage.value = "Error parsing file: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun applyPendingPreview() {
        val preview = _pendingPreview.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            val result = blocklistRepo.applyBlocklist(
                preview = preview,
                uriString = pendingUri?.toString(),
                fileSizeBytes = pendingFileSize
            )

            if (result.isSuccess) {
                app.loadDomainRules()
                _userMessage.value = "Successfully applied ${result.getOrNull()} domains"
                _pendingPreview.value = null
                pendingUri = null
            } else {
                _userMessage.value = "Failed to apply blocklist: ${result.exceptionOrNull()?.message}"
            }
            _isLoading.value = false
        }
    }

    fun cancelPreview() {
        _pendingPreview.value = null
        pendingUri = null
    }

    fun loadDefaultBlocklist() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = blocklistRepo.loadDefaultBlocklist()
            if (result.isSuccess) {
                app.loadDomainRules()
                _userMessage.value = "Default security blocklist loaded successfully"
            } else {
                _userMessage.value = "Failed to load default blocklist"
            }
            _isLoading.value = false
        }
    }

    fun clearAllBlocklists() {
        viewModelScope.launch {
            _isLoading.value = true
            blocklistRepo.clearBlocklists()
            app.domainMatcher.clearBlocked()
            _userMessage.value = "All blocklists cleared"
            _isLoading.value = false
        }
    }

    fun downloadPresetBlocklist(preset: OnlineBlocklistPreset) {
        viewModelScope.launch {
            _isLoading.value = true
            _userMessage.value = "Downloading ${preset.name}..."
            val result = blocklistRepo.downloadAndApplyBlocklist(preset.url, preset.name)
            if (result.isSuccess) {
                app.loadDomainRules()
                _userMessage.value = "Successfully applied ${result.getOrNull()} domains from ${preset.name}"
            } else {
                _userMessage.value = "Failed to download ${preset.name}: ${result.exceptionOrNull()?.message}"
            }
            _isLoading.value = false
        }
    }

    fun downloadCustomBlocklist(url: String, name: String = "Custom Online Blocklist") {
        if (url.isBlank()) {
            _userMessage.value = "URL cannot be empty"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            _userMessage.value = "Downloading blocklist from URL..."
            val result = blocklistRepo.downloadAndApplyBlocklist(url.trim(), name.trim())
            if (result.isSuccess) {
                app.loadDomainRules()
                _userMessage.value = "Successfully applied ${result.getOrNull()} domains from online source"
            } else {
                _userMessage.value = "Failed to download blocklist: ${result.exceptionOrNull()?.message}"
            }
            _isLoading.value = false
        }
    }

    fun dismissMessage() {
        _userMessage.value = null
    }
}
