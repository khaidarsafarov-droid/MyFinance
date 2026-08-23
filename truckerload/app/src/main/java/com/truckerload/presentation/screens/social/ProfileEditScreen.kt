package com.truckerload.presentation.screens.social

import com.truckerload.presentation.icons.AppIcons

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.truckerload.presentation.components.TlButton as Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.domain.geo.CountryCatalog
import com.truckerload.domain.social.DriverStatus
import com.truckerload.domain.social.TruckType
import com.truckerload.presentation.components.CountryPickerField
import com.truckerload.presentation.components.PhoneWithCountryField
import com.truckerload.presentation.theme.AppTextFieldDefaults
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.LocalTruckColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val profile = uiState.profile
    val tc = LocalTruckColors.current
    var showAvatarPicker by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.avatarError, uiState.saveError) {
        val message = uiState.avatarError ?: uiState.saveError
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearAvatarError()
        }
    }

    ProfileAvatarPickerSheet(
        visible = showAvatarPicker,
        hasAvatar = !profile?.avatarUrl.isNullOrBlank(),
        onDismiss = { showAvatarPicker = false },
        onBitmapSelected = viewModel::uploadAvatar,
        onRemove = viewModel::removeAvatar,
    )

    var hydrated by remember { mutableStateOf(false) }
    var displayName by remember { mutableStateOf("") }
    var truckType by remember { mutableStateOf("") }
    var experienceYears by remember { mutableStateOf("") }
    var homeCountry by remember { mutableStateOf(CountryCatalog.default) }
    var routes by remember { mutableStateOf("") }
    var about by remember { mutableStateOf("") }
    var status by remember { mutableStateOf(DriverStatus.ONLINE) }
    var licenseClass by remember { mutableStateOf("") }
    var phoneCountry by remember { mutableStateOf(CountryCatalog.default) }
    var nationalNumber by remember { mutableStateOf("") }
    var specialties by remember { mutableStateOf("") }

    LaunchedEffect(profile?.id, profile != null) {
        val loaded = profile ?: return@LaunchedEffect
        if (hydrated) return@LaunchedEffect
        displayName = loaded.displayName
        truckType = loaded.truckType.takeIf { it != TruckType.OTHER }?.label.orEmpty()
        experienceYears = loaded.experienceYears.takeIf { it > 0 }?.toString().orEmpty()
        homeCountry = CountryCatalog.byIso2(loaded.homeState) ?: CountryCatalog.default
        routes = loaded.preferredRoutes.joinToString(", ")
        about = loaded.about
        status = loaded.status
        licenseClass = loaded.licenseClass
        val parsedPhone = CountryCatalog.parsePhone(loaded.phoneNumber)
        phoneCountry = parsedPhone.first
        nationalNumber = parsedPhone.second
        specialties = loaded.specialties.joinToString(", ")
        hydrated = true
    }

    Scaffold(
        modifier = modifier,
        containerColor = BentoGlassTheme.ScreenBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.edit_profile)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(AppIcons.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                ProfileAvatar(
                    avatarUrl = profile?.avatarUrl,
                    isUploading = uiState.isUploadingAvatar,
                    onClick = { showAvatarPicker = true },
                )
            }
            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                label = { Text(stringResource(R.string.social_display_name)) },
                modifier = Modifier.fillMaxWidth(),
                colors = AppTextFieldDefaults.outlined(),
                singleLine = true,
            )
            CountryPickerField(
                selected = homeCountry,
                onSelected = { homeCountry = it },
            )
            PhoneWithCountryField(
                country = phoneCountry,
                nationalNumber = nationalNumber,
                onCountryChange = { phoneCountry = it },
                onNationalNumberChange = { nationalNumber = it },
            )
            OutlinedTextField(
                value = truckType,
                onValueChange = { truckType = it },
                label = { Text(stringResource(R.string.social_truck_type)) },
                modifier = Modifier.fillMaxWidth(),
                colors = AppTextFieldDefaults.outlined(),
                singleLine = true,
            )
            OutlinedTextField(
                value = experienceYears,
                onValueChange = { experienceYears = it.filter { ch -> ch.isDigit() } },
                label = { Text(stringResource(R.string.experience_years)) },
                modifier = Modifier.fillMaxWidth(),
                colors = AppTextFieldDefaults.outlined(),
                singleLine = true,
            )
            OutlinedTextField(
                value = routes,
                onValueChange = { routes = it },
                label = { Text(stringResource(R.string.social_favorite_routes)) },
                modifier = Modifier.fillMaxWidth(),
                colors = AppTextFieldDefaults.outlined(),
            )
            OutlinedTextField(
                value = licenseClass,
                onValueChange = { licenseClass = it.uppercase().take(1) },
                label = { Text(stringResource(R.string.social_license_class)) },
                modifier = Modifier.fillMaxWidth(),
                colors = AppTextFieldDefaults.outlined(),
                singleLine = true,
            )
            OutlinedTextField(
                value = specialties,
                onValueChange = { specialties = it },
                label = { Text(stringResource(R.string.social_specialties)) },
                modifier = Modifier.fillMaxWidth(),
                colors = AppTextFieldDefaults.outlined(),
            )
            OutlinedTextField(
                value = about,
                onValueChange = { about = it },
                label = { Text(stringResource(R.string.about_me)) },
                modifier = Modifier.fillMaxWidth(),
                colors = AppTextFieldDefaults.outlined(),
                minLines = 3,
            )
            DriverStatus.entries.forEach { option ->
                Button(
                    onClick = { status = option },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = status != option,
                ) {
                    Text("${option.label} ${if (status == option) "✓" else ""}")
                }
            }
            Button(
                onClick = {
                    viewModel.saveEdit(
                        displayName = displayName,
                        truckType = truckType,
                        experienceYears = experienceYears.toIntOrNull() ?: 0,
                        homeState = homeCountry.iso2,
                        routes = routes,
                        about = about,
                        status = status,
                        licenseClass = licenseClass,
                        phoneNumber = CountryCatalog.formatE164(phoneCountry, nationalNumber),
                        specialties = specialties,
                        onResult = { ok -> if (ok) onSaved() },
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSaving && displayName.isNotBlank(),
            ) {
                Text(stringResource(R.string.common_save))
            }
        }
    }
}
