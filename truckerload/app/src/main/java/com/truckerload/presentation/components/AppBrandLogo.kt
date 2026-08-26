package com.truckerload.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.truckerload.R

/** Brand navy behind the shield so Fit letterboxing matches the mark. */
private val BrandNavy = Color(0xFF143882)

/**
 * Brand mark (shield: loads + diesel + money).
 *
 * Uses [ContentScale.Fit] inside a navy rounded plate so the shield stays
 * centered and the tip is not clipped by the corner radius.
 */
@Composable
fun AppBrandLogo(
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    cornerRadius: Dp = 20.dp,
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(BrandNavy),
        contentAlignment = Alignment.Center,
    ) {
        Image(
            painter = painterResource(R.drawable.app_logo),
            contentDescription = stringResource(R.string.home_brand_title),
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(size)
                .padding(size * 0.04f),
        )
    }
}
