package com.truckerload.presentation.screens.social.friends

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.domain.friends.FriendProfileHit
import com.truckerload.presentation.screens.social.friends.map.FriendRequestStatusText
import com.truckerload.presentation.theme.AppTextFieldDefaults
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.components.TlButton as Button
import com.truckerload.presentation.components.TlOutlinedButton as OutlinedButton

@Composable
fun FriendsAddByNicknameForm(
    searchQuery: String,
    searchBusy: Boolean,
    searchHit: FriendProfileHit?,
    searchNotFound: Boolean,
    statusMessage: String?,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tc = LocalTruckColors.current
    val context = LocalContext.current
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.friends_add_by_nickname_title),
            style = MaterialTheme.typography.titleSmall,
            color = tc.TextPrimary,
        )
        Text(
            text = stringResource(R.string.friends_directory_hint),
            style = MaterialTheme.typography.bodySmall,
            color = tc.TextSecondary,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onQueryChange,
                label = { Text(stringResource(R.string.friends_search_nickname_label)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = AppTextFieldDefaults.outlined(),
            )
            Button(
                onClick = onSearch,
                enabled = !searchBusy,
            ) {
                if (searchBusy) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.PersonAdd, contentDescription = null)
                }
            }
        }
        searchHit?.let { hit ->
            Text(
                text = stringResource(R.string.friends_found, hit.nickname),
                style = MaterialTheme.typography.bodyMedium,
                color = tc.TextPrimary,
            )
            Button(
                onClick = onAdd,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.friends_request_send_button))
            }
        }
        FriendRequestStatusText(statusMessage)
        if (statusMessage == "self") {
            Text(
                text = stringResource(R.string.friends_cannot_add_self),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        if (statusMessage == "need_supabase") {
            Text(
                text = stringResource(R.string.friends_live_need_supabase),
                style = MaterialTheme.typography.bodySmall,
                color = tc.AccentPrimary,
            )
        }
        if (searchNotFound || statusMessage == "not_found") {
            Text(
                text = stringResource(R.string.friends_not_in_app),
                style = MaterialTheme.typography.bodySmall,
                color = tc.TextSecondary,
            )
            OutlinedButton(
                onClick = {
                    val share = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, context.getString(R.string.friends_invite_share_text))
                    }
                    context.startActivity(
                        Intent.createChooser(share, context.getString(R.string.friends_invite_share_title)),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 6.dp),
                )
                Text(stringResource(R.string.friends_invite_share_button))
            }
        }
    }
}
