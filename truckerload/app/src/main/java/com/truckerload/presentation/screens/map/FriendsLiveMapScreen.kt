package com.truckerload.presentation.screens.map

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.model.LatLng
import com.truckerload.R
import com.truckerload.data.remote.SupabaseFriendsRealtimeService
import com.truckerload.presentation.di.LocalAuthStore
import com.truckerload.presentation.theme.AppTypography
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.ForestScreenTitle
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.theme.UiDimens
import com.truckerload.sync.FriendsLocationShareService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)@Composable
fun FriendsLiveMapScreen(
    onBack: () -> Unit = {},
) {
    val tc = LocalTruckColors.current
    val context = LocalContext.current
    val authStore = LocalAuthStore.current
    val friendsApi = remember(authStore) { SupabaseFriendsRealtimeService(authStore) }
    val locationHelper = remember(context) { com.truckerload.utils.LocationHelper(context) }
    val viewModel: FriendsLiveMapViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        hasLocationPermission = result.values.any { it }
    }

    var myLocation by remember { mutableStateOf<LatLng?>(null) }
    var centerOnMeNonce by remember { mutableIntStateOf(0) }
    var mapExpanded by remember { mutableStateOf(false) }
    var manageExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    suspend fun refreshMyLocation(): LatLng? {
        if (!hasLocationPermission) return null
        val loc = locationHelper.getCurrentLocation()
        val lat = loc?.latitude
        val lng = loc?.longitude
        if (lat == null || lng == null) return null
        viewModel.updateMyLocation(lat, lng)
        return LatLng(lat, lng).also { myLocation = it }
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        }
    }

    LaunchedEffect(hasLocationPermission) {
        if (!hasLocationPermission) {
            myLocation = null
            return@LaunchedEffect
        }
        refreshMyLocation()
    }

    LaunchedEffect(uiState.sharePathEnabled, hasLocationPermission, uiState.supabaseReady) {
        if (uiState.sharePathEnabled && uiState.supabaseReady && hasLocationPermission) {
            FriendsLocationShareService.start(context)
        } else {
            FriendsLocationShareService.stop(context)
        }
    }

    if (mapExpanded) {
        FullscreenFriendsMapDialog(
            overlays = uiState.friends,
            myPathPast = uiState.myPathPast,
            myPathRemaining = uiState.myPathRemaining,
            selectedFriendId = uiState.selectedFriendId,
            myLocation = myLocation,
            showMyLocationLayer = hasLocationPermission,
            centerOnMeNonce = centerOnMeNonce,
            isLoading = uiState.isLoading && uiState.friends.isEmpty() && myLocation == null,
            onDismiss = { mapExpanded = false },
            onMarkerClick = viewModel::selectFriend,
            onCenterMe = {
                if (!hasLocationPermission) {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                        ),
                    )
                    return@FullscreenFriendsMapDialog
                }
                scope.launch {
                    refreshMyLocation()
                    centerOnMeNonce += 1
                }
            },
        )
    }

    Scaffold(
        containerColor = BentoGlassTheme.ScreenBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        ForestScreenTitle(stringResource(R.string.friends_live_map_title))
                        Text(
                            text = stringResource(R.string.friends_live_map_subtitle),
                            style = AppTypography.Subtitle,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.size(UiDimens.ToolbarTouchTarget)) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                            tint = tc.TextPrimary,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.common_refresh))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BentoGlassTheme.ScreenBackground,
                    titleContentColor = tc.TextPrimary,
                    actionIconContentColor = tc.TextPrimary,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                FriendsSharePathToggleRow(
                    sharePathEnabled = uiState.sharePathEnabled,
                    hasLocationPermission = hasLocationPermission,
                    onSharePathChange = viewModel::setSharePathEnabled,
                    onRequestLocationPermission = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                            ),
                        )
                    },
                )
            }
            uiState.errorMessage?.let { err ->
                item {
                    Text(text = err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }

            item {
                FriendsMapPreviewCard(
                    mapExpanded = mapExpanded,
                    myPathPast = uiState.myPathPast,
                    myPathRemaining = uiState.myPathRemaining,
                    myLocation = myLocation,
                    showMyLocationLayer = hasLocationPermission,
                    isLoading = uiState.isLoading,
                    myRouteSummary = uiState.myRouteSummary,
                    friendsCount = uiState.friends.size,
                    onOpenMap = { mapExpanded = true },
                )
            }

            item {
                FriendsManageSectionHeader(
                    expanded = manageExpanded,
                    onToggle = { manageExpanded = !manageExpanded },
                )
            }

            if (manageExpanded) {
                item {
                    FriendsManageSection(
                        uiState = uiState,
                        viewModel = viewModel,
                        context = context,
                    )
                }
            }

            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}
