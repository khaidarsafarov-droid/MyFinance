package com.truckerload.presentation.screens.advisor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import com.truckerload.presentation.components.TlTextButton as TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import android.content.ClipData
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.truckerload.R
import com.truckerload.presentation.di.LocalDieselRepository
import com.truckerload.presentation.di.LocalAiRepository
import com.truckerload.presentation.di.LocalLoadRepository
import com.truckerload.presentation.di.LocalPaycheckRepository
import com.truckerload.presentation.screens.chat.ChatMessage
import com.truckerload.presentation.screens.chat.ChatViewModel
import com.truckerload.presentation.theme.AppTextFieldDefaults
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.theme.UiDimens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialAdvisorScreen(onBack: () -> Unit) {
    val tc = LocalTruckColors.current
    val context = LocalContext.current
    val viewModel: ChatViewModel = viewModel(
        factory = ChatViewModel.Factory(
            LocalAiRepository.current,
            LocalLoadRepository.current,
            LocalPaycheckRepository.current,
            LocalDieselRepository.current,
            context
        )
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val copiedMessage = stringResource(R.string.advisor_health_copied)
    val suggestedQuestions = listOf(
        stringResource(R.string.advisor_suggestion_income),
        stringResource(R.string.advisor_suggestion_routes),
        stringResource(R.string.advisor_suggestion_taxes),
        stringResource(R.string.advisor_suggestion_compare_week)
    )
    var healthFlashColor by remember { mutableStateOf<Color?>(null) }
    var healthCooldown by remember { mutableStateOf(false) }
    val pulseTransition = rememberInfiniteTransition(label = "advisorHealthPulse")
    val healthButtonScale by pulseTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "advisorHealthPulseScale"
    )
    val healthDotAlpha by pulseTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "advisorHealthDotAlpha"
    )

    LaunchedEffect(uiState.messages.size, uiState.messages.lastOrNull()?.text) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }
    LaunchedEffect(uiState.messages.lastOrNull()?.text, uiState.isLoading) {
        if (uiState.isLoading) return@LaunchedEffect
        val lastText = uiState.messages.lastOrNull()?.text.orEmpty()
        val isHealthSuccess = lastText.startsWith("✅") && lastText.contains("AI health:")
        val isHealthFailure = lastText.startsWith("❌") && lastText.contains("AI health:")
        when {
            isHealthSuccess -> {
                healthFlashColor = tc.AccentProfit
                delay(800)
                healthFlashColor = null
            }
            isHealthFailure -> {
                healthFlashColor = tc.AccentExpense
                delay(800)
                healthFlashColor = null
            }
        }
    }

    Scaffold(
        containerColor = tc.Background,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.advisor_title), color = tc.TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.size(UiDimens.ToolbarTouchTarget)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back), tint = tc.TextPrimary)
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (healthCooldown) return@TextButton
                            healthCooldown = true
                            scope.launch {
                                delay(1000)
                                healthCooldown = false
                            }
                            viewModel.runAiHealthCheck()
                        },
                        enabled = !uiState.isLoading && !healthCooldown,
                        modifier = Modifier
                            .size(44.dp)
                            .graphicsLayer {
                                val scale = if (uiState.isLoading) healthButtonScale else 1f
                                scaleX = scale
                                scaleY = scale
                            }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.advisor_health_short),
                                color = when {
                                    uiState.isLoading -> tc.TextSecondary
                                    healthFlashColor != null -> healthFlashColor ?: tc.AccentPrimary
                                    else -> tc.AccentPrimary
                                },
                                style = MaterialTheme.typography.labelLarge
                            )
                            if (uiState.isLoading) {
                                Text(
                                    text = "•",
                                    modifier = Modifier.padding(start = 3.dp),
                                    color = tc.AccentPrimary.copy(alpha = healthDotAlpha),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = tc.Background,
                    titleContentColor = tc.TextPrimary
                )
            )
        }
    ) { padding ->
        if (!viewModel.isAvailable) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    stringResource(R.string.advisor_missing_key),
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
            if (uiState.messages.isEmpty()) {
                LazyRow(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(suggestedQuestions, key = { it }) { q ->
                        FilterChip(
                            selected = false,
                            onClick = { viewModel.sendMessage(q) },
                            label = { Text(q, style = MaterialTheme.typography.bodySmall) }
                        )
                    }
                }
            }
            LazyColumn(
                modifier = Modifier.weight(1f),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                itemsIndexed(uiState.messages, key = { index, _ -> "msg_$index" }) { _, msg ->
                    MessageBubble(
                        isUser = msg.role == "user",
                        text = msg.text,
                        onCopyDiagnostics = {
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = copiedMessage
                                )
                            }
                        }
                    )
                }
                if (uiState.isLoading && uiState.messages.lastOrNull()?.text.isNullOrBlank()) {
                    item(key = "typing") {
                        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.padding(8.dp), color = tc.AccentPrimary, strokeWidth = 2.dp)
                            Text(stringResource(R.string.advisor_thinking), color = tc.TextSecondary, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = uiState.inputText,
                    onValueChange = viewModel::setInputText,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(stringResource(R.string.advisor_placeholder_question), color = tc.TextSecondary) },
                    colors = AppTextFieldDefaults.outlined(),
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 4
                )
                IconButton(
                    onClick = { viewModel.sendMessage() },
                    enabled = uiState.inputText.isNotBlank() && !uiState.isLoading,
                    modifier = Modifier.size(UiDimens.ToolbarTouchTarget)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.advisor_cd_send), tint = if (uiState.inputText.isNotBlank() && !uiState.isLoading) tc.AccentPrimary else tc.TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(
    isUser: Boolean,
    text: String,
    onCopyDiagnostics: () -> Unit = {}
) {
    val tc = LocalTruckColors.current
    val clipboard = LocalClipboard.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val isHealthSuccess = !isUser && text.startsWith("✅") && text.contains("AI health:")
    val isHealthFailure = !isUser && text.startsWith("❌") && text.contains("AI health:")
    val isHealthMessage = isHealthSuccess || isHealthFailure
    val bubbleColor = when {
        isUser -> tc.AccentPrimary.copy(alpha = 0.2f)
        isHealthSuccess -> tc.AccentProfit.copy(alpha = 0.22f)
        isHealthFailure -> tc.AccentExpense.copy(alpha = 0.22f)
        else -> tc.SurfaceSecondary
    }
    var expanded by remember(text) { mutableStateOf(false) }
    var copiedFlash by remember(text) { mutableStateOf(false) }
    val healthHeader = if (isHealthMessage) text.substringBefore(": ").ifBlank { text } else text
    val healthBody = if (isHealthMessage) text.substringAfter(": ", "").trim() else ""
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .animateContentSize()
                .then(
                    if (isHealthMessage) {
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { expanded = !expanded }
                    } else {
                        Modifier
                    }
                ),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = bubbleColor
            )
        ) {
            if (isHealthMessage) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = healthHeader,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = tc.TextPrimary
                    )
                    if (expanded && healthBody.isNotBlank()) {
                        Text(
                            text = healthBody,
                            style = MaterialTheme.typography.bodySmall,
                            color = tc.TextSecondary
                        )
                        TextButton(
                            onClick = {
                                scope.launch {
                                    clipboard.setClipEntry(
                                        ClipEntry(ClipData.newPlainText("diagnostics", text)),
                                    )
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onCopyDiagnostics()
                                    copiedFlash = true
                                    delay(1200)
                                    copiedFlash = false
                                    expanded = false
                                }
                            },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text(
                                text = if (copiedFlash) {
                                    stringResource(R.string.advisor_health_copied_short)
                                } else {
                                    stringResource(R.string.advisor_health_copy_details)
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = tc.AccentPrimary
                            )
                        }
                    }
                    Text(
                        text = if (expanded) stringResource(R.string.advisor_health_hide_details) else stringResource(R.string.advisor_health_show_details),
                        style = MaterialTheme.typography.labelSmall,
                        color = tc.TextSecondary
                    )
                }
            } else {
                Text(text = text, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium, color = tc.TextPrimary)
            }
        }
    }
}
