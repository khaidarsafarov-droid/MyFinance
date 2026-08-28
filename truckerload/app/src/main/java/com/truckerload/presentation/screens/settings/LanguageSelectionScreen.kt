package com.truckerload.presentation.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.data.preferences.AppLanguage
import com.truckerload.presentation.components.SoftAppPageScaffold
import com.truckerload.presentation.di.LocalSettingsDataStore
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.theme.TruckerLoadTheme
import com.truckerload.presentation.utils.adaptiveHorizontalPadding
import com.truckerload.presentation.utils.useNavigationRail
import com.truckerload.utils.AppLanguageManager
import com.truckerload.utils.LanguageItem
import com.truckerload.widget.WidgetRefresh
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSelectionScreen(
    onBack: () -> Unit,
    showBack: Boolean = true,
) {
    val settingsDataStore = LocalSettingsDataStore.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val tabletChrome = useNavigationRail()

    LanguageSelectionContent(
        selectedCode = AppLanguageManager.getCurrentLanguageCode(),
        showBack = showBack && !tabletChrome,
        onBack = onBack,
        onSelect = { item ->
            if (item.code == AppLanguageManager.getCurrentLanguageCode()) return@LanguageSelectionContent
            scope.launch {
                settingsDataStore.saveLanguage(AppLanguage.fromTag(item.code))
                AppLanguageManager.setLanguage(item.code)
                WidgetRefresh.refreshAndUpdateAsync(context)
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LanguageSelectionContent(
    selectedCode: String,
    onSelect: (LanguageItem) -> Unit,
    onBack: () -> Unit,
    showBack: Boolean,
    modifier: Modifier = Modifier,
) {
    val languages = AppLanguageManager.getSupportedLanguages()
    SoftAppPageScaffold(
        title = stringResource(R.string.settings_language_title),
        showBack = showBack,
        onBack = onBack,
        showPhoneMenu = false,
        modifier = modifier,
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding),
            contentPadding = PaddingValues(
                horizontal = adaptiveHorizontalPadding(),
                vertical = 8.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.settings_language_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = LocalTruckColors.current.TextSecondary,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            items(languages, key = { it.code }) { item ->
                LanguageOptionCard(
                    item = item,
                    selected = item.code == selectedCode,
                    onClick = { onSelect(item) },
                )
            }
        }
    }
}

@Composable
private fun LanguageOptionCard(
    item: LanguageItem,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val tc = LocalTruckColors.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                tc.AccentPrimary.copy(alpha = 0.16f)
            } else {
                tc.SurfaceSecondary
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = item.flagEmoji, style = MaterialTheme.typography.headlineSmall)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.nativeName,
                    color = tc.TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = item.code.uppercase(),
                    color = tc.TextSecondary,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            RadioButton(
                selected = selected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = tc.AccentPrimary),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LanguageSelectionScreenPreview() {
    TruckerLoadTheme(darkTheme = true, dynamicColor = false) {
        LanguageSelectionContent(
            selectedCode = "en",
            onSelect = {},
            onBack = {},
            showBack = true,
        )
    }
}
