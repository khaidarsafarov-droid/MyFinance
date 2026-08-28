package com.truckerload.presentation.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.truckerload.R
import com.truckerload.presentation.components.AppBrandLogo
import com.truckerload.presentation.components.FillViewportScrollColumn
import com.truckerload.presentation.components.TlButton as Button
import com.truckerload.presentation.theme.AppTextFieldDefaults
import com.truckerload.presentation.theme.BentoGlassScreenBackground
import com.truckerload.presentation.theme.LocalTruckColors

@Composable
fun FirstRunNameScreen(
    viewModel: FirstRunViewModel = hiltViewModel(),
) {
    val tc = LocalTruckColors.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val canSave = uiState.givenName.isNotBlank() &&
        uiState.familyName.isNotBlank() &&
        !uiState.isSaving

    BentoGlassScreenBackground {
        FillViewportScrollColumn(
            contentPadding = PaddingValues(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AppBrandLogo(size = 112.dp)
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = stringResource(R.string.first_run_title),
                    style = MaterialTheme.typography.headlineLarge,
                    color = tc.TextPrimary,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.first_run_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = tc.TextSecondary,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(32.dp))
                OutlinedTextField(
                    value = uiState.givenName,
                    onValueChange = viewModel::onGivenNameChanged,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isSaving,
                    singleLine = true,
                    label = { Text(stringResource(R.string.analytics_share_given_name)) },
                    colors = AppTextFieldDefaults.outlined(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next,
                    ),
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = uiState.familyName,
                    onValueChange = viewModel::onFamilyNameChanged,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isSaving,
                    singleLine = true,
                    label = { Text(stringResource(R.string.analytics_share_family_name)) },
                    colors = AppTextFieldDefaults.outlined(),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            viewModel.onSave()
                        },
                    ),
                )
                uiState.errorMessageRes?.let { resId ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(resId),
                        style = MaterialTheme.typography.bodySmall,
                        color = tc.AccentExpense,
                        textAlign = TextAlign.Center,
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.onSave()
                    },
                    enabled = canSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    Text(stringResource(R.string.common_save))
                }
            }
        }
    }
}
