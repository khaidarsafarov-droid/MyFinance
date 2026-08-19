package com.truckerload.presentation.screens.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.truckerload.data.repository.social.ChatRepository
import com.truckerload.data.repository.social.GroupRepository
import com.truckerload.data.repository.social.SocialSyncCoordinator
import com.truckerload.domain.social.GroupInviteCode
import com.truckerload.domain.social.SocialResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class GroupsUiState(
    val publicGroups: List<com.truckerload.domain.social.SocialChat> = emptyList(),
    val recommendedGroups: List<com.truckerload.domain.social.SocialChat> = emptyList(),
    val inviteCode: String = "",
    val errorMessage: String? = null,
)

@HiltViewModel
class GroupsViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val groupRepository: GroupRepository,
    private val socialSyncCoordinator: SocialSyncCoordinator,
) : ViewModel() {
    private val _inviteCode = MutableStateFlow("")
    private val _errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<GroupsUiState> =
        combine(
            chatRepository.watchPublicGroups(),
            chatRepository.recommendGroups(),
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
        viewModelScope.launch {
            socialSyncCoordinator.ensureInitialized()
            while (isActive) {
                delay(4_000)
                runCatching { socialSyncCoordinator.pullRemote() }
            }
        }
    }

    fun setInviteCode(code: String) {
        _inviteCode.value = code
    }

    fun joinGroup(chatId: String, displayName: String, onJoined: (String) -> Unit) {
        viewModelScope.launch {
            _errorMessage.value = null
            when (val result = groupRepository.joinGroup(chatId, displayName)) {
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
            when (val result = groupRepository.joinGroupByInviteCode(GroupInviteCode.normalize(code), displayName)) {
                is SocialResult.Success -> onJoined(result.data)
                is SocialResult.Error -> _errorMessage.value = result.message
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
