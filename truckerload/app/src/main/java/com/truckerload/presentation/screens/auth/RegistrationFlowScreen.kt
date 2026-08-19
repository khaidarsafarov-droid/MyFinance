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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.truckerload.R
import com.truckerload.domain.account.CommunityVisibilitySettings
import com.truckerload.domain.account.DriverRole
import com.truckerload.presentation.components.TlButton as Button
import com.truckerload.presentation.di.LocalUserProfileStore
import com.truckerload.presentation.theme.AppTextFieldDefaults
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.LocalTruckColors

private enum class WizardPane { BASIC, PROFESSIONAL, COMMUNITY }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationFlowScreen(
    onCompleted: () -> Unit,
    viewModel: RegistrationViewModel = hiltViewModel(),
    startAtOptional: Boolean = false,
) {
    val tc = LocalTruckColors.current
    val context = LocalContext.current
    val userProfile by LocalUserProfileStore.current.profile.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    var pane by remember {
        mutableStateOf(
            when {
                startAtOptional && progress.professionalPending -> WizardPane.PROFESSIONAL
                startAtOptional && progress.communityPending -> WizardPane.COMMUNITY
                else -> WizardPane.BASIC
            },
        )
    }
    var displayName by remember {
        mutableStateOf(userProfile?.displayName?.takeIf { it != userProfile?.email }.orEmpty())
    }
    var role by remember { mutableStateOf(DriverRole.OWNER_OPERATOR) }
    var companyName by remember { mutableStateOf("") }
    var cdlNumber by remember { mutableStateOf("") }
    var vehicleType by remember { mutableStateOf("") }
    var primaryRegion by remember { mutableStateOf("") }
    var nickname by remember { mutableStateOf(userProfile?.nickname.orEmpty()) }
    var bio by remember { mutableStateOf("") }
    var bioVisible by remember { mutableStateOf(false) }
    var avatarVisible by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    fun finishIfOptionalDone() {
        if (!viewModel.needsRequiredOnboarding()) onCompleted()
    }

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
            val stepIndex = when (pane) {
                WizardPane.BASIC -> 0
                WizardPane.PROFESSIONAL -> 1
                WizardPane.COMMUNITY -> 2
            }
            LinearProgressIndicator(
                progress = { (stepIndex + 1) / 3f },
                modifier = Modifier.fillMaxWidth().height(6.dp),
            )
            Text(
                text = stringResource(
                    when (pane) {
                        WizardPane.BASIC -> R.string.reg_step_basic
                        WizardPane.PROFESSIONAL -> R.string.reg_step_professional
                        WizardPane.COMMUNITY -> R.string.reg_step_community
                    },
                ),
                style = MaterialTheme.typography.titleMedium,
                color = tc.TextPrimary,
            )
            when (pane) {
                WizardPane.BASIC -> {
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
                }
                WizardPane.PROFESSIONAL -> {
                    OutlinedTextField(
                        value = companyName,
                        onValueChange = { companyName = it },
                        label = { Text(stringResource(R.string.reg_company_name)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = AppTextFieldDefaults.outlined(),
                    )
                    OutlinedTextField(
                        value = cdlNumber,
                        onValueChange = { cdlNumber = it },
                        label = { Text(stringResource(R.string.profile_setup_cdl_number)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = AppTextFieldDefaults.outlined(),
                    )
                    OutlinedTextField(
                        value = vehicleType,
                        onValueChange = { vehicleType = it },
                        label = { Text(stringResource(R.string.reg_vehicle_type)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = AppTextFieldDefaults.outlined(),
                    )
                    OutlinedTextField(
                        value = primaryRegion,
                        onValueChange = { primaryRegion = it },
                        label = { Text(stringResource(R.string.reg_primary_region)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = AppTextFieldDefaults.outlined(),
                    )
                }
                WizardPane.COMMUNITY -> {
                    OutlinedTextField(
                        value = nickname,
                        onValueChange = { nickname = it; error = null },
                        label = { Text(stringResource(R.string.reg_nickname_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = AppTextFieldDefaults.outlined(),
                    )
                    OutlinedTextField(
                        value = bio,
                        onValueChange = { bio = it },
                        label = { Text(stringResource(R.string.reg_bio_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = AppTextFieldDefaults.outlined(),
                    )
                    SwitchRow(
                        label = stringResource(R.string.reg_visibility_bio),
                        checked = bioVisible,
                        onCheckedChange = { bioVisible = it },
                    )
                    SwitchRow(
                        label = stringResource(R.string.reg_visibility_avatar),
                        checked = avatarVisible,
                        onCheckedChange = { avatarVisible = it },
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
                    when (pane) {
                        WizardPane.BASIC -> {
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
                                    onSuccess = { pane = WizardPane.PROFESSIONAL },
                                    onFailure = {
                                        error = context.getString(R.string.profile_setup_name_required)
                                    },
                                )
                            }
                        }
                        WizardPane.PROFESSIONAL -> {
                            busy = true
                            viewModel.completeProfessional(
                                companyName = companyName,
                                cdlNumber = cdlNumber,
                                vehicleType = vehicleType,
                                primaryRegion = primaryRegion,
                            ) {
                                busy = false
                                pane = WizardPane.COMMUNITY
                            }
                        }
                        WizardPane.COMMUNITY -> {
                            if (nickname.isBlank()) {
                                error = context.getString(R.string.profile_setup_name_required)
                                return@Button
                            }
                            busy = true
                            viewModel.completeCommunity(
                                nickname = nickname,
                                bio = bio,
                                visibility = CommunityVisibilitySettings(
                                    nicknameVisible = true,
                                    bioVisible = bioVisible,
                                    avatarVisible = avatarVisible,
                                ),
                            ) { result ->
                                busy = false
                                result.fold(
                                    onSuccess = { onCompleted() },
                                    onFailure = {
                                        error = context.getString(R.string.profile_setup_name_required)
                                    },
                                )
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !busy,
            ) {
                Text(
                    stringResource(
                        if (pane == WizardPane.COMMUNITY) R.string.profile_setup_continue
                        else R.string.profile_setup_next,
                    ),
                )
            }
            if (pane != WizardPane.BASIC) {
                TextButton(
                    onClick = {
                        if (busy) return@TextButton
                        busy = true
                        when (pane) {
                            WizardPane.PROFESSIONAL -> viewModel.skipProfessional {
                                busy = false
                                pane = WizardPane.COMMUNITY
                            }
                            WizardPane.COMMUNITY -> viewModel.skipCommunity {
                                busy = false
                                onCompleted()
                            }
                            WizardPane.BASIC -> Unit
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !busy,
                ) {
                    Text(stringResource(R.string.reg_skip_later))
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val tc = LocalTruckColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = tc.TextPrimary, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
