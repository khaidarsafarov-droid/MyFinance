package com.truckerload.presentation.screens.social

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.truckerload.R
import com.truckerload.presentation.icons.AppIcons
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.theme.UiDimens
import java.io.File

@Composable
fun ProfileAvatar(
    avatarUrl: String?,
    modifier: Modifier = Modifier,
) {
    val tc = LocalTruckColors.current
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

    Box(
        modifier = modifier
            .size(UiDimens.AvatarProfile)
            .clip(CircleShape)
            .background(tc.AccentPrimary.copy(alpha = 0.25f)),
        contentAlignment = Alignment.Center,
    ) {
        if (imageRequest != null) {
            SubcomposeAsyncImage(
                model = imageRequest,
                contentDescription = stringResource(R.string.profile_photo),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds,
                loading = {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 2.dp,
                        color = tc.AccentPrimary,
                    )
                },
                error = {
                    Icon(
                        imageVector = AppIcons.Person,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )
        } else {
            Icon(
                imageVector = AppIcons.Person,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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
