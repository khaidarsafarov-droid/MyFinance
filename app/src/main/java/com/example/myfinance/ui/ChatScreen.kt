package com.example.myfinance.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myfinance.data.Company

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    appDataContext: String,
    telegramViewModel: TelegramViewModel? = null,
    logisticsViewModel: LogisticsViewModel? = null,
    companies: List<Company> = emptyList(),
    modifier: Modifier = Modifier
) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val telegramState by if (telegramViewModel != null) telegramViewModel.uiState.collectAsState() else remember { mutableStateOf(TelegramUiState()) }
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Column(modifier = modifier.fillMaxSize()) {
        if (telegramViewModel != null && logisticsViewModel != null && telegramViewModel.isConfigured) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        telegramViewModel.syncFromTelegram(
                            getCurrentCompanyId = { logisticsViewModel.getCurrentCompany()?.id },
                            onAddWeeklyTotal = { parsed -> logisticsViewModel.addWeeklyTotalFromParsed(parsed, companies) },
                            onAddTrip = { parsed -> logisticsViewModel.addTripFromParsed(parsed, logisticsViewModel.getCurrentCompany()?.id) }
                        )
                    },
                    enabled = telegramState.isSyncing != true,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(if (telegramState.isSyncing == true) "Syncing…" else "Sync from Telegram", modifier = Modifier.padding(start = 6.dp))
                }
                if (telegramState.message != null) {
                    Text(
                        telegramState.message!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (messages.isEmpty()) {
                item {
                    Text(
                        "Ask the AI (Send) or send a request to the bot (▶): e.g. \"Total Rate: \$1247, Miles: 218\" or \"gross 5000 salary 3000 diesel 500\" — the bot will add a week or a load. You can also Sync from Telegram above.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
            items(messages) { msg ->
                val isUser = msg.role == "user"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            if (isUser) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            msg.text,
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (isLoading) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text("...", modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            BasicTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                decorationBox = { inner ->
                    Box {
                        if (inputText.isEmpty()) {
                            Text("Ask about your data...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                        }
                        inner()
                    }
                },
                singleLine = false,
                maxLines = 3
            )
            if (telegramViewModel != null && logisticsViewModel != null) {
                IconButton(
                    onClick = {
                        val t = inputText.trim()
                        if (t.isNotBlank()) {
                            viewModel.sendToBot(t) { txt ->
                                telegramViewModel.processMessageLocally(
                                    txt,
                                    getCurrentCompanyId = { logisticsViewModel.getCurrentCompany()?.id },
                                    onAddWeeklyTotal = { parsed -> logisticsViewModel.addWeeklyTotalFromParsed(parsed, companies) },
                                    onAddTrip = { parsed -> logisticsViewModel.addTripFromParsed(parsed, logisticsViewModel.getCurrentCompany()?.id) }
                                )
                            }
                            inputText = ""
                        }
                    }
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Send to bot", modifier = Modifier.size(24.dp))
                }
            }
            IconButton(
                onClick = {
                    val t = inputText.trim()
                    if (t.isNotBlank()) {
                        viewModel.sendMessage(t, appDataContext)
                        inputText = ""
                    }
                }
            ) {
                Icon(Icons.Default.Send, contentDescription = "Ask AI", modifier = Modifier.size(24.dp))
            }
        }
    }
}
