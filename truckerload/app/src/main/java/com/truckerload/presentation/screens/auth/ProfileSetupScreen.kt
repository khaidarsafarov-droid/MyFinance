package com.truckerload.presentation.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.domain.geo.CountryCatalog
import com.truckerload.domain.social.SocialResult
import com.truckerload.presentation.components.CountryPickerField
import com.truckerload.presentation.components.PhoneWithCountryField
import com.truckerload.presentation.components.TlButton as Button
import com.truckerload.presentation.di.LocalSocialRepository
import com.truckerload.presentation.di.LocalUserProfileStore
import com.truckerload.presentation.screens.social.ProfileAvatar
import com.truckerload.presentation.screens.social.ProfileAvatarPickerSheet
import com.truckerload.presentation.theme.AppTextFieldDefaults
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.LocalTruckColors
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * First check-in: name, worldwide country, phone with country code, optional photo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(
    onCompleted: () -> Unit,
) {
    val tc = LocalTruckColors.current
    val socialRepository = LocalSocialRepository.current
    val userProfileStore = LocalUserProfileStore.current
    val userProfile by userProfileStore.profile.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var displayName by remember {
        mutableStateOf(userProfile?.displayName?.takeIf { it != userProfile?.email }.orEmpty())
    }
    val parsedPhone = remember(userProfile?.phoneNumber) {
        CountryCatalog.parsePhone(userProfile?.phoneNumber)
    }
    var phoneCountry by remember { mutableStateOf(parsedPhone.first) }
    var nationalNumber by remember { mutableStateOf(parsedPhone.second) }
    var homeCountry by remember { mutableStateOf(CountryCatalog.default) }
    var truckType by remember { mutableStateOf("") }
    var avatarUrl by remember { mutableStateOf(userProfile?.photoUrl) }
    var showAvatarPicker by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var isUploadingAvatar by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        socialRepository.ensureInitialized()
        val profile = socialRepository.watchMyEnhancedProfile().first()
        if (displayName.isBlank() && profile.displayName.isNotBlank() &&
            profile.displayName !in setOf("Водитель", "Driver", "User")
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
    }

    ProfileAvatarPickerSheet(
        visible = showAvatarPicker,
        hasAvatar = !avatarUrl.isNullOrBlank(),
        onDismiss = { showAvatarPicker = false },
        onBitmapSelected = { bitmap ->
            scope.launch {
                isUploadingAvatar = true
                when (val result = socialRepository.uploadAvatar(bitmap)) {
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
                socialRepository.removeAvatar()
                avatarUrl = null
            }
        },
    )

    Scaffold(
        containerColor = BentoGlassTheme.ScreenBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile_setup_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BentoGlassTheme.ScreenBackground,
                    titleContentColor = tc.TextPrimary,
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
                text = stringResource(R.string.profile_setup_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = tc.TextSecondary,
            )
            Spacer(modifier = Modifier.height(8.dp))
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
            OutlinedTextField(
                value = truckType,
                onValueChange = { truckType = it },
                label = { Text(stringResource(R.string.social_truck_type)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = AppTextFieldDefaults.outlined(),
            )
            error?.let {
                Text(text = it, color = tc.AccentExpense, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    if (isSaving) return@Button
                    isSaving = true
                    error = null
                    scope.launch {
                        val e164 = CountryCatalog.formatE164(phoneCountry, nationalNumber)
                        when (
                            val result = socialRepository.completeProfileSetup(
                                displayName = displayName,
                                phoneNumber = e164,
                                homeCountryIso2 = homeCountry.iso2,
                                truckType = truckType,
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
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = !isSaving && !isUploadingAvatar,
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Text(stringResource(R.string.profile_setup_continue))
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
