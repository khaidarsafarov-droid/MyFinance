package com.truckerload.presentation.screens.social

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.viewModelScope
import com.truckerload.data.repository.SocialRepository
import com.truckerload.domain.social.SocialChat
import com.truckerload.domain.social.SocialResult
import com.truckerload.domain.social.GroupInviteCode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GroupsUiState(
    val publicGroups: List<com.truckerload.domain.social.SocialChat> = emptyList(),
    val recommendedGroups: List<com.truckerload.domain.social.SocialChat> = emptyList(),
    val inviteCode: String = "",
    val errorMessage: String? = null,
)

@HiltViewModel
class GroupsViewModel @Inject constructor(
    private val socialRepository: SocialRepository,
) : ViewModel() {
    private val _inviteCode = MutableStateFlow("")
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<GroupsUiState> =
        combine(
            socialRepository.watchPublicGroups(),
            socialRepository.recommendGroups(),
            _inviteCode,
            _errorMessage,
        ) { groups, recommended, code, error ->
            GroupsUiState(
                publicGroups = groups,
                recommendedGroups = recommended.filter { rec ->
                    groups.none { it.id == rec.id } || !rec.isMember
                },
                inviteCode = code,
                errorMessage = error,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GroupsUiState())

    init {
        viewModelScope.launch { socialRepository.ensureInitialized() }
    }

    fun setInviteCode(code: String) {
        _inviteCode.value = code
    }

    fun joinGroup(chatId: String, displayName: String, onJoined: (String) -> Unit) {
        viewModelScope.launch {
            _errorMessage.value = null
            when (val result = socialRepository.joinGroup(chatId, displayName)) {
                is SocialResult.Success -> onJoined(chatId)
                is SocialResult.Error -> _errorMessage.value = result.message
            }
        }
    }

    fun joinByCode(displayName: String, onJoined: (String) -> Unit) {
        val code = _inviteCode.value
        // Blank / whitespace: no-op (button should stay disabled for blank).
        if (GroupInviteCode.isBlank(code)) return
        viewModelScope.launch {
            _errorMessage.value = null
            when (val result = socialRepository.joinGroupByInviteCode(GroupInviteCode.normalize(code), displayName)) {
                is SocialResult.Success -> onJoined(result.data)
                is SocialResult.Error -> _errorMessage.value = result.message
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}

data class GroupDetailUiState(
    val chat: com.truckerload.domain.social.SocialChat? = null,
    val members: List<com.truckerload.domain.social.ChatMember> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

@HiltViewModel
class GroupDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val socialRepository: SocialRepository,
) : ViewModel() {
    private val chatId = Uri.decode(savedStateHandle.get<String>("chatId").orEmpty())
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<GroupDetailUiState> =
        combine(
            socialRepository.watchChats().map { chats -> chats.firstOrNull { it.id == chatId } },
            socialRepository.watchGroupMembers(chatId),
            _errorMessage,
        ) { chat, members, errorMessage ->
            GroupDetailUiState(
                chat = chat,
                members = members,
                isLoading = false,
                errorMessage = errorMessage,
            )
        }.catch { error ->
            emit(GroupDetailUiState(isLoading = false, errorMessage = error.toUiMessage()))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GroupDetailUiState())

    fun leaveGroup(onLeft: () -> Unit) {
        viewModelScope.launch {
            _errorMessage.value = null
            when (val result = socialRepository.leaveGroup(chatId)) {
                is SocialResult.Success -> onLeft()
                is SocialResult.Error -> _errorMessage.value = result.message
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}

