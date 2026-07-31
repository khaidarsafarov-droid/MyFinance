package com.truckerload.presentation.screens.social

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.truckerload.R
import com.truckerload.data.remote.SupabaseFriendsRealtimeService
import com.truckerload.domain.friends.NicknameValidator
import com.truckerload.presentation.components.TlButton
import com.truckerload.presentation.components.TlOutlinedButton
import com.truckerload.presentation.di.LocalAuthStore
import com.truckerload.presentation.di.LocalUserProfileStore
import com.truckerload.presentation.theme.AppTextFieldDefaults
import com.truckerload.presentation.theme.AppTypography
import com.truckerload.presentation.theme.BentoGlassCard
import com.truckerload.presentation.theme.LocalTruckColors
import kotlinx.coroutines.launch

@Composable
internal fun ProfileNicknameSection() {
    val tc = LocalTruckColors.current
    val userProfileStore = LocalUserProfileStore.current
    val authStore = LocalAuthStore.current
    val authProfile by userProfileStore.profile.collectAsStateWithLifecycle()
    val friendsApi = remember(authStore) { SupabaseFriendsRealtimeService(authStore) }
    val currentNick = authProfile?.nickname.orEmpty()
    var draft by remember { mutableStateOf(currentNick) }
    var editing by remember { mutableStateOf(currentNick.isBlank()) }
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Keep collapsed/expanded in sync when profile loads from prefs / Supabase.
    LaunchedEffect(currentNick) {
        if (currentNick.isNotBlank() && !editing) {
            draft = currentNick
        }
        if (currentNick.isNotBlank() && message == "saved") {
            editing = false
        }
        if (currentNick.isBlank()) {
            editing = true
        }
    }

    BentoGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.friends_my_nickname_title),
                style = AppTypography.CardTitle,
                color = tc.TextPrimary,
            )
            Text(
                text = stringResource(R.string.friends_my_nickname_hint),
                style = AppTypography.Subtitle,
                color = tc.TextSecondary,
            )
            if (!editing && currentNick.isNotBlank()) {
                Text(
                    text = stringResource(R.string.friends_my_nickname_current, currentNick),
                    style = MaterialTheme.typography.bodyLarge,
                    color = tc.AccentPrimary,
                )
                TlOutlinedButton(
                    onClick = {
                        draft = currentNick
                        editing = true
                        message = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.friends_change_nickname_button))
                }
            } else {
                OutlinedTextField(
                    value = draft,
                    onValueChange = {
                        draft = it
                        message = null
                    },
                    label = { Text(stringResource(R.string.friends_nickname_label)) },
                    placeholder = { Text(stringResource(R.string.friends_nickname_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = AppTextFieldDefaults.outlined(),
                )
                TlButton(
                    onClick = {
                        val handle = NicknameValidator.sanitizeOrNull(draft)
                        if (handle == null) {
                            message = "invalid"
                            return@TlButton
                        }
                        busy = true
                        scope.launch {
                            // Supabase is source of truth when cloud is configured.
                            val result = if (friendsApi.isConfigured()) {
                                friendsApi.upsertMyNickname(handle, authProfile?.displayName)
                            } else {
                                Result.success(Unit)
                            }
                            busy = false
                            val err = result.exceptionOrNull()?.message
                            if (result.isFailure &&
                                err != SupabaseFriendsRealtimeService.ERROR_NICKNAME_SCHEMA_MISSING
                            ) {
                                message = err ?: "error"
                                return@launch
                            }
                            val current = userProfileStore.profile.value
                            if (current != null) {
                                userProfileStore.saveProfile(current.copy(nickname = handle))
                            }
                            if (err == SupabaseFriendsRealtimeService.ERROR_NICKNAME_SCHEMA_MISSING) {
                                message = SupabaseFriendsRealtimeService.ERROR_NICKNAME_SCHEMA_MISSING
                                // Keep form open until schema is applied.
                                editing = true
                            } else {
                                message = "saved"
                                editing = false
                                draft = handle
                            }
                        }
                    },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (currentNick.isBlank()) {
                            stringResource(R.string.friends_add_nickname_button)
                        } else {
                            stringResource(R.string.friends_nickname_save)
                        },
                    )
                }
                if (currentNick.isNotBlank()) {
                    TlOutlinedButton(
                        onClick = {
                            draft = currentNick
                            editing = false
                            message = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.common_cancel))
                    }
                }
            }
            val feedback = message
            when (feedback) {
                "invalid" -> Text(
                    stringResource(R.string.friends_nickname_invalid),
                    color = MaterialTheme.colorScheme.error,
                    style = AppTypography.Subtitle,
                )
                "saved" -> Text(
                    stringResource(R.string.friends_nickname_saved),
                    color = tc.AccentPrimary,
                    style = AppTypography.Subtitle,
                )
                SupabaseFriendsRealtimeService.ERROR_NICKNAME_SCHEMA_MISSING -> Text(
                    stringResource(R.string.friends_nickname_schema_missing),
                    color = MaterialTheme.colorScheme.error,
                    style = AppTypography.Subtitle,
                )
                null -> Unit
                else -> Text(feedback, color = MaterialTheme.colorScheme.error, style = AppTypography.Subtitle)
            }
        }
    }
}
