package com.truckerload.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.presentation.theme.SoftUiColors
import com.truckerload.presentation.theme.SoftUiElevation
import com.truckerload.presentation.theme.UiDimens

@Composable
fun CameraButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    elevated: Boolean = true,
    size: Dp = UiDimens.FabSize,
    iconSize: Dp = UiDimens.IconFab,
) {
    val gradient = Brush.horizontalGradient(
        listOf(SoftUiColors.PurpleStart, SoftUiColors.PurpleEnd),
    )
    Box(
        modifier = modifier
            .then(
                if (elevated) {
                    Modifier.shadow(
                        SoftUiElevation.Fab,
                        CircleShape,
                        ambientColor = SoftUiColors.ShadowTint,
                        spotColor = SoftUiColors.ShadowNeutral,
                    )
                } else {
                    Modifier
                },
            )
            .size(size)
            .clip(CircleShape)
            .background(gradient)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.CameraAlt,
            contentDescription = stringResource(R.string.camera),
            tint = Color.White,
            modifier = Modifier.size(iconSize),
        )
    }
}
