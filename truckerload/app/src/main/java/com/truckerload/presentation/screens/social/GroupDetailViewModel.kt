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
import com.truckerload.data.preferences.CallPrivacyStore
import com.truckerload.data.repository.VoiceRepository
import com.truckerload.data.repository.social.ChatRepository
import com.truckerload.data.repository.social.GroupRepository
import com.truckerload.data.repository.social.ProfileRepository
import com.truckerload.data.voice.CallHistory
import com.truckerload.domain.social.SocialResult
import com.truckerload.domain.voice.CallPolicy

data class GroupDetailUiState(
    val chat: com.truckerload.domain.social.SocialChat? = null,
    val members: List<com.truckerload.domain.social.ChatMember> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val isCreator: Boolean = false,
    val isManager: Boolean = false,
    val isMember: Boolean = false,
    val callsEnabled: Boolean = true,
    val adminsOnly: Boolean = false,
)

@HiltViewModel
class GroupDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val chatRepository: ChatRepository,
    private val groupRepository: GroupRepository,
    private val voiceRepository: VoiceRepository,
    private val profileRepository: ProfileRepository,
    private val callPrivacyStore: CallPrivacyStore,
) : ViewModel() {
    private val chatId = Uri.decode(savedStateHandle.get<String>("chatId").orEmpty())
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _privacyTick = MutableStateFlow(0)

    val uiState: StateFlow<GroupDetailUiState> =
        combine(
            chatRepository.watchChats().map { chats -> chats.firstOrNull { it.id == chatId } },
            groupRepository.watchGroupMembers(chatId),
            _errorMessage,
            _privacyTick,
        ) { chat, members, errorMessage, _ ->
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
                isMember = me != null,
                callsEnabled = callPrivacyStore.groupCallsEnabled(chatId),
                adminsOnly = callPrivacyStore.groupAdminsOnly(chatId),
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

    fun setCallsEnabled(enabled: Boolean) {
        callPrivacyStore.setGroupCallsEnabled(chatId, enabled)
        _privacyTick.value += 1
    }

    fun setAdminsOnly(adminsOnly: Boolean) {
        callPrivacyStore.setGroupAdminsOnly(chatId, adminsOnly)
        _privacyTick.value += 1
    }

    fun startGroupCall(onStarted: (String) -> Unit) {
        val state = uiState.value
        if (!CallPolicy.canStartGroupCall(state.callsEnabled, state.adminsOnly, state.isManager, state.isMember)) {
            _errorMessage.value = "calls_disabled"
            return
        }
        viewModelScope.launch {
            val title = state.chat?.title.orEmpty().ifBlank { "Group call" }
            voiceRepository.ensureGroupRoom(chatId, title)
                .onSuccess { roomId ->
                    runCatching { CallHistory.recordGroupStart(chatRepository, profileRepository, chatId) }
                    onStarted(roomId)
                }
                .onFailure { error ->
                    _errorMessage.value = error.message
                }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
