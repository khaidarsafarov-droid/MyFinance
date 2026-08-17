package com.truckerload.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.presentation.components.TlButton
import com.truckerload.presentation.theme.AppSwitchDefaults
import com.truckerload.presentation.theme.SoftUiColors
import com.truckerload.presentation.theme.SoftUiElevation
import com.truckerload.presentation.theme.SoftUiShapes
import com.truckerload.presentation.utils.isTablet

@Composable
fun WidgetConfigureScreen(
    sizeMode: WidgetSizeMode,
    showGross: Boolean,
    showPace: Boolean,
    showGoal: Boolean,
    themeMode: WidgetThemeMode,
    onSizeModeChange: (WidgetSizeMode) -> Unit,
    onShowGrossChange: (Boolean) -> Unit,
    onShowPaceChange: (Boolean) -> Unit,
    onShowGoalChange: (Boolean) -> Unit,
    onThemeModeChange: (WidgetThemeMode) -> Unit,
    onSave: () -> Unit,
) {
    val tablet = isTablet()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SoftUiColors.ShellBg)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = if (tablet) 28.dp else 20.dp)
                .padding(top = if (tablet) 24.dp else 16.dp),
        ) {
            Text(
                text = stringResource(R.string.widget_configure_title),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = SoftUiColors.ForestPrimary,
            )
            Text(
                text = stringResource(R.string.widget_configure_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = SoftUiColors.ForestMuted,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
            )

            if (tablet) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    PreviewPane(
                        sizeMode = sizeMode,
                        showGross = showGross,
                        showPace = showPace,
                        showGoal = showGoal,
                        themeMode = themeMode,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                    OptionsPane(
                        sizeMode = sizeMode,
                        showGross = showGross,
                        showPace = showPace,
                        showGoal = showGoal,
                        themeMode = themeMode,
                        onSizeModeChange = onSizeModeChange,
                        onShowGrossChange = onShowGrossChange,
                        onShowPaceChange = onShowPaceChange,
                        onShowGoalChange = onShowGoalChange,
                        onThemeModeChange = onThemeModeChange,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    PreviewPane(
                        sizeMode = sizeMode,
                        showGross = showGross,
                        showPace = showPace,
                        showGoal = showGoal,
                        themeMode = themeMode,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OptionsContent(
                        sizeMode = sizeMode,
                        showGross = showGross,
                        showPace = showPace,
                        showGoal = showGoal,
                        themeMode = themeMode,
                        onSizeModeChange = onSizeModeChange,
                        onShowGrossChange = onShowGrossChange,
                        onShowPaceChange = onShowPaceChange,
                        onShowGoalChange = onShowGoalChange,
                        onThemeModeChange = onThemeModeChange,
                    )
                }
            }
        }

        TlButton(
            onClick = onSave,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (tablet) 28.dp else 20.dp)
                .padding(bottom = 16.dp, top = 8.dp),
        ) {
            Text(
                text = stringResource(R.string.widget_configure_save),
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun PreviewPane(
    sizeMode: WidgetSizeMode,
    showGross: Boolean,
    showPace: Boolean,
    showGoal: Boolean,
    themeMode: WidgetThemeMode,
    modifier: Modifier = Modifier,
) {
    SoftSectionCard(modifier = modifier) {
        Text(
            text = stringResource(R.string.widget_configure_preview),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = SoftUiColors.ForestMuted,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 160.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(SoftUiColors.OuterBg)
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            WidgetLivePreview(
                sizeMode = sizeMode,
                showGross = showGross,
                showPace = showPace,
                showGoal = showGoal,
                themeMode = themeMode,
            )
        }
    }
}

@Composable
private fun OptionsPane(
    sizeMode: WidgetSizeMode,
    showGross: Boolean,
    showPace: Boolean,
    showGoal: Boolean,
    themeMode: WidgetThemeMode,
    onSizeModeChange: (WidgetSizeMode) -> Unit,
    onShowGrossChange: (Boolean) -> Unit,
    onShowPaceChange: (Boolean) -> Unit,
    onShowGoalChange: (Boolean) -> Unit,
    onThemeModeChange: (WidgetThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        OptionsContent(
            sizeMode = sizeMode,
            showGross = showGross,
            showPace = showPace,
            showGoal = showGoal,
            themeMode = themeMode,
            onSizeModeChange = onSizeModeChange,
            onShowGrossChange = onShowGrossChange,
            onShowPaceChange = onShowPaceChange,
            onShowGoalChange = onShowGoalChange,
            onThemeModeChange = onThemeModeChange,
        )
    }
}

@Composable
private fun OptionsContent(
    sizeMode: WidgetSizeMode,
    showGross: Boolean,
    showPace: Boolean,
    showGoal: Boolean,
    themeMode: WidgetThemeMode,
    onSizeModeChange: (WidgetSizeMode) -> Unit,
    onShowGrossChange: (Boolean) -> Unit,
    onShowPaceChange: (Boolean) -> Unit,
    onShowGoalChange: (Boolean) -> Unit,
    onThemeModeChange: (WidgetThemeMode) -> Unit,
) {
    SoftSectionCard {
        SectionLabel(stringResource(R.string.widget_configure_size))
        SizeGrid(
            selected = sizeMode,
            onSelect = onSizeModeChange,
            modifier = Modifier.padding(top = 10.dp),
        )
    }
    SoftSectionCard {
        SectionLabel(stringResource(R.string.widget_configure_theme))
        ThemeRow(
            selected = themeMode,
            onSelect = onThemeModeChange,
            modifier = Modifier.padding(top = 10.dp),
        )
    }
    SoftSectionCard {
        SectionLabel(stringResource(R.string.widget_configure_metrics))
        MetricToggleRow(
            label = stringResource(R.string.widget_configure_show_gross),
            checked = showGross,
            onCheckedChange = onShowGrossChange,
        )
        SoftDivider()
        MetricToggleRow(
            label = stringResource(R.string.widget_configure_show_pace),
            checked = showPace,
            onCheckedChange = onShowPaceChange,
        )
        SoftDivider()
        MetricToggleRow(
            label = stringResource(R.string.widget_configure_show_goal),
            checked = showGoal,
            onCheckedChange = onShowGoalChange,
        )
    }
}

@Composable
private fun SoftSectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = SoftUiElevation.Card,
                shape = SoftUiShapes.Card,
                ambientColor = SoftUiColors.ShadowTint,
                spotColor = SoftUiColors.ShadowNeutral,
            )
            .clip(SoftUiShapes.Card)
            .background(SoftUiColors.SurfaceLight)
            .padding(16.dp),
        content = content,
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        color = SoftUiColors.ForestPrimary,
        modifier = Modifier.padding(bottom = 4.dp),
    )
}

