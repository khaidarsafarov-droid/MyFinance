package com.truckerload.share

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.truckerload.R

internal enum class ShareReceiverMessage {
    REJECTED,
    DUPLICATE,
    TOO_LARGE,
    UNREADABLE,
    SIGN_IN,
}

@Composable
internal fun ShareReceiverScreen(
    checking: Boolean,
    message: ShareReceiverMessage?,
    onDone: () -> Unit,
    onViewLoad: (() -> Unit)?,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (checking) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.share_import_checking),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
            )
            return@Column
        }
        val title = when (message) {
            ShareReceiverMessage.REJECTED -> R.string.share_import_rejected_title
            ShareReceiverMessage.DUPLICATE -> R.string.share_import_duplicate_title
            ShareReceiverMessage.TOO_LARGE -> R.string.share_import_rejected_title
            ShareReceiverMessage.UNREADABLE -> R.string.share_import_rejected_title
            ShareReceiverMessage.SIGN_IN -> R.string.share_import_rejected_title
            null -> R.string.share_import_rejected_title
        }
        val body = when (message) {
            ShareReceiverMessage.REJECTED -> R.string.share_import_rejected_body
            ShareReceiverMessage.DUPLICATE -> R.string.share_import_duplicate_body
            ShareReceiverMessage.TOO_LARGE -> R.string.share_import_too_large
            ShareReceiverMessage.UNREADABLE -> R.string.share_import_unreadable
            ShareReceiverMessage.SIGN_IN -> R.string.share_import_sign_in
            null -> R.string.share_import_unreadable
        }
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(28.dp))
        if (onViewLoad != null) {
            Button(onClick = onViewLoad) {
                Text(stringResource(R.string.share_import_view_load))
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        OutlinedButton(onClick = onDone) {
            Text(stringResource(R.string.share_import_done))
        }
    }
}
