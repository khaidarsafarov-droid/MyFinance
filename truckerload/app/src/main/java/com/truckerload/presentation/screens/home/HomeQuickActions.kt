package com.truckerload.presentation.screens.home

import com.truckerload.presentation.icons.AppIcons
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import com.truckerload.R
import com.truckerload.presentation.theme.pressScale
import com.truckerload.presentation.utils.adaptiveHorizontalPadding

@Composable
internal fun HomeQuickActionRow(
    onDiesel: () -> Unit,
    onPaycheck: () -> Unit,
    onExpense: () -> Unit,
    onScanner: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = adaptiveHorizontalPadding(), vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        QuickTile(AppIcons.LocalGasStation, stringResource(R.string.home_quick_diesel), onDiesel, Modifier.weight(1f))
        QuickTile(AppIcons.Payments, stringResource(R.string.home_quick_paycheck), onPaycheck, Modifier.weight(1f))
        QuickTile(AppIcons.Receipt, stringResource(R.string.home_quick_expense), onExpense, Modifier.weight(1f))
        QuickTile(AppIcons.DocumentScanner, stringResource(R.string.home_quick_scanner), onScanner, Modifier.weight(1f))
    }
}

@Composable
private fun QuickTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Column(
            modifier = Modifier
                .size(64.dp)
                .pressScale()
                .clip(RoundedCornerShape(22.dp))
                .background(cs.surface)
                .clickable(role = Role.Button, onClick = onClick),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = cs.primary,
                modifier = Modifier.size(26.dp),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = cs.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
