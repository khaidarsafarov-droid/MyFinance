package com.truckerload.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.truckerload.R
import com.truckerload.data.preferences.AuthSessionHealth
import com.truckerload.data.preferences.SecurePreferences
import com.truckerload.presentation.di.LocalAuthStore
import com.truckerload.presentation.theme.LocalTruckColors

/**
 * Soft status strip for degraded auth / secure-storage (guide Part 2–3).
 * Never blocks local work.
 */
@Composable
fun AuthStatusBanner(modifier: Modifier = Modifier) {
    val authStore = LocalAuthStore.current
    val health by authStore.sessionHealth.collectAsStateWithLifecycle()
    val tc = LocalTruckColors.current
    val messageRes = when {
        SecurePreferences.plaintextFallbackUsed -> R.string.auth_secure_storage_fallback_banner
        health == AuthSessionHealth.SESSION_UNCONFIRMED -> R.string.auth_session_unconfirmed_banner
        health == AuthSessionHealth.OFFLINE_LOCAL -> R.string.auth_session_offline_local_banner
        else -> null
    } ?: return

    Text(
        text = stringResource(messageRes),
        style = MaterialTheme.typography.labelMedium,
        color = tc.TextPrimary,
        textAlign = TextAlign.Center,
        modifier = modifier
            .fillMaxWidth()
            .background(tc.AccentWarning.copy(alpha = 0.18f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}
