package com.truckerload.presentation.screens.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.presentation.theme.LocalTruckColors

data class RegistrationConsentState(
    val ageConfirmed: Boolean = false,
    val tosAccepted: Boolean = false,
    val analyticsAccepted: Boolean = false,
)

@Composable
fun RegistrationConsentSection(
    state: RegistrationConsentState,
    onChange: (RegistrationConsentState) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        ConsentRow(
            checked = state.ageConfirmed,
            label = stringResource(R.string.signup_age_confirm),
            onCheckedChange = { onChange(state.copy(ageConfirmed = it)) },
        )
        ConsentRow(
            checked = state.tosAccepted,
            label = stringResource(R.string.signup_tos_confirm),
            onCheckedChange = { onChange(state.copy(tosAccepted = it)) },
        )
        ConsentRow(
            checked = state.analyticsAccepted,
            label = stringResource(R.string.signup_analytics_confirm),
            onCheckedChange = { onChange(state.copy(analyticsAccepted = it)) },
        )
    }
}

@Composable
private fun ConsentRow(
    checked: Boolean,
    label: String,
    onCheckedChange: (Boolean) -> Unit,
) {
    val tc = LocalTruckColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = tc.TextPrimary,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}
