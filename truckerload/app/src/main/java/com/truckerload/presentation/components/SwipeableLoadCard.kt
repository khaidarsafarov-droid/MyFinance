package com.truckerload.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.presentation.icons.AppIcons
import com.truckerload.presentation.theme.LocalBrandPalette
import com.truckerload.presentation.theme.LocalSemantics
import com.truckerload.presentation.theme.loadSharedBounds
import com.truckerload.utils.FeedbackManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableLoadCard(
    card: LoadCardModel,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    onCameraClick: (() -> Unit)? = null,
    onScanClick: (() -> Unit)? = null,
    /** Bump to snap the card back to settled (e.g. after undoing a delete). */
    settleKey: Any = Unit,
    /** Disable swipe-to-delete in multi-column tablet grids (gesture conflicts). */
    enableSwipe: Boolean = true,
) {
    val brand = LocalBrandPalette.current
    val cardShape = remember { RoundedCornerShape(22.dp) }
    val highlighted = card.id.isNotBlank() && card.id in LocalShareHighlightLoadIds.current
    val cardBorder = BorderStroke(
        width = if (highlighted) 2.dp else 1.dp,
        color = if (highlighted) brand.goldStart else brand.cardBorder,
    )

    if (!enableSwipe) {
        Surface(
            modifier = modifier
                .loadSharedBounds(card.id)
                .then(if (highlighted) Modifier.semantics { selected = true } else Modifier)
                .shadow(
                    elevation = 4.dp,
                    shape = cardShape,
                    ambientColor = brand.shadow,
                    spotColor = brand.shadow,
                ),
            shape = cardShape,
            color = brand.card,
            border = cardBorder,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            LoadCard(
                card = card,
                onClick = onClick,
                wrapInCard = false,
                onCameraClick = onCameraClick,
                onScanClick = onScanClick,
            )
        }
        return
    }

    val dismissScope = rememberCoroutineScope()
    val dismissState = rememberSwipeToDismissBoxState()
    LaunchedEffect(settleKey, card.id) {
        dismissState.snapTo(SwipeToDismissBoxValue.Settled)
    }
    var swipeHapticFired by remember(card.id, settleKey) { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(dismissState.progress) {
        if (dismissState.progress > 0.35f && !swipeHapticFired) {
            swipeHapticFired = true
            FeedbackManager.onSwipeAction()
            haptic.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
        }
        if (dismissState.progress < 0.05f) {
            swipeHapticFired = false
        }
    }
    val showDeleteBackground = dismissState.progress > 0.02f

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        onDismiss = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                // Soft-delete with undo snackbar; snap back so undo can restore the card.
                onDelete()
                dismissScope.launch { dismissState.snapTo(SwipeToDismissBoxValue.Settled) }
            }
        },
        backgroundContent = {
            if (showDeleteBackground) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(cardShape)
                        .background(LocalSemantics.current.danger)
                        .padding(end = 24.dp),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            AppIcons.Delete,
                            contentDescription = stringResource(R.string.common_delete),
                            tint = LocalSemantics.current.dangerContainer,
                        )
                        Text(
                            text = stringResource(R.string.common_delete),
                            color = LocalSemantics.current.dangerContainer,
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        },
        // Horizontal padding owned by the caller (HomeLoadCardRow / adaptive padding).
        modifier = modifier
            .loadSharedBounds(card.id)
            .then(if (highlighted) Modifier.semantics { selected = true } else Modifier)
            .clip(cardShape),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = cardShape,
            color = brand.card,
            border = cardBorder,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
        ) {
            LoadCard(
                card = card,
                onClick = onClick,
                wrapInCard = false,
                onCameraClick = onCameraClick,
                onScanClick = onScanClick,
            )
        }
    }
}
