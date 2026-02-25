package com.example.myfinance.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Sync
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.delay
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.myfinance.data.Company

@Composable
fun SettingsScreen(
    companies: List<Company>,
    weeklyTotals: List<com.example.myfinance.data.WeeklyTotal>,
    trips: List<com.example.myfinance.data.Trip>,
    onOpenCompany: (String) -> Unit,
    onAddCompany: ((String) -> Unit)? = null,
    logisticsViewModel: LogisticsViewModel? = null,
    telegramViewModel: TelegramViewModel? = null,
    modifier: Modifier = Modifier
) {
    var showAddDialog by remember { mutableStateOf(false) }
    val telegramState = telegramViewModel?.uiState?.collectAsState()?.value
    val telegramChatId = telegramViewModel?.chatId?.collectAsState()?.value

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            "Settings",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            "My Companies & Telegram",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
        Spacer(Modifier.height(16.dp))

        telegramViewModel?.let { vm ->
            if (vm.isConfigured) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Telegram",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        telegramState?.message?.let { msg ->
                            Spacer(Modifier.height(8.dp))
                            Text(msg, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                            LaunchedEffect(msg) { delay(4000); vm.clearMessage() }
                        }
                        if (telegramState?.linkCode != null) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Send this code to your bot in Telegram:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                            Text(
                                telegramState.linkCode,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (telegramState.isPolling) {
                                Text("Waiting for you to send the code…", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = { vm.stopLinking() }, shape = RoundedCornerShape(12.dp)) { Text("Cancel") }
                        } else {
                            Spacer(Modifier.height(8.dp))
                            if (telegramChatId != null) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text("Send totals or trip details to the bot (or add the bot to a group — Sync will analyze all messages and extract orders). Tap Sync to add.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                                    Spacer(Modifier.height(8.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = {
                                        logisticsViewModel?.let { lvm ->
                                            vm.syncFromTelegram(
                                                getCurrentCompanyId = { lvm.getCurrentCompany()?.id },
                                                onAddWeeklyTotal = { parsed -> lvm.addWeeklyTotalFromParsed(parsed, companies) },
                                                onAddTrip = { parsed -> lvm.addTripFromParsed(parsed, lvm.getCurrentCompany()?.id) }
                                            )
                                        }
                                    }, shape = RoundedCornerShape(12.dp), enabled = telegramState?.isSyncing != true) {
                                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.size(4.dp))
                                        Text("Sync")
                                    }
                                    Button(onClick = { vm.sendTestMessage() }, shape = RoundedCornerShape(12.dp), enabled = telegramState?.isLoading != true) {
                                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.size(4.dp))
                                        Text("Test")
                                    }
                                    Button(onClick = { vm.disconnect() }, shape = RoundedCornerShape(12.dp)) {
                                        Icon(Icons.Default.LinkOff, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.size(4.dp))
                                        Text("Disconnect")
                                    }
                                    }
                                }
                            } else {
                                    Button(onClick = { vm.startLinking() }, shape = RoundedCornerShape(12.dp)) {
                                        Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.size(4.dp))
                                        Text("Connect Telegram")
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "My Companies",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.weight(1f)
            )
            if (onAddCompany != null) {
                Button(
                    onClick = { showAddDialog = true },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Add Company")
                }
            }
        }
        if (showAddDialog) {
            var name by remember { mutableStateOf("") }
            Dialog(onDismissRequest = { showAddDialog = false }) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.padding(16.dp)
                ) {
                    Column(Modifier.padding(24.dp)) {
                        Text("Add Company", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Company Name") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Button(onClick = { showAddDialog = false }, modifier = Modifier.weight(1f)) { Text("Cancel") }
                            Spacer(Modifier.size(8.dp))
                            Button(
                                onClick = {
                                    if (name.isNotBlank()) {
                                        onAddCompany?.invoke(name.trim())
                                        showAddDialog = false
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("Add") }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        companies.forEach { company ->
            val companyWeeks = weeklyTotals.filter { it.companyIds.contains(company.id) }
            val weeks = companyWeeks.size
            val gross = companyWeeks.sumOf { it.gross }
            val profit = companyWeeks.sumOf { it.netProfit }
            val diesel = companyWeeks.sumOf { it.diesel }
            Card(
                onClick = { onOpenCompany(company.id) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Business,
                                contentDescription = null,
                                modifier = Modifier.padding(8.dp).size(24.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(Modifier.size(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                company.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "$weeks week${if (weeks == 1) "" else "s"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            Text(
                                "Gross ${formatCurrency(gross)} · Profit ${formatCurrency(profit)} · Diesel ${formatCurrency(diesel)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        if (company.isCurrent) {
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.size(4.dp))
                                    Text("Current", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        } else {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = "Open",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                    if (!company.isCurrent && logisticsViewModel != null) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            TextButton(onClick = { logisticsViewModel.setCurrentCompany(company.id) }) {
                                Text("Set as current company")
                            }
                        }
                    }
                }
            }
        }
    }
