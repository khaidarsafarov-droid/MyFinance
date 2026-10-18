package com.truckerload.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.truckerload.presentation.icons.AppIcons
import com.truckerload.presentation.theme.TruckerLoadTheme

@Composable
internal fun DashboardStylePreview(dark: Boolean, modifier: Modifier = Modifier) {
    TruckerLoadTheme(darkTheme = dark, dynamicColor = false, reduceMotion = true) {
        Column(modifier = modifier.fillMaxWidth()) {
            DashboardHero(
                title = "Журнал",
                subtitle = "Неделя 42",
                showMenu = true,
            ) {}
            Column(modifier = Modifier.fillMaxWidth()) {
                DashboardSectionHeader("Счета")
                DashboardCard {
                    DashboardRow(
                        icon = AppIcons.Payments,
                        title = "Зарплата",
                        subtitle = "18 окт",
                        value = "$1,200",
                        onClick = {},
                    )
                }
                DashboardEmpty(title = "Пока пусто", message = "Добавьте первую запись.")
            }
        }
    }
}

@Preview(name = "Light", showBackground = true, widthDp = 390, heightDp = 700)
@Composable
private fun DashboardLightPreview() {
    DashboardStylePreview(dark = false)
}

@Preview(name = "Dark", showBackground = true, widthDp = 390, heightDp = 700)
@Composable
private fun DashboardDarkPreview() {
    DashboardStylePreview(dark = true)
}