@Composable
private fun SoftDivider() {
    HorizontalDivider(
        color = SoftUiColors.CardBorder,
        thickness = 1.dp,
        modifier = Modifier.padding(vertical = 2.dp),
    )
}

@Composable
private fun SizeGrid(
    selected: WidgetSizeMode,
    onSelect: (WidgetSizeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = listOf(
        Triple(WidgetSizeMode.AUTO, R.string.widget_configure_size_auto, R.string.widget_configure_size_auto_hint),
        Triple(WidgetSizeMode.SMALL, R.string.widget_configure_size_small, R.string.widget_configure_size_small_hint),
        Triple(WidgetSizeMode.MEDIUM, R.string.widget_configure_size_medium, R.string.widget_configure_size_medium_hint),
        Triple(WidgetSizeMode.LARGE, R.string.widget_configure_size_large, R.string.widget_configure_size_large_hint),
    )
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { (mode, titleRes, hintRes) ->
                    ChoiceTile(
                        title = stringResource(titleRes),
                        hint = stringResource(hintRes),
                        selected = selected == mode,
                        onClick = { onSelect(mode) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (row.size == 1) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ThemeRow(
    selected: WidgetThemeMode,
    onSelect: (WidgetThemeMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf(
            WidgetThemeMode.SYSTEM to R.string.widget_configure_theme_system,
            WidgetThemeMode.LIGHT to R.string.widget_configure_theme_light,
            WidgetThemeMode.DARK to R.string.widget_configure_theme_dark,
        ).forEach { (mode, labelRes) ->
            ChoiceTile(
                title = stringResource(labelRes),
                hint = null,
                selected = selected == mode,
                onClick = { onSelect(mode) },
                modifier = Modifier.weight(1f),
                minHeight = 48.dp,
            )
        }
    }
}

@Composable
private fun ChoiceTile(
    title: String,
    hint: String?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    minHeight: Dp = 64.dp,
) {
    val bg = if (selected) SoftUiColors.Sage else SoftUiColors.ContentBg
    val border = if (selected) SoftUiColors.ForestAccent else SoftUiColors.SageBorder
    val titleColor = if (selected) SoftUiColors.ForestPrimary else SoftUiColors.ForestMuted
    Column(
        modifier = modifier
            .heightIn(min = minHeight)
            .clip(SoftUiShapes.Chip)
            .background(bg)
            .border(width = 1.5.dp, color = border, shape = SoftUiShapes.Chip)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = titleColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (!hint.isNullOrBlank()) {
            Text(
                text = hint,
                style = MaterialTheme.typography.labelSmall,
                color = SoftUiColors.ForestMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}

@Composable
private fun MetricToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = SoftUiColors.ForestPrimary,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = AppSwitchDefaults.colors(),
        )
    }
}
