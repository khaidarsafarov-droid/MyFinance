package com.truckerload.presentation.screens.map

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.truckerload.R
import com.truckerload.data.remote.SupabaseFriendsRealtimeService
import com.truckerload.domain.friends.FriendShareLink
import com.truckerload.presentation.components.TlButton as Button
import com.truckerload.presentation.components.TlOutlinedButton as OutlinedButton
import com.truckerload.presentation.di.LocalAuthStore
import com.truckerload.presentation.di.LocalLoadRepository
import com.truckerload.presentation.di.LocalSettingsDataStore
import com.truckerload.presentation.di.LocalUserProfileStore
import com.truckerload.presentation.theme.AppFilterChipDefaults
import com.truckerload.presentation.theme.AppSwitchDefaults
import com.truckerload.presentation.theme.AppTextFieldDefaults
import com.truckerload.presentation.theme.AppTypography
import com.truckerload.presentation.theme.BentoGlassCard
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.ForestScreenTitle
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.theme.UiDimens
import com.truckerload.sync.FriendsLocationShareService
import kotlinx.coroutines.launch

private val COLOR_PAST = 0xFF9CA3AF.toInt()
private val COLOR_REMAINING = 0xFF2563EB.toInt()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsLiveMapScreen(
    onBack: () -> Unit = {},
) {
    val tc = LocalTruckColors.current
    val context = LocalContext.current
    val loadRepository = LocalLoadRepository.current
    val settings = LocalSettingsDataStore.current
    val authStore = LocalAuthStore.current
    val userProfileStore = LocalUserProfileStore.current
    val friendsApi = remember(authStore) { SupabaseFriendsRealtimeService(authStore) }
    val viewModel: FriendsLiveMapViewModel = viewModel(
        factory = FriendsLiveMapViewModel.Factory(
            loadRepository = loadRepository,
            settingsDataStore = settings,
            authStore = authStore,
            userProfileStore = userProfileStore,
            friendsApi = friendsApi,
        ),
    )
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
    val locationHelper = remember(context) { com.truckerload.utils.LocationHelper(context) }
    val scope = rememberCoroutineScope()

    suspend fun refreshMyLocation(): LatLng? {
        if (!hasLocationPermission) return null
        val loc = locationHelper.getCurrentLocation()
        val lat = loc?.latitude
        val lng = loc?.longitude
        if (lat == null || lng == null) return null
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.friends_share_path_toggle),
                        style = MaterialTheme.typography.titleSmall,
                        color = tc.TextPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    Switch(
                        checked = uiState.sharePathEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled && !hasLocationPermission) {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION,
                                    ),
                                )
                            }
                            viewModel.setSharePathEnabled(enabled)
                        },
                        colors = AppSwitchDefaults.colors(),
                    )
                }
                if (!hasLocationPermission) {
                    Text(
                        text = stringResource(R.string.friends_need_location_permission),
                        style = MaterialTheme.typography.bodySmall,
                        color = tc.AccentPrimary,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            uiState.errorMessage?.let { err ->
                item {
                    Text(text = err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }

            // Compact preview: only "me". Tap opens the full map.
            item {
                BentoGlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(16.dp)),
                        ) {
                            // Avoid two live MapViews at once (preview + fullscreen).
                            if (!mapExpanded) {
                                FriendsGoogleMap(
                                    overlays = emptyList(),
                                    selectedFriendId = null,
                                    myLocation = myLocation,
                                    showMyLocationLayer = hasLocationPermission,
                                    centerOnMeNonce = 0,
                                    interactive = false,
                                    onMarkerClick = {},
                                )
                            }
                            // Overlay captures the tap; MapView would otherwise eat it.
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { mapExpanded = true },
                            )
                            if (uiState.isLoading && myLocation == null && !mapExpanded) {
                                CircularProgressIndicator(
                                    modifier = Modifier.align(Alignment.Center),
                                    color = tc.AccentPrimary,
                                )
                            }
                        }
                        Text(
                            text = stringResource(R.string.friends_map_tap_open),
                            style = MaterialTheme.typography.labelMedium,
                            color = tc.TextSecondary,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        if (uiState.friends.isNotEmpty()) {
                            Text(
                                text = stringResource(R.string.friends_online_count, uiState.friends.size),
                                style = MaterialTheme.typography.labelSmall,
                                color = tc.TextSecondary,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                    }
                }
            }

            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { manageExpanded = !manageExpanded },
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(R.string.friends_manage_section),
                            style = MaterialTheme.typography.titleSmall,
                            color = tc.TextPrimary,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            if (manageExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = tc.TextSecondary,
                        )
                    }
                }
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

