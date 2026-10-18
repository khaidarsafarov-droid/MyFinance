package com.truckerload.presentation.screens.add

import com.truckerload.presentation.icons.AppIcons

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import com.truckerload.presentation.theme.AppChipLabel
import com.truckerload.presentation.theme.AppFilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import com.truckerload.presentation.components.blurGate
import com.truckerload.presentation.components.errorSupport
import com.truckerload.presentation.components.formKeyboard
import com.truckerload.presentation.components.formKeyboardActions
import com.truckerload.presentation.components.formatRpm
import com.truckerload.presentation.components.rememberBlurGate
import com.truckerload.presentation.components.RpmTier
import com.truckerload.presentation.components.rpmTier
import com.truckerload.presentation.components.rpmTierWord
import com.truckerload.utils.FieldErrors
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.truckerload.R
import com.truckerload.domain.model.EquipmentType
import com.truckerload.presentation.components.TlOutlinedButton as OutlinedButton
import com.truckerload.presentation.di.LocalRpmThresholdsStore
import com.truckerload.presentation.theme.AppTextFieldDefaults
import com.truckerload.presentation.theme.BentoGlassCard
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.LocalReduceMotion
import com.truckerload.presentation.theme.LocalSemanticColors
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.utils.dateStringToUtcDatePickerMillis
import com.truckerload.utils.getYesterdayDate
import com.truckerload.utils.utcDatePickerMillisToDateString
import java.util.Calendar

