package com.truckerload.presentation.components

import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.truckerload.presentation.theme.UiDimens

/**
 * Centered dialog with tablet-friendly max width and dismiss-on-outside-tap.
 */
@Composable
fun AdaptiveAlertDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    containerColor: Color = AlertDialogDefaults.containerColor,
    iconContentColor: Color = AlertDialogDefaults.iconContentColor,
    titleContentColor: Color = AlertDialogDefaults.titleContentColor,
    textContentColor: Color = AlertDialogDefaults.textContentColor,
    properties: DialogProperties = DialogProperties(
        dismissOnBackPress = true,
        dismissOnClickOutside = true,
        usePlatformDefaultWidth = false,
    ),
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier.widthIn(min = 280.dp, max = UiDimens.DialogMaxWidth),
        confirmButton = confirmButton,
        dismissButton = dismissButton,
        icon = icon,
        title = title,
        text = text,
        containerColor = containerColor,
        iconContentColor = iconContentColor,
        titleContentColor = titleContentColor,
        textContentColor = textContentColor,
        properties = properties,
    )
}
