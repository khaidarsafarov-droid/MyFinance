package com.truckerload.presentation.startup

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.presentation.theme.SoftUiColors

/**
 * Midnight & Gold cold start. Shown until [com.truckerload.data.local.DatabaseStartup]
 * leaves Loading, and again when the journal open fails.
 */
@Composable
fun DatabaseStartupLoading() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(midnightBrush())
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(color = SoftUiColors.ForestAccentDark)
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.db_startup_loading),
            color = SoftUiColors.HeroTextDark,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun DatabaseStartupFailed(
    recoverable: Boolean,
    onRetry: () -> Unit,
    onRecover: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(midnightBrush())
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(R.string.db_startup_failed_title),
            color = SoftUiColors.HeroTextDark,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.db_startup_failed_body),
            color = SoftUiColors.HeroTextMutedDark,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(28.dp))
        if (recoverable) {
            GoldButton(
                label = stringResource(R.string.db_startup_retry),
                onClick = onRetry,
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
        GoldButton(
            label = stringResource(R.string.db_startup_recover),
            onClick = onRecover,
        )
    }
}

@Composable
private fun GoldButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = SoftUiColors.ForestAccentDark,
            contentColor = SoftUiColors.OnForestAccentDark,
        ),
    ) {
        Text(text = label)
    }
}

private fun midnightBrush(): Brush = Brush.verticalGradient(
    listOf(SoftUiColors.HeroStartDark, SoftUiColors.HeroEndDark),
)