@Composable
private fun FriendsManageSection(
    uiState: FriendsLiveMapUiState,
    viewModel: FriendsLiveMapViewModel,
    context: android.content.Context,
) {
    val tc = LocalTruckColors.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (!uiState.supabaseReady) {
            Text(
                text = stringResource(R.string.friends_live_need_supabase),
                style = MaterialTheme.typography.bodySmall,
                color = tc.AccentPrimary,
            )
        }

        Text(
            text = stringResource(R.string.friends_my_nickname_title),
            style = MaterialTheme.typography.titleSmall,
            color = tc.TextPrimary,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = uiState.nicknameDraft,
                onValueChange = viewModel::setNicknameDraft,
                label = { Text(stringResource(R.string.friends_nickname_label)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = AppTextFieldDefaults.outlined(),
            )
            Button(onClick = { viewModel.saveNickname() }) {
                Text(stringResource(R.string.friends_nickname_save))
            }
        }
        when (uiState.nicknameMessage) {
            "invalid" -> Text(
                stringResource(R.string.friends_nickname_invalid),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
            "saved", "saved_local" -> Text(
                stringResource(R.string.friends_nickname_saved),
                color = tc.AccentPrimary,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Text(
            text = stringResource(R.string.friends_add_by_nickname_title),
            style = MaterialTheme.typography.titleSmall,
            color = tc.TextPrimary,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::setSearchQuery,
                label = { Text(stringResource(R.string.friends_search_nickname_label)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = AppTextFieldDefaults.outlined(),
            )
            Button(
                onClick = { viewModel.searchFriend() },
                enabled = !uiState.searchBusy,
            ) {
                if (uiState.searchBusy) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.PersonAdd, contentDescription = null)
                }
            }
        }
        uiState.searchHit?.let { hit ->
            Text(
                text = stringResource(R.string.friends_found, hit.displayName, hit.nickname),
                style = MaterialTheme.typography.bodyMedium,
                color = tc.TextPrimary,
            )
            Button(onClick = { viewModel.addSearchedFriend() }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.friends_add_button))
            }
        }
        if (uiState.searchNotFound || uiState.statusMessage == "not_found") {
            Text(
                text = stringResource(R.string.friends_not_in_app),
                style = MaterialTheme.typography.bodySmall,
                color = tc.TextSecondary,
            )
            OutlinedButton(
                onClick = {
                    val share = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, context.getString(R.string.friends_invite_share_text))
                    }
                    context.startActivity(
                        Intent.createChooser(share, context.getString(R.string.friends_invite_share_title)),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
                Text(stringResource(R.string.friends_invite_share_button))
            }
        }

        OutlinedButton(onClick = { viewModel.setShowOverlapsPanel(!uiState.showOverlapsPanel) }) {
            Icon(Icons.Default.Groups, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
            Text(stringResource(R.string.friends_overlap_button))
        }
        if (uiState.showOverlapsPanel) {
            if (uiState.overlaps.isEmpty()) {
                Text(
                    text = stringResource(R.string.friends_overlap_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = tc.TextSecondary,
                )
            } else {
                uiState.overlaps.forEach { match ->
                    Text(
                        text = "${match.friendDisplayName}: ${match.reason}",
                        style = MaterialTheme.typography.bodySmall,
                        color = tc.TextSecondary,
                    )
                }
            }
        }

        Text(
            text = stringResource(R.string.friends_sharing_list_title),
            style = MaterialTheme.typography.titleSmall,
            color = tc.TextPrimary,
        )
        if (uiState.shareLinks.isEmpty()) {
            Text(
                text = stringResource(R.string.friends_sharing_list_empty),
                style = MaterialTheme.typography.bodySmall,
                color = tc.TextSecondary,
            )
        }
        uiState.shareLinks.forEach { link ->
            FriendShareRow(
                link = link,
                editing = uiState.editingFriendId == link.friendUserId,
                onEdit = { viewModel.setEditingFriend(link.friendUserId) },
                onCloseEdit = { viewModel.setEditingFriend(null) },
                onSavePrefs = { loc, route ->
                    viewModel.updateSharePrefs(link.friendUserId, loc, route)
                },
                onDelete = { viewModel.removeFriend(link.friendUserId) },
                onFocusMap = {
                    viewModel.selectFriend(link.friendUserId)
                    viewModel.toggleShowPath(link.friendUserId)
                },
            )
        }

        Text(
            text = stringResource(R.string.friends_list_title),
            style = MaterialTheme.typography.titleSmall,
            color = tc.TextPrimary,
        )
        if (uiState.friends.isEmpty()) {
            Text(
                text = stringResource(R.string.friends_list_empty),
                style = MaterialTheme.typography.bodySmall,
                color = tc.TextSecondary,
            )
        }
        uiState.friends.forEach { friend ->
            val selected = friend.presence.userId == uiState.selectedFriendId
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = friend.presence.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (selected) tc.AccentPrimary else tc.TextPrimary,
                )
                friend.route?.let { route ->
                    Text(
                        text = "${route.originLabel} → ${route.destinationLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = tc.TextSecondary,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = friend.showPath,
                        onClick = { viewModel.toggleShowPath(friend.presence.userId) },
                        label = {
                            Text(
                                if (friend.showPath) {
                                    stringResource(R.string.friends_hide_path)
                                } else {
                                    stringResource(R.string.friends_show_path)
                                },
                            )
                        },
                        colors = AppFilterChipDefaults.colors(),
                    )
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.selectFriend(friend.presence.userId) },
                        label = { Text(stringResource(R.string.friends_focus)) },
                        colors = AppFilterChipDefaults.colors(),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FullscreenFriendsMapDialog(
    overlays: List<FriendMapOverlay>,
    selectedFriendId: String?,
    myLocation: LatLng?,
    showMyLocationLayer: Boolean,
    centerOnMeNonce: Int,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onMarkerClick: (String) -> Unit,
    onCenterMe: () -> Unit,
) {
    val tc = LocalTruckColors.current
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = BentoGlassTheme.ScreenBackground,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                FriendsGoogleMap(
                    overlays = overlays,
                    selectedFriendId = selectedFriendId,
                    myLocation = myLocation,
                    showMyLocationLayer = showMyLocationLayer,
                    centerOnMeNonce = centerOnMeNonce,
                    interactive = true,
                    onMarkerClick = onMarkerClick,
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp)
                        .size(UiDimens.ToolbarTouchTarget),
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = stringResource(R.string.friends_map_close),
                        tint = tc.TextPrimary,
                    )
                }
                FloatingActionButton(
                    onClick = onCenterMe,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(20.dp),
                    containerColor = tc.AccentPrimary,
                    contentColor = tc.Background,
                ) {
                    Icon(
                        Icons.Default.MyLocation,
                        contentDescription = stringResource(R.string.friends_center_on_me),
                    )
                }
                Text(
                    text = stringResource(R.string.friends_map_legend),
                    style = MaterialTheme.typography.labelSmall,
                    color = tc.TextSecondary,
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp),
                )
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = tc.AccentPrimary,
                    )
                }
            }
        }
    }
}

