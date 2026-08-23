package com.truckerload.presentation.screens.camera

import com.truckerload.presentation.icons.AppIcons

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.truckerload.R
import com.truckerload.presentation.components.TlButton as Button
import com.truckerload.presentation.components.TlOutlinedButton as OutlinedButton
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.utils.rememberDecodedBitmap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoBatchReviewScreen(
    photos: List<CapturedPhoto>,
    onAddMore: () -> Unit,
    onRemoveAt: (Int) -> Unit,
    onSaveAll: () -> Unit,
    onShare: () -> Unit,
    onCancel: () -> Unit,
    onSaved: () -> Unit,
    saveSuccess: Boolean,
    saveError: Boolean,
    onSaveErrorShown: () -> Unit,
) {
    val context = LocalContext.current
    val tc = LocalTruckColors.current
    val snackbarHostState = remember { SnackbarHostState() }
    var previewIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
            snackbarHostState.showSnackbar(context.getString(R.string.photo_saved))
            onSaved()
        }
    }

    LaunchedEffect(saveError) {
        if (saveError) {
            snackbarHostState.showSnackbar(context.getString(R.string.photo_save_error))
            onSaveErrorShown()
        }
    }

    LaunchedEffect(photos.size) {
        val index = previewIndex ?: return@LaunchedEffect
        if (index !in photos.indices) {
            previewIndex = null
        }
    }

    previewIndex?.let { index ->
        if (index in photos.indices) {
            FullscreenPhotoDialog(
                photo = photos[index],
                onDismiss = { previewIndex = null },
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.camera_batch_title, photos.size))
                },
                navigationIcon = {
                    IconButton(onClick = onAddMore) {
                        Icon(
                            AppIcons.ArrowBack,
                            contentDescription = stringResource(R.string.camera_add_another),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(photos, key = { _, photo -> photo.file.absolutePath }) { index, photo ->
                    val bitmap = rememberDecodedBitmap(photo.file.absolutePath)
                    Column {
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap,
                                contentDescription = stringResource(R.string.camera_photo_preview),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { previewIndex = index },
                            )
                        }
                        IconButton(
                            onClick = { onRemoveAt(index) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(
                                AppIcons.Close,
                                contentDescription = stringResource(R.string.common_delete),
                                tint = tc.AccentExpense,
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = onAddMore,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.camera_add_another))
                }
                Button(
                    onClick = onShare,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = tc.AccentPrimary),
                ) {
                    Text(stringResource(R.string.camera_share_all))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onSaveAll,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = tc.AccentProfit),
                ) {
                    Text(stringResource(R.string.camera_save_all))
                }
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        }
    }
}

@Composable
private fun FullscreenPhotoDialog(
    photo: CapturedPhoto,
    onDismiss: () -> Unit,
) {
    val bitmap = rememberDecodedBitmap(photo.file.absolutePath)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                )
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = stringResource(R.string.camera_photo_preview),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                        .align(Alignment.Center),
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.45f)),
            ) {
                Icon(
                    AppIcons.Close,
                    contentDescription = stringResource(R.string.common_close),
                    tint = Color.White,
                )
            }
        }
    }
}
