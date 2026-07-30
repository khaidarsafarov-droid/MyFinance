package com.truckerload.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.data.preferences.RpmThresholds
import com.truckerload.domain.model.Load
import com.truckerload.R
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.SoftUiColors
import com.truckerload.presentation.theme.SoftUiElevation

private val DeleteSwipeColor = Color(0xFFE57373)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableLoadCard(
    load: Load,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    rpmThresholds: RpmThresholds,
    modifier: Modifier = Modifier,
    onCameraClick: (() -> Unit)? = null,
    onScanClick: (() -> Unit)? = null,
    onToggleFinished: (() -> Unit)? = null,
    /** Bump to snap the card back to settled (e.g. after canceling delete confirm). */
    settleKey: Any = Unit,
) {
    val cardShape = remember { RoundedCornerShape(BentoGlassTheme.CardRadius) }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                // Request delete confirm; keep the card settled so cancel does not leave a ghost.
                onDelete()
                false
            } else {
                false
            }
        },
    )
    LaunchedEffect(settleKey, load.id) {
        dismissState.snapTo(SwipeToDismissBoxValue.Settled)
    }
    val showDeleteBackground = dismissState.progress > 0.02f

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            if (showDeleteBackground) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(cardShape)
                        .background(DeleteSwipeColor)
                        .padding(end = 24.dp),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(R.string.common_delete),
                            tint = Color.White,
                        )
                        Text(
                            text = stringResource(R.string.common_delete),
                            color = Color.White,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        },
        modifier = modifier
            .padding(horizontal = 16.dp)
            .clip(cardShape),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .shadow(
                    elevation = SoftUiElevation.Card,
                    shape = cardShape,
                    ambientColor = SoftUiColors.ShadowTint,
                    spotColor = SoftUiColors.ShadowNeutral,
                ),
            shape = cardShape,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            LoadCard(
                load = load,
                onClick = onClick,
                wrapInCard = false,
                rpmThresholds = rpmThresholds,
                onCameraClick = onCameraClick,
                onScanClick = onScanClick,
                onToggleFinished = onToggleFinished,
            )
        }
    }
}
