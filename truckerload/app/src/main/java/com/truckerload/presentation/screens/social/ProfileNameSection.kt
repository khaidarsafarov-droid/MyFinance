package com.truckerload.presentation.screens.social

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.presentation.components.TlButton as Button
import com.truckerload.presentation.theme.AppTextFieldDefaults
import com.truckerload.presentation.theme.AppTypography
import com.truckerload.presentation.theme.BentoGlassCard
import com.truckerload.presentation.theme.LocalTruckColors

@Composable
internal fun ProfileNameSection(
    givenName: String,
    familyName: String,
    message: String?,
    messageIsError: Boolean,
    onSave: (String, String) -> Unit,
) {
    val tc = LocalTruckColors.current
    var given by remember(givenName) { mutableStateOf(givenName) }
    var family by remember(familyName) { mutableStateOf(familyName) }
    // Baseline of last persisted values — updated from props and on successful save so
    // the Save button hides immediately even if the store emits a beat later.
    var committedGiven by remember { mutableStateOf(givenName) }
    var committedFamily by remember { mutableStateOf(familyName) }
    LaunchedEffect(givenName, familyName) {
        committedGiven = givenName
        committedFamily = familyName
    }
    LaunchedEffect(message, messageIsError) {
        if (message != null && !messageIsError) {
            committedGiven = given.trim()
            committedFamily = family.trim()
        }
    }
    val isDirty =
        given.trim() != committedGiven.trim() || family.trim() != committedFamily.trim()
    // Success text only while fields match saved values; errors stay visible while editing.
    val visibleMessage = when {
        message == null -> null
        messageIsError -> message
        isDirty -> null
        else -> message
    }

    BentoGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.profile_name_section_title),
                style = AppTypography.CardTitle,
                color = tc.TextPrimary,
            )
            Text(
                text = stringResource(R.string.profile_name_section_hint),
                style = AppTypography.Subtitle,
                color = tc.TextSecondary,
                modifier = Modifier.padding(top = 6.dp, bottom = 12.dp),
            )
            OutlinedTextField(
                value = given,
                onValueChange = { given = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.analytics_share_given_name)) },
                colors = AppTextFieldDefaults.outlined(),
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = family,
                onValueChange = { family = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.analytics_share_family_name)) },
                colors = AppTextFieldDefaults.outlined(),
            )
            visibleMessage?.let { text ->
                Text(
                    text = text,
                    style = AppTypography.Caption,
                    color = if (messageIsError) tc.AccentExpense else tc.Success,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            if (isDirty) {
                Button(
                    onClick = { onSave(given, family) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                ) {
                    Text(stringResource(R.string.common_save))
                }
            }
        }
    }
}
