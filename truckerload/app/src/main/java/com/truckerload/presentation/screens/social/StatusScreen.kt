package com.truckerload.presentation.screens.social

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.truckerload.R
import com.truckerload.domain.social.DriverStatusPost
import com.truckerload.domain.social.StatusType
import com.truckerload.presentation.di.LocalSocialRepository
import com.truckerload.presentation.theme.BentoGlassCard
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.LocalTruckColors
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusScreen(
    onBack: () -> Unit,
    viewModel: StatusViewModel = viewModel(
        factory = StatusViewModel.Factory(LocalSocialRepository.current),
    ),
) {
    val uiState by viewModel.uiState.collectAsState()
    val profileVm: ProfileViewModel = viewModel(
        factory = ProfileViewModel.Factory(LocalSocialRepository.current),
    )
    val profileState by profileVm.uiState.collectAsState()
    val displayName = profileState.profile?.displayName.orEmpty()
    val tc = LocalTruckColors.current
    val context = LocalContext.current

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val bitmap = BitmapFactory.decodeStream(stream) ?: return@rememberLauncherForActivityResult
            viewModel.postPhotoStatus(bitmap, displayName, uiState.inputText)
            viewModel.setInput("")
        }
    }

    Scaffold(
        containerColor = BentoGlassTheme.ScreenBackground,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.social_statuses)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BentoGlassTheme.ScreenBackground),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BentoGlassCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.social_add_status), style = MaterialTheme.typography.titleMedium, color = tc.TextPrimary)
                    OutlinedTextField(
                        value = uiState.inputText,
                        onValueChange = viewModel::setInput,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(stringResource(R.string.social_status_hint)) },
                    )
                    androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.postTextStatus(displayName) },
                            enabled = uiState.inputText.isNotBlank(),
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Text(stringResource(R.string.social_post_status))
                        }
                        IconButton(onClick = { photoPicker.launch("image/*") }) {
                            Icon(Icons.Default.Image, contentDescription = stringResource(R.string.social_attach_photo), tint = tc.AccentPrimary)
                        }
                    }
                }
            }
            LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(uiState.statuses, key = { it.id }) { status ->
                    StatusItem(status = status, onView = { viewModel.markViewed(status.id) })
                }
            }
        }
    }
}

@Composable
private fun StatusItem(status: DriverStatusPost, onView: () -> Unit) {
    val tc = LocalTruckColors.current
    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(status.createdAt))
    BentoGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onView),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            androidx.compose.foundation.layout.Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(tc.AccentPrimary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(status.displayName.take(1).uppercase())
                }
                Column {
                    Text(status.displayName, style = MaterialTheme.typography.titleSmall, color = tc.TextPrimary)
                    Text(time, style = MaterialTheme.typography.labelSmall, color = tc.TextSecondary)
                }
            }
            when (status.type) {
                StatusType.TEXT -> Text(
                    text = status.text.orEmpty(),
                    modifier = Modifier.padding(top = 8.dp),
                    color = tc.TextPrimary,
                )
                StatusType.PHOTO -> {
                    status.mediaPath?.let { path ->
                        AsyncImage(
                            model = File(path),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop,
                        )
                    }
                    status.text?.let {
                        Text(it, modifier = Modifier.padding(top = 4.dp), color = tc.TextSecondary)
                    }
                }
                StatusType.VOICE -> Text(
                    text = "🎤 ${status.durationMs / 1000}s",
                    modifier = Modifier.padding(top = 8.dp),
                    color = tc.AccentPrimary,
                )
            }
        }
    }
}
