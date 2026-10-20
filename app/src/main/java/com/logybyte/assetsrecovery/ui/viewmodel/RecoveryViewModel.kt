package com.logybyte.assetsrecovery.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.logybyte.assetsrecovery.data.model.FilterType
import com.logybyte.assetsrecovery.data.model.MediaItem
import com.logybyte.assetsrecovery.data.model.MediaType
import com.logybyte.assetsrecovery.data.model.RecoveryResult
import com.logybyte.assetsrecovery.data.model.RecoveryStatus
import com.logybyte.assetsrecovery.data.model.ScanResult
import com.logybyte.assetsrecovery.data.model.SortOrder
import com.logybyte.assetsrecovery.data.recovery.RecoveryEngine
import com.logybyte.assetsrecovery.data.scanner.MediaScanner
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class ScanUiState {
    object Idle : ScanUiState()
    data class Scanning(val count: Int, val currentFile: String) : ScanUiState()
    data class Finished(val result: ScanResult) : ScanUiState()
    data class Error(val message: String) : ScanUiState()
}

class RecoveryViewModel(application: Application) : AndroidViewModel(application) {

    private val mediaScanner = MediaScanner(application)
    private val recoveryEngine = RecoveryEngine(application)

    private var scanJob: Job? = null

    private val _scanState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val scanState: StateFlow<ScanUiState> = _scanState.asStateFlow()

    private val _allMediaItems = MutableStateFlow<List<MediaItem>>(emptyList())
    val allMediaItems: StateFlow<List<MediaItem>> = _allMediaItems.asStateFlow()

    private val _selectedUris = MutableStateFlow<Set<Uri>>(emptySet())
    val selectedUris: StateFlow<Set<Uri>> = _selectedUris.asStateFlow()

