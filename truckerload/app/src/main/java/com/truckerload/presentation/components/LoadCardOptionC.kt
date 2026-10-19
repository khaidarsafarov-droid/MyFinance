package com.truckerload.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.truckerload.R
import com.truckerload.domain.model.Load
import com.truckerload.domain.model.Stop
import com.truckerload.presentation.icons.AppIcons
import com.truckerload.presentation.theme.BrandPalette
import com.truckerload.presentation.theme.LocalBrandPalette
import com.truckerload.presentation.utils.MoneyFormat

enum class LoadSourceTag { RELAY, MANUAL, BROKER }

/** Best-effort source from fields we already store. No schema change. */
internal fun Load.journalSource(): LoadSourceTag {
    val raw = rawMessage
    if (raw.contains("Amazon Relay", ignoreCase = true) || raw.contains("PU#")) {
        return LoadSourceTag.RELAY
    }
    if (tripId.startsWith("M-")) return LoadSourceTag.MANUAL
    val relayHits = listOf("Trip ID:", "Total Rate:", "Pu-address:", "Del-address:")
        .count { raw.contains(it, ignoreCase = true) }
    if (relayHits >= 2) return LoadSourceTag.RELAY
    if (raw.isBlank()) return LoadSourceTag.MANUAL
    return LoadSourceTag.BROKER
}

@Composable
internal fun LoadCardOptionC(
    card: LoadCardModel,
    onCameraClick: (() -> Unit)?,
    onScanClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val brand = LocalBrandPalette.current
    val rpmText = formatRpm(
        card.totalRate,
        card.drivenMiles,
        stringResource(R.string.rpm_per_mile_format),
    )
    val trip = card.tripId.let { id ->
        when {
            id.isBlank() -> ""
            id.startsWith("#") -> id
            else -> "#$id"
        }
    }
    val dateLabel = formatJournalDate(card.headerDate)
    val header = buildString {
        append(dateLabel)
        if (trip.isNotBlank()) {
            append(" · ")
            append(trip)
        }
    }
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(brand.heroStart, brand.heroEnd)))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = header,
                color = brand.heroText,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (card.rpmTier != null && card.rpm != null) {
                RpmValueBadge(rpmText = rpmText, tier = card.rpmTier)
            }
        }
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (card.isDispute) {
                DisputeCardChip(hadDispute = card.hadDispute)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = MoneyFormat.formatCurrency(card.totalRate, decimals = 0),
                        color = brand.textPrimary,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontFeatureSettings = "tnum",
                            fontSize = 32.sp,
                            lineHeight = 36.sp,
                        ),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (card.pace.isPositive()) {
                        Text(
                            text = stringResource(
                                R.string.load_card_per_day,
                                MoneyFormat.formatCurrency(card.pace, decimals = 2),
                            ),
                            color = brand.textSecondary,
                            style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (onCameraClick != null || onScanClick != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (onCameraClick != null) {
                            TonalCardAction(
                                onClick = onCameraClick,
                                icon = AppIcons.CameraAlt,
                                contentDescription = stringResource(R.string.load_card_attach_photo),
                                brand = brand,
                            )
                        }
                        if (onScanClick != null) {
                            TonalCardAction(
                                onClick = onScanClick,
                                icon = AppIcons.DocumentScanner,
                                contentDescription = stringResource(R.string.load_card_attach_scan),
                                brand = brand,
                            )
                        }
                    }
                }
            }
            RoutePair(
                originCity = card.originCity,
                originStreet = card.originStreet,
                destCity = card.destCity,
                destStreet = card.destStreet,
                brand = brand,
            )
            FactsRow(
                miles = MoneyFormat.formatNumber(card.drivenMiles),
                stops = card.stopCount,
                days = card.durationDays,
                source = card.source,
                brand = brand,
            )
        }
    }
}

