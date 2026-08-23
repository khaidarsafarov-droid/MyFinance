package com.truckerload.presentation.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.truckerload.R
import com.truckerload.domain.account.DriverRole
import com.truckerload.presentation.components.TlButton as Button
import com.truckerload.presentation.di.LocalUserProfileStore
import com.truckerload.presentation.theme.AppTextFieldDefaults
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.LocalTruckColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationFlowScreen(
    onCompleted: () -> Unit,
    viewModel: RegistrationViewModel = hiltViewModel(),
) {
    val tc = LocalTruckColors.current
    val context = LocalContext.current
    val userProfile by LocalUserProfileStore.current.profile.collectAsStateWithLifecycle()
    var displayName by remember {
        mutableStateOf(userProfile?.displayName?.takeIf { it != userProfile?.email }.orEmpty())
    }
    var role by remember { mutableStateOf(DriverRole.OWNER_OPERATOR) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = BentoGlassTheme.ScreenBackground,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile_setup_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.reg_step_basic),
                style = MaterialTheme.typography.titleMedium,
                color = tc.TextPrimary,
            )
            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it; error = null },
                label = { Text(stringResource(R.string.social_display_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = AppTextFieldDefaults.outlined(),
            )
            Text(stringResource(R.string.reg_role_label), color = tc.TextSecondary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DriverRole.entries.forEach { option ->
                    FilterChip(
                        selected = role == option,
                        onClick = { role = option },
                        label = {
                            Text(
                                stringResource(
                                    when (option) {
                                        DriverRole.OWNER_OPERATOR -> R.string.reg_role_owner
                                        DriverRole.HIRED_DRIVER -> R.string.reg_role_hired
                                        DriverRole.DISPATCHER -> R.string.reg_role_dispatcher
                                    },
                                ),
                            )
                        },
                    )
                }
            }
            error?.let {
                Text(text = it, color = tc.AccentExpense, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    if (busy) return@Button
                    if (displayName.isBlank()) {
                        error = context.getString(R.string.profile_setup_name_required)
                        return@Button
                    }
                    busy = true
                    viewModel.completeBasic(
                        displayName = displayName,
                        role = role,
                        phone = userProfile?.phoneNumber,
                    ) { result ->
                        busy = false
                        result.fold(
                            onSuccess = { onCompleted() },
                            onFailure = {
                                error = context.getString(R.string.profile_setup_name_required)
                            },
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !busy,
            ) {
                Text(stringResource(R.string.profile_setup_next))
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