    private val _filterType = MutableStateFlow(FilterType.ALL)
    val filterType: StateFlow<FilterType> = _filterType.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.DATE_DESC)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private val _isRecovering = MutableStateFlow(false)
    val isRecovering: StateFlow<Boolean> = _isRecovering.asStateFlow()

    private val _recoveryProgress = MutableStateFlow<Pair<Int, Int>?>(null)
    val recoveryProgress: StateFlow<Pair<Int, Int>?> = _recoveryProgress.asStateFlow()

    private val _recoveryResult = MutableStateFlow<RecoveryResult?>(null)
    val recoveryResult: StateFlow<RecoveryResult?> = _recoveryResult.asStateFlow()

    private val _lastScanSummary = MutableStateFlow<ScanResult?>(null)
    val lastScanSummary: StateFlow<ScanResult?> = _lastScanSummary.asStateFlow()

    fun startScan() {
        if (scanJob?.isActive == true) return

        scanJob = viewModelScope.launch {
            _scanState.value = ScanUiState.Scanning(0, "Initializing scan...")
            _selectedUris.value = emptySet()
            val discoveredList = mutableListOf<MediaItem>()
            val startTime = System.currentTimeMillis()

            try {
                mediaScanner.scanMedia().collect { progress ->
                    if (progress.lastDiscoveredItem != null) {
                        discoveredList.add(progress.lastDiscoveredItem)
                        _allMediaItems.value = discoveredList.toList()
                    }
                    _scanState.value = ScanUiState.Scanning(progress.scannedCount, progress.currentPath)
                }

                val duration = System.currentTimeMillis() - startTime
                val imagesFound = discoveredList.count { it.mediaType == MediaType.IMAGE }
                val videosFound = discoveredList.count { it.mediaType == MediaType.VIDEO }
                val trashedCount = discoveredList.count { it.isTrashed }
                val hiddenCount = discoveredList.count { it.isHidden }
                val recoverableCount = discoveredList.count { it.isRecoverable }
                val activeCount = discoveredList.size - recoverableCount

                val summary = ScanResult(
                    imagesFound = imagesFound,
                    videosFound = videosFound,
                    recoverableCount = recoverableCount,
                    trashedCount = trashedCount,
                    hiddenCount = hiddenCount,
                    activeCount = activeCount,
                    totalScanned = discoveredList.size,
                    durationMs = duration,
                    items = discoveredList.toList()
                )

                _lastScanSummary.value = summary
                _scanState.value = ScanUiState.Finished(summary)
            } catch (e: Exception) {
                _scanState.value = ScanUiState.Error(e.message ?: "Scan failed")
            }
        }
    }

    fun cancelScan() {
        scanJob?.cancel()
        scanJob = null
        _scanState.value = ScanUiState.Idle
    }

    fun toggleSelection(uri: Uri) {
        val current = _selectedUris.value.toMutableSet()
        if (current.contains(uri)) {
            current.remove(uri)
        } else {
            current.add(uri)
        }
        _selectedUris.value = current
    }

    fun selectAll(items: List<MediaItem>) {
        val set = items.map { it.uri }.toSet()
        _selectedUris.value = set
    }

    fun clearSelection() {
        _selectedUris.value = emptySet()
    }

    fun setFilter(filter: FilterType) {
        _filterType.value = filter
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    fun recoverSelected(onCompleted: ((RecoveryResult) -> Unit)? = null) {
        val selected = _selectedUris.value
        if (selected.isEmpty() || _isRecovering.value) return

        val itemsToRecover = _allMediaItems.value.filter { selected.contains(it.uri) }
        performRecovery(itemsToRecover, onCompleted)
    }

    fun recoverSingle(item: MediaItem, onCompleted: ((RecoveryResult) -> Unit)? = null) {
        if (_isRecovering.value) return
        performRecovery(listOf(item), onCompleted)
    }

    private fun performRecovery(
        items: List<MediaItem>,
        onCompleted: ((RecoveryResult) -> Unit)? = null
    ) {
        viewModelScope.launch {
            _isRecovering.value = true
            _recoveryProgress.value = Pair(0, items.size)

            val result = recoveryEngine.recoverItems(items) { current, total, _ ->
                _recoveryProgress.value = Pair(current, total)
            }

            // Update item list with recovered statuses
            val updatedMap = result.recoveredItems.associateBy { it.uri }
            val currentList = _allMediaItems.value.map { item ->
                updatedMap[item.uri] ?: item
            }
            _allMediaItems.value = currentList

            // Clear selected items that were processed
            _selectedUris.value = emptySet()
            _recoveryResult.value = result
            _isRecovering.value = false
            _recoveryProgress.value = null

            onCompleted?.invoke(result)
        }
    }

    fun getFilteredAndSortedItems(mediaType: MediaType?, onlyRecovered: Boolean = false): List<MediaItem> {
        var list = _allMediaItems.value

        if (onlyRecovered) {
            list = list.filter { it.recoveryStatus == RecoveryStatus.RECOVERED }
        } else {
            if (mediaType != null) {
                list = list.filter { it.mediaType == mediaType }
            }

            list = when (_filterType.value) {
                FilterType.ALL -> list
                FilterType.RECOVERABLE_ONLY -> list.filter { it.isRecoverable }
                FilterType.TRASHED -> list.filter { it.isTrashed }
                FilterType.HIDDEN -> list.filter { it.isHidden }
                FilterType.ACTIVE -> list.filter { !it.isRecoverable }
            }
        }

        return when (_sortOrder.value) {
            SortOrder.DATE_DESC -> list.sortedByDescending { it.dateModified }
            SortOrder.DATE_ASC -> list.sortedBy { it.dateModified }
            SortOrder.SIZE_DESC -> list.sortedByDescending { it.size }
            SortOrder.SIZE_ASC -> list.sortedBy { it.size }
            SortOrder.NAME_ASC -> list.sortedBy { it.displayName.lowercase() }
            SortOrder.NAME_DESC -> list.sortedByDescending { it.displayName.lowercase() }
        }
    }
}