@Composable
private fun FriendShareRow(
    link: FriendShareLink,
    editing: Boolean,
    onEdit: () -> Unit,
    onCloseEdit: () -> Unit,
    onSavePrefs: (Boolean, Boolean) -> Unit,
    onDelete: () -> Unit,
    onFocusMap: () -> Unit,
) {
    val tc = LocalTruckColors.current
    var shareLoc by remember(link.friendUserId, link.shareMyLocation) { mutableStateOf(link.shareMyLocation) }
    var shareRoute by remember(link.friendUserId, link.shareMyRoute) { mutableStateOf(link.shareMyRoute) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "@${link.friendNickname}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = tc.TextPrimary,
                )
                Text(
                    text = link.friendDisplayName,
                    style = MaterialTheme.typography.bodySmall,
                    color = tc.TextSecondary,
                )
                Text(
                    text = stringResource(
                        R.string.friends_share_summary,
                        if (link.shareMyLocation) "✓" else "—",
                        if (link.shareMyRoute) "✓" else "—",
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = tc.TextSecondary,
                )
            }
            IconButton(onClick = onFocusMap) {
                Icon(Icons.Default.Groups, contentDescription = stringResource(R.string.friends_show_path))
            }
            IconButton(onClick = { if (editing) onCloseEdit() else onEdit() }) {
                Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.friends_edit_share))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.friends_remove))
            }
        }
        if (editing) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(stringResource(R.string.friends_pref_show_me), color = tc.TextPrimary)
                Switch(
                    checked = shareLoc,
                    onCheckedChange = { shareLoc = it },
                    colors = AppSwitchDefaults.colors(),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(stringResource(R.string.friends_pref_show_route), color = tc.TextPrimary)
                Switch(
                    checked = shareRoute,
                    onCheckedChange = { shareRoute = it },
                    colors = AppSwitchDefaults.colors(),
                )
            }
            Button(
                onClick = {
                    onSavePrefs(shareLoc, shareRoute)
                    onCloseEdit()
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.friends_prefs_save))
            }
        }
    }
}

