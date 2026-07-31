package com.truckerload.presentation.screens.social

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.truckerload.data.repository.SocialRepository
import com.truckerload.domain.social.SocialResult
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
