package com.statusvault.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.statusvault.app.data.prefs.SettingsDataStore
import com.statusvault.app.data.repository.MessageRepository
import com.statusvault.app.data.repository.StatusRepository
import com.statusvault.app.worker.StatusScanWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    private val settingsDataStore: SettingsDataStore,
    private val statusRepository: StatusRepository,
    private val messageRepository: MessageRepository
) : AndroidViewModel(application) {

    val isDarkMode = settingsDataStore.isDarkMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isSystemTheme = settingsDataStore.isSystemTheme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isAutoSave = settingsDataStore.isAutoSave
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setDarkMode(enabled)
        }
    }

    fun setSystemTheme(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setSystemTheme(enabled)
        }
    }

    fun setAutoSave(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setAutoSave(enabled)
            if (enabled) {
                StatusScanWorker.enqueuePeriodicWork(getApplication())
            } else {
                StatusScanWorker.cancelWork(getApplication())
            }
        }
    }

    fun cleanupSavedStatuses(onComplete: (Int) -> Unit) {
        viewModelScope.launch {
            statusRepository.cleanupSavedStatuses()
                .onSuccess { count ->
                    onComplete(count)
                }
                .onFailure {
                    onComplete(0)
                }
        }
    }

    fun clearAllMessages() {
        viewModelScope.launch {
            messageRepository.clearAllMessages()
        }
    }
}
