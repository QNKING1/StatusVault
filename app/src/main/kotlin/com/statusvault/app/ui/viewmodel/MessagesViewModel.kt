package com.statusvault.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.statusvault.app.data.entity.RecoveredMessage
import com.statusvault.app.data.repository.MessageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MessagesViewModel @Inject constructor(
    private val messageRepository: MessageRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _showDeletedOnly = MutableStateFlow(false)
    val showDeletedOnly: StateFlow<Boolean> = _showDeletedOnly

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Messages flow that reacts to search query and deleted filter changes
    val messages: StateFlow<List<RecoveredMessage>> = _searchQuery
        .flatMapLatest { query ->
            _showDeletedOnly.flatMapLatest { deletedOnly ->
                resolveMessagesFlow(query, deletedOnly)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val deletedCount: StateFlow<Int> = messageRepository.getDeletedCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalCount: StateFlow<Int> = messageRepository.getTotalCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private fun resolveMessagesFlow(
        query: String,
        deletedOnly: Boolean
    ): Flow<List<RecoveredMessage>> {
        return if (query.isBlank()) {
            if (deletedOnly) messageRepository.getDeletedMessages()
            else messageRepository.getAllMessages()
        } else {
            messageRepository.searchMessages(query, deletedOnly)
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleDeletedOnly() {
        _showDeletedOnly.value = !_showDeletedOnly.value
    }

    fun setShowDeletedOnly(show: Boolean) {
        _showDeletedOnly.value = show
    }

    fun deleteMessage(messageId: Long) {
        viewModelScope.launch {
            messageRepository.deleteMessage(messageId)
        }
    }

    fun clearAllMessages() {
        viewModelScope.launch {
            messageRepository.clearAllMessages()
        }
    }
}
