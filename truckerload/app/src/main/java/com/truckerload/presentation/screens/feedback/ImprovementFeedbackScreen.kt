package com.truckerload.presentation.screens.feedback

import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.truckerload.BuildConfig
import com.truckerload.R
import com.truckerload.domain.feedback.ImprovementFeedbackMail
import com.truckerload.presentation.components.SoftAppPageScaffold
import com.truckerload.presentation.components.TlButton as Button
import com.truckerload.presentation.components.TlChipButton
import com.truckerload.presentation.components.verticalContentScroll
import com.truckerload.presentation.theme.AppTextFieldDefaults
import com.truckerload.presentation.theme.BentoGlassCard
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.utils.ImprovementFeedbackSendResult
import com.truckerload.utils.ImprovementFeedbackSender

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImprovementFeedbackScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val tc = LocalTruckColors.current
    var topic by rememberSaveable { mutableStateOf(ImprovementFeedbackMail.Topic.IMPROVE) }
    var message by rememberSaveable { mutableStateOf("") }
    var submitted by rememberSaveable { mutableStateOf(false) }

    SoftAppPageScaffold(
        title = stringResource(R.string.improve_title),
        showBack = true,
        onBack = onBack,
        showPhoneMenu = false,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalContentScroll()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (submitted) {
                FeedbackThankYou(onBack = onBack)
                return@Column
            }

            Text(
                text = stringResource(R.string.improve_intro),
                style = MaterialTheme.typography.bodyLarge,
                color = tc.TextPrimary,
            )
            Text(
                text = stringResource(R.string.improve_to, ImprovementFeedbackMail.SUPPORT_EMAIL),
                style = MaterialTheme.typography.bodySmall,
                color = tc.TextSecondary,
            )
            BentoGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = stringResource(R.string.improve_topic),
                        style = MaterialTheme.typography.titleSmall,
                        color = tc.TextPrimary,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TopicChip(
                            label = stringResource(R.string.improve_topic_add),
                            selected = topic == ImprovementFeedbackMail.Topic.ADD,
                            onClick = { topic = ImprovementFeedbackMail.Topic.ADD },
                            modifier = Modifier.weight(1f),
                        )
                        TopicChip(
                            label = stringResource(R.string.improve_topic_improve),
                            selected = topic == ImprovementFeedbackMail.Topic.IMPROVE,
                            onClick = { topic = ImprovementFeedbackMail.Topic.IMPROVE },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        TopicChip(
                            label = stringResource(R.string.improve_topic_works),
                            selected = topic == ImprovementFeedbackMail.Topic.WORKS,
                            onClick = { topic = ImprovementFeedbackMail.Topic.WORKS },
                            modifier = Modifier.weight(1f),
                        )
                        TopicChip(
                            label = stringResource(R.string.improve_topic_broken),
                            selected = topic == ImprovementFeedbackMail.Topic.BROKEN,
                            onClick = { topic = ImprovementFeedbackMail.Topic.BROKEN },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        label = { Text(stringResource(R.string.improve_message_label)) },
                        placeholder = { Text(stringResource(R.string.improve_message_hint)) },
                        colors = AppTextFieldDefaults.outlined(),
                    )
                    Button(
                        onClick = {
                            val topicLabel = context.getString(topicLabelRes(topic))
                            val draft = ImprovementFeedbackMail.compose(
                                topic = topic,
                                message = message,
                                topicLabel = topicLabel,
                                appVersion = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                                androidRelease = Build.VERSION.RELEASE,
                            )
                            if (draft == null) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.improve_empty),
                                    Toast.LENGTH_SHORT,
                                ).show()
                                return@Button
                            }
                            val result = ImprovementFeedbackSender.send(context, draft)
                            val followUp = when (result) {
                                ImprovementFeedbackSendResult.OPENED_EMAIL ->
                                    context.getString(R.string.improve_opened)
                                ImprovementFeedbackSendResult.COPIED_FALLBACK ->
                                    context.getString(
                                        R.string.improve_copied,
                                        ImprovementFeedbackMail.SUPPORT_EMAIL,
                                    )
                            }
                            Toast.makeText(
                                context,
                                "${context.getString(R.string.improve_thank_you)}\n$followUp",
                                Toast.LENGTH_LONG,
                            ).show()
                            submitted = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.improve_send))
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedbackThankYou(onBack: () -> Unit) {
    val tc = LocalTruckColors.current
    BentoGlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.improve_thank_you),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = tc.AccentPrimary,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.improve_thank_you_body),
                style = MaterialTheme.typography.bodyLarge,
                color = tc.TextPrimary,
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.improve_thank_you_close))
            }
        }
    }
}

@Composable
private fun TopicChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TlChipButton(
        text = label,
        onClick = onClick,
        modifier = modifier,
        selected = selected,
    )
}

private fun topicLabelRes(topic: ImprovementFeedbackMail.Topic): Int = when (topic) {
    ImprovementFeedbackMail.Topic.ADD -> R.string.improve_topic_add
    ImprovementFeedbackMail.Topic.IMPROVE -> R.string.improve_topic_improve
    ImprovementFeedbackMail.Topic.WORKS -> R.string.improve_topic_works
    ImprovementFeedbackMail.Topic.BROKEN -> R.string.improve_topic_broken
}