@Composable
private fun RoutePair(
    originCity: String,
    originStreet: String,
    destCity: String,
    destStreet: String,
    brand: BrandPalette,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            RouteMark(filled = false, color = brand.primary)
            Box(
                modifier = Modifier
                    .padding(vertical = 2.dp)
                    .width(2.dp)
                    .height(28.dp)
                    .background(brand.primary.copy(alpha = 0.35f)),
            )
            RouteMark(filled = true, color = brand.primary)
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            CityLine(originCity, brand)
            StreetLine(originStreet, brand)
            CityLine(destCity, brand)
            StreetLine(destStreet, brand)
        }
        Icon(
            imageVector = AppIcons.ChevronRight,
            contentDescription = null,
            tint = brand.textTertiary,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun CityLine(text: String, brand: BrandPalette) {
    Text(
        text = text,
        color = brand.textPrimary,
        fontWeight = FontWeight.SemiBold,
        style = MaterialTheme.typography.titleSmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun StreetLine(text: String, brand: BrandPalette) {
    if (text.isBlank()) return
    Text(
        text = text,
        color = brand.textSecondary,
        style = MaterialTheme.typography.bodySmall,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun RouteMark(filled: Boolean, color: Color) {
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .then(
                if (filled) {
                    Modifier.background(color)
                } else {
                    Modifier.border(2.dp, color, CircleShape)
                },
            ),
    )
}

@Composable
private fun FactsRow(
    miles: String,
    stops: Int,
    days: Int,
    source: LoadSourceTag,
    brand: BrandPalette,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Fact(
            icon = AppIcons.Route,
            tint = brand.iconViolet,
            text = stringResource(R.string.load_card_miles, miles),
            brand = brand,
        )
        if (stops > 0) {
            Fact(
                icon = AppIcons.Place,
                tint = brand.orange,
                text = pluralStringResource(R.plurals.load_card_stops, stops, stops),
                brand = brand,
            )
        }
        if (days > 0) {
            Fact(
                icon = AppIcons.Schedule,
                tint = brand.iconBlue,
                text = pluralStringResource(R.plurals.load_card_duration_days, days, days),
                brand = brand,
            )
        }
        Box(modifier = Modifier.weight(1f))
        SourceTag(source, brand)
    }
}

@Composable
private fun Fact(icon: ImageVector, tint: Color, text: String, brand: BrandPalette) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        Text(
            text = text,
            color = brand.textSecondary,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SourceTag(source: LoadSourceTag, brand: BrandPalette) {
    val (label, fg, bg) = when (source) {
        LoadSourceTag.RELAY -> Triple(
            stringResource(R.string.load_source_relay),
            brand.tagRelayFg,
            brand.tagRelayBg,
        )
        LoadSourceTag.MANUAL -> Triple(
            stringResource(R.string.load_source_manual),
            brand.tagManualFg,
            brand.tagManualBg,
        )
        LoadSourceTag.BROKER -> Triple(
            stringResource(R.string.load_source_broker),
            brand.tagBrokerFg,
            brand.tagBrokerBg,
        )
    }
    Text(
        text = label,
        color = fg,
        fontWeight = FontWeight.Bold,
        style = MaterialTheme.typography.labelSmall,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

/** 40dp visual, 48dp touch, tonal container. */
@Composable
private fun TonalCardAction(
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    brand: BrandPalette,
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .minimumInteractiveComponentSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(brand.container),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = brand.onContainer,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

internal fun routeStreet(stop: Stop?, cityState: String): String {
    if (stop == null) return ""
    val full = stop.fullAddress.trim()
    if (full.isBlank()) return ""
    val city = stop.city.trim()
    val state = stop.state.trim()
    val suffixes = listOf(
        ", $city, $state",
        ", $cityState",
        " $city, $state",
    ).filter { it.isNotBlank() && it != ", " && it != "," }
    var street = full
    for (suffix in suffixes) {
        if (street.endsWith(suffix, ignoreCase = true)) {
            street = street.dropLast(suffix.length).trim().trimEnd(',')
            break
        }
    }
    if (street.equals(cityState, ignoreCase = true)) return ""
    if (city.isNotBlank() && street.equals(city, ignoreCase = true)) return ""
    if (city.isNotBlank() && state.isNotBlank() &&
        street.equals("$city, $state", ignoreCase = true)
    ) {
        return ""
    }
    return street
}
