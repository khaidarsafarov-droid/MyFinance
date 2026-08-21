package com.truckerload.presentation.voice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.truckerload.voice.AppVoiceAction
import com.truckerload.voice.VoiceAssistantLogger
import com.truckerload.voice.VoiceCommandBus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class VoiceCommandViewModel @Inject constructor() : ViewModel() {

    private val _navigateTo = MutableStateFlow<String?>(null)
    val navigateTo: StateFlow<String?> = _navigateTo.asStateFlow()

    init {
        viewModelScope.launch {
            VoiceCommandBus.pending.collect { command ->
                if (command != null) handle(command)
            }
        }
    }

    fun onNavigated() {
        _navigateTo.value = null
    }

    private fun handle(command: AppVoiceAction) {
        VoiceCommandBus.consume()
        when (command) {
            is AppVoiceAction.OpenScreen -> {
                VoiceAssistantLogger.log(command, "navigate")
                _navigateTo.value = command.route
            }
        }
    }
}