@Composable
fun AddLoadManualForm(
    fields: ManualLoadFields,
    onTripId: (String) -> Unit,
    onDate: (String) -> Unit,
    onRate: (String) -> Unit,
    onMiles: (String) -> Unit,
    onPointChange: (Int, String) -> Unit,
    onAddPoint: () -> Unit,
    onRemovePoint: (Int) -> Unit,
    modifier: Modifier = Modifier,
    hintRes: Int = R.string.add_load_manual_hint,
    showFieldErrors: Boolean = false,
    fieldErrorNonce: Int = 0,
    onSubmit: () -> Unit = {},
    equipment: EquipmentType? = null,
    onEquipment: (EquipmentType?) -> Unit = {},
    showPasteBanner: Boolean = false,
    onPaste: () -> Unit = {},
    onOpenDocuments: (() -> Unit)? = null,
) {
    val tc = LocalTruckColors.current
    val cs = MaterialTheme.colorScheme
    val colors = AppTextFieldDefaults.outlined()
    val rateGate = rememberBlurGate()
    val milesGate = rememberBlurGate()
    val rateFocus = remember { FocusRequester() }
    val milesFocus = remember { FocusRequester() }
    val rateError = FieldErrors.amountRes(fields.rate)
    val milesError = FieldErrors.milesRes(fields.miles)
    val showRate = rateGate.visibleRes(rateError, showFieldErrors)
    val showMiles = milesGate.visibleRes(milesError, showFieldErrors)
    var moreOpen by rememberSaveable { mutableStateOf(false) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    val today = remember { isoToday() }
    val yesterday = remember { getYesterdayDate() }
    val reduceMotion = LocalReduceMotion.current
    LaunchedEffect(fieldErrorNonce) {
        if (fieldErrorNonce == 0) return@LaunchedEffect
        val target = when {
            rateError != null -> rateFocus
            milesError != null -> milesFocus
            else -> null
        }
        target?.let { runCatching { it.requestFocus() } }
    }
    if (showDatePicker) {
        JournalDatePickerDialog(
            recordedAtMillis = System.currentTimeMillis(),
            initialDateMillis = dateStringToUtcDatePickerMillis(fields.date.ifBlank { today })
                ?: System.currentTimeMillis(),
            onDismiss = { showDatePicker = false },
            onConfirm = { millis -> onDate(utcDatePickerMillisToDateString(millis)) },
        )
    }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (showPasteBanner) {
            PasteBanner(onPaste = onPaste)
        }
        if (hintRes != R.string.add_load_manual_hint) {
            Text(
                text = stringResource(hintRes),
                style = MaterialTheme.typography.bodySmall,
                color = tc.TextSecondary,
            )
        }
        SectionLabel(stringResource(R.string.add_load_route_label))
        val points = fields.allPoints()
        points.forEachIndexed { index, value ->
            val letter = ManualLoadFields.pointLetter(index)
            val lastPoint = index == points.lastIndex
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = value,
                    onValueChange = { onPointChange(index, it) },
                    label = { Text(stringResource(R.string.add_load_point_n, letter)) },
                    placeholder = { Text(pointPlaceholder(index)) },
                    leadingIcon = {
                        Icon(AppIcons.Place, contentDescription = null, tint = cs.primary)
                    },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = formKeyboard(KeyboardType.Text, done = lastPoint),
                    keyboardActions = formKeyboardActions(
                        onDone = if (lastPoint && rateError == null) onSubmit else null,
                    ),
                    shape = RoundedCornerShape(BentoGlassTheme.CellRadius),
                    colors = colors,
                )
                if (index >= ManualLoadFields.MIN_ROUTE_POINTS) {
                    IconButton(
                        onClick = { onRemovePoint(index) },
                        modifier = Modifier.height(48.dp),
                    ) {
                        Icon(
                            AppIcons.Close,
                            contentDescription = stringResource(R.string.add_load_remove_point, letter),
                            tint = tc.TextSecondary,
                        )
                    }
                }
            }
        }
        fields.nextPointLetter()?.let { nextLetter ->
            OutlinedButton(
                onClick = onAddPoint,
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                Icon(AppIcons.Add, contentDescription = null)
                Text(
                    stringResource(R.string.add_load_add_point, nextLetter),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
        SectionLabel(stringResource(R.string.add_load_pay_label))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = fields.rate,
                onValueChange = onRate,
                label = { Text(stringResource(R.string.edit_load_total_rate)) },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(rateFocus)
                    .blurGate(rateGate),
                singleLine = true,
                isError = showRate != null,
                supportingText = errorSupport(showRate),
                keyboardOptions = formKeyboard(KeyboardType.Decimal),
                keyboardActions = formKeyboardActions(),
                shape = RoundedCornerShape(BentoGlassTheme.CellRadius),
                colors = colors,
            )
            OutlinedTextField(
                value = fields.miles,
                onValueChange = onMiles,
                label = { Text(stringResource(R.string.edit_load_total_miles)) },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(milesFocus)
                    .blurGate(milesGate),
                singleLine = true,
                isError = showMiles != null,
                supportingText = errorSupport(showMiles),
                keyboardOptions = formKeyboard(KeyboardType.Decimal),
                keyboardActions = formKeyboardActions(),
                shape = RoundedCornerShape(BentoGlassTheme.CellRadius),
                colors = colors,
            )
        }
        LiveRpmRow(rate = fields.parsedRate(), miles = fields.parsedMiles())
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AppFilterChip(
                selected = fields.date.take(10) == today,
                onClick = { onDate(today) },
                label = { AppChipLabel(stringResource(R.string.filter_today)) },
            )
            AppFilterChip(
                selected = fields.date.take(10) == yesterday,
                onClick = { onDate(yesterday) },
                label = { AppChipLabel(stringResource(R.string.add_load_date_yesterday)) },
                leadingIcon = {
                    Icon(
                        AppIcons.History,
                        contentDescription = null,
                        tint = com.truckerload.presentation.theme.LocalBrandPalette.current.iconBlue,
                        modifier = Modifier.size(18.dp),
                    )
                },
            )
            AppFilterChip(
                selected = fields.date.isNotBlank() &&
                    fields.date.take(10) != today &&
                    fields.date.take(10) != yesterday,
                onClick = { showDatePicker = true },
                label = { AppChipLabel(stringResource(R.string.add_load_date_pick)) },
                leadingIcon = {
                    Icon(
                        AppIcons.DateRange,
                        contentDescription = null,
                        tint = com.truckerload.presentation.theme.LocalBrandPalette.current.iconBlue,
                        modifier = Modifier.size(18.dp),
                    )
                },
                trailingIcon = {
                    Icon(
                        AppIcons.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                },
            )
        }
        BentoGlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (reduceMotion) Modifier else Modifier.animateContentSize())
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.add_load_more_details),
                        style = MaterialTheme.typography.titleSmall,
                        color = cs.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = stringResource(R.string.add_load_optional_tag),
                        style = MaterialTheme.typography.labelMedium,
                        color = cs.onSurfaceVariant,
                        modifier = Modifier.padding(end = 4.dp),
                    )
                    IconButton(onClick = { moreOpen = !moreOpen }) {
                        Icon(
                            if (moreOpen) AppIcons.ExpandLess else AppIcons.ExpandMore,
                            contentDescription = stringResource(R.string.add_load_more_details),
                        )
                    }
                }
                if (moreOpen) {
                    OutlinedTextField(
                        value = fields.tripId,
                        onValueChange = onTripId,
                        label = { Text(stringResource(R.string.edit_load_trip_id)) },
                        placeholder = { Text(stringResource(R.string.add_load_trip_optional)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = formKeyboard(KeyboardType.Text),
                        keyboardActions = formKeyboardActions(),
                        shape = RoundedCornerShape(BentoGlassTheme.CellRadius),
                        colors = colors,
                    )
                    EquipmentTypeChipRow(
                        selected = equipment,
                        onSelect = onEquipment,
                        title = null,
                    )
                    if (onOpenDocuments != null) {
                        AppFilterChip(
                            selected = false,
                            onClick = onOpenDocuments,
                            label = { AppChipLabel(stringResource(R.string.add_load_mode_document)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PasteBanner(onPaste: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = cs.primaryContainer.copy(alpha = 0.45f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(AppIcons.Send, contentDescription = null, tint = cs.primary)
            Text(
                text = stringResource(R.string.add_load_paste_banner),
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurface,
                modifier = Modifier.weight(1f),
            )
            Surface(
                onClick = onPaste,
                shape = RoundedCornerShape(16.dp),
                color = cs.primary,
                contentColor = cs.onPrimary,
            ) {
                Text(
                    text = stringResource(R.string.add_load_paste_action),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun LiveRpmRow(rate: Double?, miles: Double) {
    val cs = MaterialTheme.colorScheme
    val semantic = LocalSemanticColors.current
    val thresholds by LocalRpmThresholdsStore.current.thresholds.collectAsStateWithLifecycle()
    val rpm = if (rate != null && rate > 0.0 && miles > 0.0) rate / miles else null
    val tier = rpmTier(rpm, thresholds.minProfit, thresholds.targetProfit)
    val rpmText = if (rpm != null) {
        formatRpm(rate ?: 0.0, miles, stringResource(R.string.rpm_per_mile_format))
    } else {
        "—"
    }
    val word = tier?.let { rpmTierWord(it) }
    val (bg, fg) = when (tier) {
        null -> semantic.successContainer.copy(alpha = 0.35f) to cs.onSurface
        RpmTier.Low -> semantic.dangerContainer to semantic.danger
        RpmTier.Ok -> semantic.warningContainer to semantic.warning
        RpmTier.Good -> semantic.successContainer to semantic.success
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = bg,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.add_load_rate_per_mile),
                style = MaterialTheme.typography.bodyMedium,
                color = fg,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = if (word != null) {
                    stringResource(R.string.add_load_rpm_live, rpmText, word)
                } else {
                    rpmText
                },
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFeatureSettings = "tnum",
                ),
                color = fg,
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun pointPlaceholder(index: Int): String = stringResource(
    when (index) {
        0 -> R.string.add_load_point_a_placeholder
        1 -> R.string.add_load_point_b_placeholder
        else -> R.string.add_load_point_more_placeholder
    },
)

private fun isoToday(): String {
    val cal = Calendar.getInstance()
    return "%04d-%02d-%02d".format(
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH) + 1,
        cal.get(Calendar.DAY_OF_MONTH),
    )
}

internal fun applyPastedPreview(
    viewModel: AddLoadViewModel,
    load: com.truckerload.domain.model.Load,
) {
    viewModel.setManualTripId(load.tripId)
    if (load.date.length >= 10) viewModel.setManualDate(load.date.take(10))
    if (load.totalRate.isPositive()) viewModel.setManualRate(load.totalRate.formatPlain())
    if (load.totalMiles > 0.0) {
        viewModel.setManualMiles(
            if (load.totalMiles % 1.0 == 0.0) {
                load.totalMiles.toInt().toString()
            } else {
                String.format(java.util.Locale.US, "%.1f", load.totalMiles)
            },
        )
    }
    load.equipmentType?.let(viewModel::setEquipmentType)
    while (viewModel.uiState.value.manual.extraPoints.isNotEmpty()) {
        viewModel.removeManualPoint(ManualLoadFields.MIN_ROUTE_POINTS)
    }
    val addresses = load.stops.map { stop ->
        stop.fullAddress.ifBlank {
            listOf(stop.city, stop.state).filter { it.isNotBlank() }.joinToString(", ")
        }
    }.filter { it.isNotBlank() }.ifEmpty {
        listOf(load.pointA, load.pointB).filter { it.isNotBlank() }
    }
    viewModel.setManualPoint(0, addresses.getOrElse(0) { "" })
    viewModel.setManualPoint(1, addresses.getOrElse(1) { "" })
    addresses.drop(2).forEach { address ->
        viewModel.addManualPoint()
        val index = viewModel.uiState.value.manual.allPoints().lastIndex
        viewModel.setManualPoint(index, address)
    }
}
