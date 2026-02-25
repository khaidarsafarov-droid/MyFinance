package com.truckerload.presentation.screens.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.truckerload.presentation.di.LocalDieselRepository
import com.truckerload.presentation.di.LocalGeminiRepository
import com.truckerload.presentation.di.LocalLoadRepository
import com.truckerload.presentation.di.LocalPaycheckRepository
import com.truckerload.presentation.theme.LocalTruckColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen() {
    val tc = LocalTruckColors.current
    val geminiRepository = LocalGeminiRepository.current
    val loadRepository = LocalLoadRepository.current
    val paycheckRepository = LocalPaycheckRepository.current
    val dieselRepository = LocalDieselRepository.current
    val viewModel: ChatViewModel = viewModel(
        factory = ChatViewModel.Factory(
            geminiRepository,
            loadRepository,
            paycheckRepository,
            dieselRepository
        )
    )
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Scaffold(
        containerColor = tc.Background,
        topBar = {
            TopAppBar(
                title = { Text("Chat with Gemini", color = tc.TextPrimary) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = tc.Background,
                    titleContentColor = tc.TextPrimary
                )
            )
        }
    ) { padding ->
        if (!viewModel.isAvailable) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Add GEMINI_API_KEY to local.properties and rebuild to use the chat.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = tc.TextSecondary
                )
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
            ) {
                items(uiState.messages, key = { "${it.role}-${it.text.hashCode()}" }) { msg ->
                    MessageBubble(
                        isUser = msg.role == "user",
                        text = msg.text
                    )
                }
                if (uiState.isLoading) {
                    item {
                        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(8.dp),
                                color = tc.AccentPrimary,
                                strokeWidth = 2.dp
                            )
                            Text("Thinking...", color = tc.TextSecondary, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = uiState.inputText,
                    onValueChange = viewModel::setInputText,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Message...", color = tc.TextSecondary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = tc.TextPrimary,
                        unfocusedTextColor = tc.TextPrimary,
                        cursorColor = tc.AccentPrimary,
                        focusedBorderColor = tc.AccentPrimary,
                        unfocusedBorderColor = tc.Divider,
                        focusedContainerColor = tc.SurfaceSecondary,
                        unfocusedContainerColor = tc.SurfaceSecondary
                    ),
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 4
                )
                IconButton(
                    onClick = { viewModel.sendMessage() },
                    enabled = uiState.inputText.isNotBlank() && !uiState.isLoading
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Send",
                        tint = if (uiState.inputText.isNotBlank() && !uiState.isLoading)
                            tc.AccentPrimary else tc.TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(isUser: Boolean, text: String) {
    val tc = LocalTruckColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier.padding(horizontal = 4.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) tc.AccentPrimary.copy(alpha = 0.2f) else tc.SurfaceSecondary
            )
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = tc.TextPrimary
            )
        }
    }
}
