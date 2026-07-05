package com.truckerload.presentation.screens.gallery

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.data.local.entities.PhotoEntity
import com.truckerload.domain.model.Load
import com.truckerload.domain.model.formatLoadRoute
import com.truckerload.presentation.di.LocalLoadRepository
import com.truckerload.presentation.di.LocalPhotoRepository
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.utils.PhotoManager
import com.truckerload.utils.ShareHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoDetailScreen(
    photoId: String,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val photoRepository = LocalPhotoRepository.current
    val loadRepository = LocalLoadRepository.current
    val tc = LocalTruckColors.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var photo by remember(photoId) { mutableStateOf<PhotoEntity?>(null) }
    var loads by remember { mutableStateOf<List<Load>>(emptyList()) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var loadMenuExpanded by remember { mutableStateOf(false) }
    var selectedLoadId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(photoId) {
        photo = withContext(Dispatchers.IO) { photoRepository.getPhotoById(photoId) }
        loads = withContext(Dispatchers.IO) { loadRepository.getLoadsForLinking() }
        selectedLoadId = photo?.loadId
    }

    val bitmap = remember(photo?.filePath) {
        photo?.filePath?.let { path ->
            if (File(path).exists()) BitmapFactory.decodeFile(path)?.asImageBitmap() else null
        }
    }

    Scaffold(
        containerColor = BentoGlassTheme.ScreenBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(photo?.fileName ?: stringResource(R.string.photo_gallery)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BentoGlassTheme.ScreenBackground),
            )
        },
    ) { padding ->
        val p = photo
        if (p == null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(stringResource(R.string.photo_file_missing), color = tc.TextSecondary)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = p.fileName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp)),
                )
            }

            Text("📍 ${p.city}, ${p.state} ${p.zipCode}".trim(), color = tc.TextPrimary)
            Text("📅 ${PhotoManager.formatDateTime(p.timestamp)}", color = tc.TextSecondary, style = MaterialTheme.typography.bodySmall)
            Text("📌 ${p.latitude}, ${p.longitude}", color = tc.TextSecondary, style = MaterialTheme.typography.bodySmall)

            Text(stringResource(R.string.link_to_load), style = MaterialTheme.typography.titleSmall, color = tc.TextPrimary)
            ExposedDropdownMenuBox(
                expanded = loadMenuExpanded,
                onExpandedChange = { loadMenuExpanded = it },
            ) {
                OutlinedButton(
                    onClick = { loadMenuExpanded = true },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                ) {
                    val label = selectedLoadId?.let { id ->
                        loads.find { it.id == id }?.let { formatLoadRoute(it) }
                    } ?: stringResource(R.string.select_load)
                    Text(label)
                }
                ExposedDropdownMenu(
                    expanded = loadMenuExpanded,
                    onDismissRequest = { loadMenuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.filter_unlinked)) },
                        onClick = {
                            selectedLoadId = null
                            loadMenuExpanded = false
                            scope.launch {
                                photoRepository.linkPhotoToLoad(p.id, null)
                                photo = photoRepository.getPhotoById(p.id)
                                snackbarHostState.showSnackbar(context.getString(R.string.photo_unlinked))
                            }
                        },
                    )
                    loads.forEach { load ->
                        DropdownMenuItem(
                            text = { Text(formatLoadRoute(load)) },
                            onClick = {
                                selectedLoadId = load.id
                                loadMenuExpanded = false
                                scope.launch {
                                    photoRepository.linkPhotoToLoad(p.id, load.id)
                                    photo = photoRepository.getPhotoById(p.id)
                                    snackbarHostState.showSnackbar(context.getString(R.string.photo_linked))
                                }
                            },
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = {
                        val file = File(p.filePath)
                        if (file.exists()) ShareHelper(context).sharePhoto(file)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = tc.AccentPrimary),
                ) {
                    Text(stringResource(R.string.share))
                }
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.common_delete))
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.common_delete)) },
            text = { Text(stringResource(R.string.photo_delete_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            photo?.let { entity ->
                                File(entity.filePath).delete()
                                photoRepository.deletePhoto(entity.id)
                            }
                            showDeleteDialog = false
                            onBack()
                        }
                    },
                ) {
                    Text(stringResource(R.string.common_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}
