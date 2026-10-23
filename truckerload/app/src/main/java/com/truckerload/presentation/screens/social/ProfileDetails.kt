package com.truckerload.presentation.screens.social

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.domain.geo.CountryCatalog
import com.truckerload.domain.social.DriverStatus
import com.truckerload.domain.social.EnhancedDriverProfile
import com.truckerload.domain.social.TruckType
import com.truckerload.presentation.icons.AppIcons
import com.truckerload.presentation.theme.LocalBrandPalette
import com.truckerload.domain.units.DisplayUnits
import com.truckerload.domain.units.MeasurementUnits
import com.truckerload.presentation.utils.MoneyFormat
import kotlin.math.roundToInt
import java.util.Locale

private val TileShape = RoundedCornerShape(20.dp)

@Composable
internal fun ProfileTotals(loads: Int, miles: Int) {
    ProfileSectionHeader(stringResource(R.string.profile_totals_title))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TotalTile(
            icon = AppIcons.LocalShipping,
            value = MoneyFormat.formatNumber(loads.toDouble()),
            caption = stringResource(R.string.profile_loads_caption),
            modifier = Modifier.weight(1f),
        )
        TotalTile(
            icon = AppIcons.Route,
            value = MoneyFormat.formatDistanceNumber(miles.toDouble()),
            caption = stringResource(
                if (DisplayUnits.isMetric) R.string.profile_miles_caption_km else R.string.profile_miles_caption,
            ),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun TotalTile(
    icon: ImageVector,
    value: String,
    caption: String,
    modifier: Modifier = Modifier,
) {
    val brand = LocalBrandPalette.current
    Row(
        modifier = modifier
            .heightIn(min = 72.dp)
            .clip(TileShape)
            .background(brand.card)
            .border(1.dp, brand.cardBorder, TileShape)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(brand.container),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = brand.primary, modifier = Modifier.size(22.dp))
        }
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(
                text = value,
                color = brand.textPrimary,
                style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = caption,
                color = brand.textTertiary,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
internal fun ProfileFilledDetails(profile: EnhancedDriverProfile) {
    val truckFilled = profile.truckType != TruckType.OTHER || profile.experienceYears > 0
    val placeFilled = profile.homeState.isNotBlank() || profile.status != DriverStatus.OFFLINE
    val route = profile.currentRoute?.takeIf { it.isNotBlank() }
    if (truckFilled) {
        ProfileSectionHeader(stringResource(R.string.profile_truck_title))
        ProfileCard {
            DetailBlock(truckLine(profile))
        }
    }
    if (placeFilled || route != null || profile.ratingCount > 0) {
        ProfileSectionHeader(stringResource(R.string.social_territory))
        ProfileCard {
            if (placeFilled) DetailBlock(placeLine(profile))
            if (route != null) DetailBlock(route)
            if (profile.ratingCount > 0) {
                DetailBlock(
                    "${String.format(Locale.US, "%.1f", profile.rating)} (${profile.ratingCount}) · " +
                        "${profile.reputation} ${stringResource(R.string.social_reputation_short)}",
                )
            }
        }
    }
    if (profile.about.isNotBlank()) {
        ProfileSectionHeader(stringResource(R.string.about_me))
        ProfileCard { DetailBlock(profile.about) }
    }
    if (profile.preferredRoutes.isNotEmpty() || (profile.maxRadius > 0 && profile.maxRadius != 500)) {
        ProfileCard { TerritoryBody(profile) }
    }
}

@Composable
private fun truckLine(profile: EnhancedDriverProfile): String {
    val years = stringResource(R.string.experience_years)
    return buildList {
        if (profile.truckType != TruckType.OTHER) add("${profile.truckType.emoji} ${profile.truckType.label}")
        if (profile.experienceYears > 0) add("${profile.experienceYears} $years")
    }.joinToString(" · ")
}

@Composable
private fun placeLine(profile: EnhancedDriverProfile): String {
    val russian = LocalLocale.current.platformLocale.language.equals("ru", ignoreCase = true)
    val country = CountryCatalog.byIso2(profile.homeState)
        ?.let { "${it.displayName(russian)} (${it.iso2})" }
        ?: profile.homeState
    val status = if (profile.status == DriverStatus.OFFLINE) {
        ""
    } else {
        profile.status.label.substringAfter(' ', profile.status.label)
    }
    return listOf(country, status).filter { it.isNotBlank() }.joinToString(" · ")
}

@Composable
private fun TerritoryBody(profile: EnhancedDriverProfile) {
    val brand = LocalBrandPalette.current
    val russian = LocalLocale.current.platformLocale.language.equals("ru", ignoreCase = true)
    val country = CountryCatalog.byIso2(profile.homeState)
        ?.let { "${it.displayName(russian)} (${it.iso2})" }
        ?: profile.homeState
    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
        if (country.isNotBlank() && profile.preferredRoutes.isNotEmpty()) {
            Text(
                text = "${stringResource(R.string.home_country)}: $country",
                color = brand.textPrimary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (profile.preferredRoutes.isNotEmpty()) {
            Text(
                text = "${stringResource(R.string.social_favorite_routes)}: ${profile.preferredRoutes.joinToString(", ")}",
                color = brand.textSecondary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        if (profile.maxRadius > 0 && profile.maxRadius != 500) {
            Text(
                text = if (DisplayUnits.isMetric) {
                    val km = MeasurementUnits.displayDistance(profile.maxRadius.toDouble()).roundToInt()
                    "${stringResource(R.string.social_max_radius)}: $km km"
                } else {
                    "${stringResource(R.string.social_max_radius)}: ${profile.maxRadius} mi"
                },
                color = brand.textSecondary,
                style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun DetailBlock(text: String) {
    if (text.isBlank()) return
    Text(
        text = text,
        color = LocalBrandPalette.current.textPrimary,
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
    )
}
