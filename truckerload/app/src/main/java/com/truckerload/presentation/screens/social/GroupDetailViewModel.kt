package com.truckerload.presentation.screens.social

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.truckerload.data.repository.social.ChatRepository
import com.truckerload.data.repository.social.GroupRepository
import com.truckerload.domain.social.SocialChat
import com.truckerload.domain.social.SocialResult

data class GroupDetailUiState(
    val chat: com.truckerload.domain.social.SocialChat? = null,
    val members: List<com.truckerload.domain.social.ChatMember> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isCreator: Boolean = false,
    val isManager: Boolean = false,
)

@HiltViewModel
class GroupDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val chatRepository: ChatRepository,
    private val groupRepository: GroupRepository,
) : ViewModel() {
    private val chatId = Uri.decode(savedStateHandle.get<String>("chatId").orEmpty())
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<GroupDetailUiState> =
        combine(
            chatRepository.watchChats().map { chats -> chats.firstOrNull { it.id == chatId } },
            groupRepository.watchGroupMembers(chatId),
            _errorMessage,
        ) { chat, members, errorMessage ->
            val me = members.find { it.isMe }
            val isCreator = (chat?.creatorId?.isNotBlank() == true && chat.creatorId == me?.userId) ||
                me?.role == "OWNER"
            val isManager = isCreator || me?.role == "MODERATOR"
            GroupDetailUiState(
                chat = chat,
                members = members,
                isLoading = false,
                errorMessage = errorMessage,
                isCreator = isCreator,
                isManager = isManager,
            )
        }.catch { error ->
            emit(GroupDetailUiState(isLoading = false, errorMessage = error.toUiMessage()))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GroupDetailUiState())

    fun leaveGroup(onLeft: () -> Unit) {
        viewModelScope.launch {
            _errorMessage.value = null
            when (val result = groupRepository.leaveGroup(chatId)) {
                is SocialResult.Success -> onLeft()
                is SocialResult.Error -> _errorMessage.value = result.message
            }
        }
    }

    fun updateDescription(description: String) {
        viewModelScope.launch {
            _errorMessage.value = null
            when (val result = groupRepository.updateGroupDescription(chatId, description)) {
                is SocialResult.Success -> Unit
                is SocialResult.Error -> _errorMessage.value = result.message
            }
        }
    }

    fun deleteGroup(onDeleted: () -> Unit) {
        viewModelScope.launch {
            _errorMessage.value = null
            when (val result = groupRepository.deleteGroup(chatId)) {
                is SocialResult.Success -> onDeleted()
                is SocialResult.Error -> _errorMessage.value = result.message
            }
        }
    }

    fun setModerator(userId: String) {
        viewModelScope.launch {
            _errorMessage.value = null
            when (val result = groupRepository.setModerator(chatId, userId)) {
                is SocialResult.Success -> Unit
                is SocialResult.Error -> _errorMessage.value = result.message
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