@Composable
private fun FriendsGoogleMap(
    overlays: List<FriendMapOverlay>,
    selectedFriendId: String?,
    myLocation: LatLng?,
    showMyLocationLayer: Boolean,
    centerOnMeNonce: Int,
    onMarkerClick: (String) -> Unit,
    interactive: Boolean = true,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val meLabel = stringResource(R.string.friends_me_marker)
    var mapView by remember { mutableStateOf<MapView?>(null) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            val map = mapView ?: return@LifecycleEventObserver
            when (event) {
                Lifecycle.Event.ON_CREATE -> map.onCreate(null)
                Lifecycle.Event.ON_START -> map.onStart()
                Lifecycle.Event.ON_RESUME -> map.onResume()
                Lifecycle.Event.ON_PAUSE -> map.onPause()
                Lifecycle.Event.ON_STOP -> map.onStop()
                Lifecycle.Event.ON_DESTROY -> map.onDestroy()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView?.onDestroy()
        }
    }

    LaunchedEffect(overlays, selectedFriendId, myLocation, showMyLocationLayer, interactive, mapView) {
        val map = mapView ?: return@LaunchedEffect
        map.getMapAsync { googleMap ->
            googleMap.clear()
            googleMap.uiSettings.isZoomControlsEnabled = interactive
            googleMap.uiSettings.isScrollGesturesEnabled = interactive
            googleMap.uiSettings.isZoomGesturesEnabled = interactive
            googleMap.uiSettings.isRotateGesturesEnabled = interactive
            googleMap.uiSettings.isTiltGesturesEnabled = interactive
            googleMap.uiSettings.isMyLocationButtonEnabled = false
            runCatching {
                googleMap.isMyLocationEnabled = showMyLocationLayer && interactive
            }
            myLocation?.let { me ->
                googleMap.addMarker(
                    MarkerOptions()
                        .position(me)
                        .title(meLabel)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)),
                )
            }
            overlays.forEach { friend ->
                val pos = LatLng(friend.presence.latitude, friend.presence.longitude)
                val marker = googleMap.addMarker(
                    MarkerOptions()
                        .position(pos)
                        .title(friend.presence.displayName)
                        .icon(
                            BitmapDescriptorFactory.defaultMarker(
                                if (friend.presence.userId == selectedFriendId) {
                                    BitmapDescriptorFactory.HUE_AZURE
                                } else {
                                    BitmapDescriptorFactory.HUE_ORANGE
                                },
                            ),
                        ),
                )
                marker?.tag = friend.presence.userId
                if (friend.showPath) {
                    if (friend.past.size >= 2) {
                        googleMap.addPolyline(
                            PolylineOptions()
                                .addAll(friend.past.map { LatLng(it.lat, it.lng) })
                                .color(COLOR_PAST)
                                .width(10f),
                        )
                    }
                    if (friend.remaining.size >= 2) {
                        googleMap.addPolyline(
                            PolylineOptions()
                                .addAll(friend.remaining.map { LatLng(it.lat, it.lng) })
                                .color(COLOR_REMAINING)
                                .width(10f),
                        )
                    }
                }
            }
            googleMap.setOnMarkerClickListener { marker ->
                if (!interactive) return@setOnMarkerClickListener true
                (marker.tag as? String)?.let(onMarkerClick)
                false
            }
            val selected = overlays.firstOrNull { it.presence.userId == selectedFriendId }
            when {
                selected != null -> {
                    googleMap.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(selected.presence.latitude, selected.presence.longitude),
                            8f,
                        ),
                    )
                }
                myLocation != null && centerOnMeNonce == 0 -> {
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 12f))
                }
                overlays.isNotEmpty() && centerOnMeNonce == 0 -> {
                    val f = overlays.first()
                    googleMap.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(f.presence.latitude, f.presence.longitude),
                            6f,
                        ),
                    )
                }
            }
        }
    }

    LaunchedEffect(centerOnMeNonce, myLocation, mapView) {
        if (centerOnMeNonce == 0) return@LaunchedEffect
        val target = myLocation ?: return@LaunchedEffect
        val map = mapView ?: return@LaunchedEffect
        map.getMapAsync { googleMap ->
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(target, 14f))
        }
    }

    AndroidView(
        factory = { ctx ->
            MapView(ctx).also { mapView = it }
        },
        modifier = Modifier.fillMaxSize(),
    )
}
