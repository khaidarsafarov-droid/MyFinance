package com.truckerload.presentation.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.truckerload.presentation.theme.LocalTruckColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val tc = LocalTruckColors.current
    Scaffold(
        containerColor = tc.Background,
        topBar = {
            TopAppBar(
                title = { Text("Настройки", color = tc.TextPrimary) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = tc.TextPrimary) } },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = tc.Background,
                    titleContentColor = tc.TextPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(tc.Background)
                .padding(padding)
                .padding(16.dp)
        ) {
            Text("TELEGRAM БОТ", style = androidx.compose.material3.MaterialTheme.typography.labelMedium, color = tc.TextLabel)
            Text("Статус, Bot Token, Webhook URL — здесь.", color = tc.TextSecondary, modifier = Modifier.padding(top = 4.dp))
            Spacer(modifier = Modifier.padding(8.dp))
            Text("Группы: добавьте бота в группу. Чтобы бот видел все сообщения группы (и сохранял лоуды по дате сообщения), в BotFather выполните /setprivacy и выберите Disable.", style = androidx.compose.material3.MaterialTheme.typography.bodySmall, color = tc.TextSecondary, modifier = Modifier.padding(top = 4.dp))
            Spacer(modifier = Modifier.padding(16.dp))
            Text("GEMINI API", style = androidx.compose.material3.MaterialTheme.typography.labelMedium, color = tc.TextLabel)
            Text("API Key, модель, тест соединения.", color = tc.TextSecondary, modifier = Modifier.padding(top = 4.dp))
            Spacer(modifier = Modifier.padding(16.dp))
            Text("УВЕДОМЛЕНИЯ", style = androidx.compose.material3.MaterialTheme.typography.labelMedium, color = tc.TextLabel)
            Text("Новый лоуд, зарплата, дизель.", color = tc.TextSecondary, modifier = Modifier.padding(top = 4.dp))
            Spacer(modifier = Modifier.padding(16.dp))
            Text("ДАННЫЕ", style = androidx.compose.material3.MaterialTheme.typography.labelMedium, color = tc.TextLabel)
            Text("Экспорт CSV, резервная копия, очистить данные.", color = tc.TextSecondary, modifier = Modifier.padding(top = 4.dp))
        }
    }
}
