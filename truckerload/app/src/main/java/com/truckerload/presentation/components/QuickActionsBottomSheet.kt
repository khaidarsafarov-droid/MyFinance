package com.truckerload.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.theme.UiDimens
import com.truckerload.utils.FeedbackManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickActionsBottomSheet(
    onDismiss: () -> Unit,
    onAddLoad: () -> Unit,
    onCamera: () -> Unit,
    onScan: () -> Unit,
    onAddDiesel: () -> Unit,
    onVoiceAssistant: () -> Unit = {},
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val tc = LocalTruckColors.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.quick_actions_title),
                style = MaterialTheme.typography.titleMedium,
                color = tc.TextPrimary,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            QuickActionRow(
                icon = Icons.Default.Add,
                title = stringResource(R.string.home_add_load_button),
                onClick = {
                    FeedbackManager.onNavSelect()
                    onDismiss()
                    onAddLoad()
                },
            )
            QuickActionRow(
                icon = Icons.Default.CameraAlt,
                title = stringResource(R.string.widget_camera),
                onClick = {
                    FeedbackManager.onNavSelect()
                    onDismiss()
                    onCamera()
                },
            )
            QuickActionRow(
                icon = Icons.Outlined.DocumentScanner,
                title = stringResource(R.string.widget_scanner),
                onClick = {
                    FeedbackManager.onNavSelect()
                    onDismiss()
                    onScan()
                },
            )
            QuickActionRow(
                icon = Icons.Default.LocalGasStation,
                title = stringResource(R.string.quick_actions_diesel),
                onClick = {
                    FeedbackManager.onNavSelect()
                    onDismiss()
                    onAddDiesel()
                },
            )
            QuickActionRow(
                icon = Icons.Default.Mic,
                title = stringResource(R.string.assistant_title),
                onClick = {
                    FeedbackManager.onNavSelect()
                    onDismiss()
                    onVoiceAssistant()
                },
            )
        }
    }
}

@Composable
private fun QuickActionRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
) {
    val tc = LocalTruckColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = UiDimens.TouchTarget)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            icon,
            contentDescription = title,
            tint = tc.AccentPrimary,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = tc.TextPrimary,
        )
    }
}
