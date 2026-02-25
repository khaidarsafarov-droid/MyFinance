package com.truckerload.presentation.screens.add

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.truckerload.presentation.di.LocalGeminiRepository
import com.truckerload.presentation.di.LocalLoadRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLoadScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit
) {
    var rawText by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val loadRepository = LocalLoadRepository.current
    val geminiRepository = LocalGeminiRepository.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New load") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = rawText,
                onValueChange = { rawText = it; error = null },
                modifier = Modifier.fillMaxWidth().height(180.dp),
                label = { Text("Paste Telegram message or enter load data...") },
                placeholder = { Text("Paste here...") }
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (geminiRepository != null) {
                Button(
                    onClick = {
                        if (rawText.isBlank()) return@Button
                        isSaving = true
                        error = null
                        scope.launch {
                            geminiRepository.parseLoadFromMessage(rawText)
                                .onSuccess { load ->
                                    loadRepository.insertLoad(load)
                                    onSaved()
                                }
                                .onFailure {
                                    error = it.message ?: "Parse failed"
                                    isSaving = false
                                }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = rawText.isNotBlank() && !isSaving
                ) {
                    Text(if (isSaving) "Saving..." else "Save to app (Offline-first)")
                }
            }
            Text(
                if (geminiRepository != null)
                    "Paste relay message and tap Save — data goes to local DB immediately. Or send to bot in Telegram."
                else
                "Отправьте сообщение боту в Telegram или добавьте GEMINI_API_KEY для локального парсинга.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
        }
    }
}
