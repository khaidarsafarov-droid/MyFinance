package com.truckerload.presentation.screens.social

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.truckerload.R
import com.truckerload.presentation.icons.AppIcons
import com.truckerload.presentation.theme.LocalBrandPalette
import com.truckerload.presentation.theme.UiDimens
import java.io.File

@Composable
fun ProfileAvatar(
    avatarUrl: String?,
    initials: String? = null,
    onHero: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val brand = LocalBrandPalette.current
    val context = LocalContext.current
    val model = remember(avatarUrl) { resolveAvatarModel(avatarUrl) }
    val imageRequest = remember(model) {
        model?.let {
            ImageRequest.Builder(context)
                .data(it)
                .crossfade(true)
                .build()
        }
    }
    val hasInitials = !initials.isNullOrBlank()
    val disc = when {
        hasInitials -> Color.White
        onHero -> Color.White.copy(alpha = 0.16f)
        else -> brand.container
    }
    val mark = if (hasInitials) brand.labelOnWhite() else brand.primary

    Box(
        modifier = modifier
            .size(UiDimens.AvatarProfile)
            .clip(CircleShape)
            .background(disc),
        contentAlignment = Alignment.Center,
    ) {
        if (imageRequest != null) {
            SubcomposeAsyncImage(
                model = imageRequest,
                contentDescription = stringResource(R.string.profile_photo),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                loading = {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 2.dp,
                        color = brand.primary,
                    )
                },
                error = { AvatarFallback(initials, mark, onHero) },
            )
        } else {
            AvatarFallback(initials, mark, onHero)
        }
    }
}

@Composable
private fun AvatarFallback(initials: String?, mark: Color, onHero: Boolean) {
    if (!initials.isNullOrBlank()) {
        Text(
            text = initials,
            color = mark,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
    } else {
        Icon(
            imageVector = AppIcons.Person,
            contentDescription = null,
            modifier = Modifier.size(32.dp),
            tint = if (onHero) Color.White.copy(alpha = 0.9f) else mark,
        )
    }
}

private fun resolveAvatarModel(avatarUrl: String?): Any? {
    if (avatarUrl.isNullOrBlank()) return null
    return if (avatarUrl.startsWith("http://") || avatarUrl.startsWith("https://") ||
        avatarUrl.startsWith("file://") || avatarUrl.startsWith("content://")
    ) {
        avatarUrl
    } else {
        File(avatarUrl).takeIf { it.exists() }
    }
}
