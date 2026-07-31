package com.truckerload.presentation.screens.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.android.gms.maps.model.LatLng
import com.truckerload.R
import com.truckerload.domain.friends.LatLngPoint
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.theme.UiDimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FullscreenFriendsMapDialog(
    overlays: List<FriendMapOverlay>,
    myPathPast: List<LatLngPoint>,
    myPathRemaining: List<LatLngPoint>,
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
                    myPathPast = myPathPast,
                    myPathRemaining = myPathRemaining,
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
