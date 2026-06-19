package com.statusvault.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.statusvault.app.R
import com.statusvault.app.data.entity.StatusFile
import com.statusvault.app.data.repository.StatusRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StatusViewModel @Inject constructor(
    application: Application,
    private val statusRepository: StatusRepository
) : AndroidViewModel(application) {

    private val _selectedTab = MutableStateFlow(0) // 0 = Images, 1 = Videos
    val selectedTab: StateFlow<Int> = _selectedTab

    private val _selectionMode = MutableStateFlow(false)
    val selectionMode: StateFlow<Boolean> = _selectionMode

    private val _selectedItems = MutableStateFlow<Set<String>>(emptySet())
    val selectedItems: StateFlow<Set<String>> = _selectedItems

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    private val _events = MutableSharedFlow<StatusEvent>()
    val events = _events.asSharedFlow()

    val imageStatuses = statusRepository.getImageStatuses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val videoStatuses = statusRepository.getVideoStatuses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allStatuses = combine(imageStatuses, videoStatuses) { images, videos ->
        images + videos
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectTab(index: Int) {
        _selectedTab.value = index
    }

    fun refreshStatuses() {
        viewModelScope.launch {
            _isRefreshing.value = true
            statusRepository.refreshStatuses()
                .onSuccess {
                    _events.emit(StatusEvent.ShowMessage(R.string.saved_successfully))
                }
                .onFailure { error ->
                    _events.emit(StatusEvent.ShowError(error.message ?: "Refresh failed"))
                }
            _isRefreshing.value = false
        }
    }

    fun toggleSelection(statusId: String) {
        _selectedItems.update { current ->
            if (statusId in current) current - statusId else current + statusId
        }
        if (_selectedItems.value.isEmpty()) {
            _selectionMode.value = false
        }
    }

    fun enterSelectionMode() {
        _selectionMode.value = true
    }

    fun exitSelectionMode() {
        _selectionMode.value = false
        _selectedItems.value = emptySet()
    }

    fun selectAll(visibleStatuses: List<StatusFile>) {
        _selectedItems.value = visibleStatuses.map { it.id }.toSet()
    }

    fun deselectAll() {
        _selectedItems.value = emptySet()
    }

    fun saveSelected() {
        viewModelScope.launch {
            val ids = _selectedItems.value.toList()
            val all = allStatuses.value
            var savedCount = 0

            ids.forEach { id ->
                val status = all.find { it.id == id } ?: return@forEach
                statusRepository.saveStatus(status)
                    .onSuccess { savedCount++ }
            }

            if (savedCount > 0) {
                _events.emit(StatusEvent.SavedCount(savedCount))
            }
            exitSelectionMode()
        }
    }

    fun saveSingle(status: StatusFile) {
        viewModelScope.launch {
            statusRepository.saveStatus(status)
                .onSuccess {
                    _events.emit(StatusEvent.SavedCount(1))
                }
                .onFailure { error ->
                    _events.emit(StatusEvent.ShowError(error.message ?: "Save failed"))
                }
        }
    }

    fun deleteSelected() {
        viewModelScope.launch {
            val ids = _selectedItems.value.toList()
            val all = allStatuses.value
            var deletedCount = 0

            ids.forEach { id ->
                val status = all.find { it.id == id } ?: return@forEach
                statusRepository.deleteSavedStatus(status)
                    .onSuccess { deletedCount++ }
            }

            if (deletedCount > 0) {
                _events.emit(StatusEvent.DeletedCount(deletedCount))
            }
            exitSelectionMode()
        }
    }

    sealed class StatusEvent {
        data class ShowMessage(val messageResId: Int) : StatusEvent()
        data class ShowError(val message: String) : StatusEvent()
        data class SavedCount(val count: Int) : StatusEvent()
        data class DeletedCount(val count: Int) : StatusEvent()
    }
}
