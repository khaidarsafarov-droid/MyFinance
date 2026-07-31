package com.truckerload.presentation.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.domain.geo.CountryCatalog
import com.truckerload.domain.social.SocialResult
import com.truckerload.presentation.components.CountryPickerField
import com.truckerload.presentation.components.PhoneWithCountryField
import com.truckerload.presentation.components.TlButton as Button
import com.truckerload.presentation.di.LocalProfileRepository
import com.truckerload.presentation.di.LocalSocialSyncCoordinator
import com.truckerload.presentation.di.LocalUserProfileStore
import com.truckerload.presentation.screens.social.ProfileAvatar
import com.truckerload.presentation.screens.social.ProfileAvatarPickerSheet
import com.truckerload.presentation.theme.AppTextFieldDefaults
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.LocalTruckColors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Multi-step Truck Load onboarding: personal → professional (CDL / truck) → home hub.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(
    onCompleted: () -> Unit,
) {
    val tc = LocalTruckColors.current
    val context = LocalContext.current
    val profileRepository = LocalProfileRepository.current
    val socialSyncCoordinator = LocalSocialSyncCoordinator.current
    val userProfileStore = LocalUserProfileStore.current
    val userProfile by userProfileStore.profile.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var step by remember { mutableIntStateOf(0) }
    var displayName by remember {
        mutableStateOf(userProfile?.displayName?.takeIf { it != userProfile?.email }.orEmpty())
    }
    val parsedPhone = remember(userProfile?.phoneNumber) {
        CountryCatalog.parsePhone(userProfile?.phoneNumber)
    }
    var phoneCountry by remember { mutableStateOf(parsedPhone.first) }
    var nationalNumber by remember { mutableStateOf(parsedPhone.second) }
    var homeCountry by remember { mutableStateOf(CountryCatalog.default) }
    var dateOfBirthText by remember { mutableStateOf("") }
    var truckType by remember { mutableStateOf("") }
    var licenseClass by remember { mutableStateOf("CDL A") }
    var cdlNumber by remember { mutableStateOf("") }
    var axleCountText by remember { mutableStateOf("") }
    var homeHubCity by remember { mutableStateOf("") }
    var avatarUrl by remember { mutableStateOf(userProfile?.photoUrl) }
    var showAvatarPicker by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var isUploadingAvatar by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val defaultDriverName = stringResource(R.string.default_driver_name)

    LaunchedEffect(defaultDriverName) {
        socialSyncCoordinator.ensureInitialized()
        val profile = profileRepository.watchMyEnhancedProfile().first()
        if (displayName.isBlank() && profile.displayName.isNotBlank() &&
            profile.displayName !in setOf(defaultDriverName, "Driver", "User")
        ) {
            displayName = profile.displayName
        }
        if (nationalNumber.isBlank() && !profile.phoneNumber.isNullOrBlank()) {
            val parsed = CountryCatalog.parsePhone(profile.phoneNumber)
            phoneCountry = parsed.first
            nationalNumber = parsed.second
        }
        CountryCatalog.byIso2(profile.homeState)?.let { homeCountry = it }
        if (avatarUrl.isNullOrBlank() && !profile.avatarUrl.isNullOrBlank()) {
            avatarUrl = profile.avatarUrl
        }
        if (truckType.isBlank() && profile.truckType != com.truckerload.domain.social.TruckType.OTHER) {
            truckType = profile.truckType.label
        }
        if (licenseClass.isBlank() && profile.licenseClass.isNotBlank()) {
            licenseClass = profile.licenseClass
        }
        if (cdlNumber.isBlank() && profile.cdlNumber.isNotBlank()) {
            cdlNumber = profile.cdlNumber
        }
        if (axleCountText.isBlank() && profile.axleCount > 0) {
            axleCountText = profile.axleCount.toString()
        }
        if (homeHubCity.isBlank() && profile.homeHubCity.isNotBlank()) {
            homeHubCity = profile.homeHubCity
        }
        profile.dateOfBirthEpochDay?.let { epoch ->
            if (dateOfBirthText.isBlank()) {
                dateOfBirthText = LocalDate.ofEpochDay(epoch).format(DOB_FORMAT)
            }
        }
    }

    ProfileAvatarPickerSheet(
        visible = showAvatarPicker,
        hasAvatar = !avatarUrl.isNullOrBlank(),
        onDismiss = { showAvatarPicker = false },
        onBitmapSelected = { bitmap ->
            scope.launch {
                isUploadingAvatar = true
                when (val result = profileRepository.uploadAvatar(bitmap)) {
                    is SocialResult.Success -> {
                        avatarUrl = result.data
                        isUploadingAvatar = false
                    }
                    is SocialResult.Error -> {
                        isUploadingAvatar = false
                        snackbarHostState.showSnackbar(result.message)
                    }
                }
            }
        },
        onRemove = {
            scope.launch {
                profileRepository.removeAvatar()
                avatarUrl = null
            }
        },
    )

    fun parseDobOrNull(): Long? {
        val raw = dateOfBirthText.trim()
        if (raw.isBlank()) return null
        return try {
            LocalDate.parse(raw, DOB_FORMAT).toEpochDay()
        } catch (_: DateTimeParseException) {
            null
        }
    }

    fun validateStep(): Boolean {
        error = null
        when (step) {
            0 -> {
                if (displayName.isBlank()) {
                    error = context.getString(R.string.profile_setup_name_required)
                    return false
                }
                if (dateOfBirthText.isNotBlank() && parseDobOrNull() == null) {
                    error = context.getString(R.string.profile_setup_dob_invalid)
                    return false
                }
                val e164 = CountryCatalog.formatE164(phoneCountry, nationalNumber)
                if (e164.filter { it.isDigit() }.length < 8) {
                    error = context.getString(R.string.profile_setup_phone_required)
                    return false
                }
            }
            1 -> {
                if (licenseClass.isBlank()) {
                    error = context.getString(R.string.profile_setup_license_required)
                    return false
                }
            }
            2 -> {
                if (homeHubCity.isBlank()) {
                    error = context.getString(R.string.profile_setup_hub_required)
                    return false
                }
            }
        }
        return true
    }

    Scaffold(
        containerColor = BentoGlassTheme.ScreenBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
            LinearProgressIndicator(
                progress = { (step + 1) / 3f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Text(
                text = stringResource(
                    when (step) {
                        0 -> R.string.profile_setup_step_personal
                        1 -> R.string.profile_setup_step_professional
                        else -> R.string.profile_setup_step_hub
                    },
                ),
                style = MaterialTheme.typography.titleMedium,
                color = tc.TextPrimary,
            )
            Text(
                text = stringResource(R.string.profile_setup_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = tc.TextSecondary,
            )

            when (step) {
                0 -> {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        ProfileAvatar(
                            avatarUrl = avatarUrl,
                            isUploading = isUploadingAvatar,
                            onClick = { showAvatarPicker = true },
                        )
                    }
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it; error = null },
                        label = { Text(stringResource(R.string.social_display_name)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = AppTextFieldDefaults.outlined(),
                    )
                    OutlinedTextField(
                        value = dateOfBirthText,
                        onValueChange = { dateOfBirthText = it; error = null },
                        label = { Text(stringResource(R.string.profile_setup_dob)) },
                        placeholder = { Text(stringResource(R.string.profile_setup_dob_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = AppTextFieldDefaults.outlined(),
                    )
                    CountryPickerField(
                        selected = homeCountry,
                        onSelected = { homeCountry = it; error = null },
                    )
                    PhoneWithCountryField(
                        country = phoneCountry,
                        nationalNumber = nationalNumber,
                        onCountryChange = { phoneCountry = it; error = null },
                        onNationalNumberChange = { nationalNumber = it; error = null },
                    )
                }
                1 -> {
                    OutlinedTextField(
                        value = licenseClass,
                        onValueChange = { licenseClass = it; error = null },
                        label = { Text(stringResource(R.string.profile_setup_license_class)) },
                        placeholder = { Text(stringResource(R.string.profile_setup_license_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = AppTextFieldDefaults.outlined(),
                    )
                    OutlinedTextField(
                        value = cdlNumber,
                        onValueChange = { cdlNumber = it; error = null },
                        label = { Text(stringResource(R.string.profile_setup_cdl_number)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = AppTextFieldDefaults.outlined(),
                    )
                    OutlinedTextField(
                        value = truckType,
                        onValueChange = { truckType = it },
                        label = { Text(stringResource(R.string.social_truck_type)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = AppTextFieldDefaults.outlined(),
                    )
                    OutlinedTextField(
                        value = axleCountText,
                        onValueChange = { axleCountText = it.filter { ch -> ch.isDigit() }; error = null },
                        label = { Text(stringResource(R.string.profile_setup_axles)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = AppTextFieldDefaults.outlined(),
                    )
                }
                else -> {
                    OutlinedTextField(
                        value = homeHubCity,
                        onValueChange = { homeHubCity = it; error = null },
                        label = { Text(stringResource(R.string.profile_setup_hub_city)) },
                        placeholder = { Text(stringResource(R.string.profile_setup_hub_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = AppTextFieldDefaults.outlined(),
                    )
                }
            }

            error?.let {
                Text(text = it, color = tc.AccentExpense, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (step > 0) {
                    TextButton(
                        onClick = { step -= 1; error = null },
                        enabled = !isSaving,
                    ) {
                        Text(stringResource(R.string.common_back))
                    }
                }
                Button(
                    onClick = {
                        if (!validateStep()) return@Button
                        if (step < 2) {
                            step += 1
                            return@Button
                        }
                        if (isSaving) return@Button
                        isSaving = true
                        error = null
                        scope.launch {
                            val e164 = CountryCatalog.formatE164(phoneCountry, nationalNumber)
                            when (
                                val result = profileRepository.completeProfileSetup(
                                    displayName = displayName,
                                    phoneNumber = e164,
                                    homeCountryIso2 = homeCountry.iso2,
                                    truckType = truckType,
                                    dateOfBirthEpochDay = parseDobOrNull(),
                                    licenseClass = licenseClass,
                                    cdlNumber = cdlNumber,
                                    axleCount = axleCountText.toIntOrNull() ?: 0,
                                    homeHubCity = homeHubCity,
                                )
                            ) {
                                is SocialResult.Success -> onCompleted()
                                is SocialResult.Error -> {
                                    error = result.message
                                    isSaving = false
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    enabled = !isSaving && !isUploadingAvatar,
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    Text(
                        stringResource(
                            if (step < 2) R.string.profile_setup_next
                            else R.string.profile_setup_continue,
                        ),
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private val DOB_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
