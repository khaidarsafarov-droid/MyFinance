package com.truckerload.presentation.screens.social

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.truckerload.R
import com.truckerload.domain.social.CommunityReportReason
import com.truckerload.presentation.components.TlTextButton as TextButton

@Composable
internal fun ChatSafetyMenu(
    enabled: Boolean,
    onOpenProfile: () -> Unit,
    onBlock: () -> Unit,
    onReport: (CommunityReportReason) -> Unit,
) {
    if (!enabled) return
    var menuOpen by remember { mutableStateOf(false) }
    var reportOpen by remember { mutableStateOf(false) }
    IconButton(onClick = { menuOpen = true }) {
        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.social_safety_menu_cd))
    }
    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.social_view_profile)) },
            onClick = {
                menuOpen = false
                onOpenProfile()
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.social_block_user)) },
            onClick = {
                menuOpen = false
                onBlock()
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.social_report_user)) },
            onClick = {
                menuOpen = false
                reportOpen = true
            },
        )
    }
    if (reportOpen) {
        ReportReasonDialog(
            onDismiss = { reportOpen = false },
            onPick = { reason ->
                reportOpen = false
                onReport(reason)
            },
        )
    }
}

@Composable
internal fun ReportReasonDialog(
    onDismiss: () -> Unit,
    onPick: (CommunityReportReason) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.social_report_title)) },
        text = {
            Column {
                Text(stringResource(R.string.social_report_body))
            }
        },
        confirmButton = {
            Column {
                CommunityReportReason.entries.forEach { reason ->
                    TextButton(onClick = { onPick(reason) }) {
                        Text(stringResource(reportReasonLabel(reason)))
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
    )
}

internal fun reportReasonLabel(reason: CommunityReportReason): Int = when (reason) {
    CommunityReportReason.SPAM -> R.string.social_report_reason_spam
    CommunityReportReason.HARASSMENT -> R.string.social_report_reason_harassment
    CommunityReportReason.HATE -> R.string.social_report_reason_hate
    CommunityReportReason.SEXUAL -> R.string.social_report_reason_sexual
    CommunityReportReason.SCAM -> R.string.social_report_reason_scam
    CommunityReportReason.OTHER -> R.string.social_report_reason_other
}
