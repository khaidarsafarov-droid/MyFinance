package com.truckerload.presentation.screens.add

import com.truckerload.presentation.icons.AppIcons

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.presentation.theme.LocalReduceMotion
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.theme.rememberChipChrome

@Composable
fun AddLoadModeSelector(
    selected: AddLoadInputMode,
    onSelect: (AddLoadInputMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tc = LocalTruckColors.current
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.add_load_mode_title),
            style = MaterialTheme.typography.titleSmall,
            color = tc.TextPrimary,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ModeSegment(
                selected = selected == AddLoadInputMode.PASTE,
                onClick = { onSelect(AddLoadInputMode.PASTE) },
                label = stringResource(R.string.add_load_mode_paste),
                icon = AppIcons.ContentPaste,
                modifier = Modifier.weight(1f),
            )
            ModeSegment(
                selected = selected == AddLoadInputMode.MANUAL,
                onClick = { onSelect(AddLoadInputMode.MANUAL) },
                label = stringResource(R.string.add_load_mode_manual),
                icon = AppIcons.EditNote,
                modifier = Modifier.weight(1f),
            )
            ModeSegment(
                selected = selected == AddLoadInputMode.DOCUMENT,
                onClick = { onSelect(AddLoadInputMode.DOCUMENT) },
                label = stringResource(R.string.add_load_mode_document),
                icon = AppIcons.PhotoCamera,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * Equal-width segment. The icon sits above the label so a long word
 * (Вставить, Вручную) keeps the full segment width and never breaks mid-word.
 */
@Composable
private fun ModeSegment(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    val chrome = rememberChipChrome(selected)
    val reduceMotion = LocalReduceMotion.current
    val fade = if (reduceMotion) snap<Float>() else tween<Float>(160)
    Surface(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 48.dp)
            .semantics {
                role = Role.Tab
                this.selected = selected
            },
        shape = RoundedCornerShape(14.dp),
        color = chrome.container,
        contentColor = chrome.content,
        border = BorderStroke(1.dp, chrome.border),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            AnimatedContent(
                targetState = selected,
                transitionSpec = { fadeIn(fade) togetherWith fadeOut(fade) },
                label = "modeIcon",
            ) { on ->
                Icon(
                    imageVector = if (on) AppIcons.Check else icon,
                    contentDescription = null,
                    tint = chrome.content,
                    modifier = Modifier.size(18.dp),
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = chrome.content,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
