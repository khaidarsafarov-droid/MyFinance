package com.truckerload.presentation.screens.detail

import com.truckerload.presentation.icons.AppIcons

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.truckerload.R
import com.truckerload.domain.model.withRouteMetrics
import com.truckerload.presentation.components.DashboardHero
import com.truckerload.presentation.components.LoadDetailSkeleton
import com.truckerload.presentation.theme.focusAfterNavigate
import com.truckerload.presentation.di.LocalPhotoRepository
import com.truckerload.presentation.di.LocalScanRepository
import com.truckerload.presentation.screens.privacy.PrivacyTrustBadge
import com.truckerload.utils.ShareHelper
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.presentation.theme.UiDimens
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoadDetailScreen(
    loadId: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onEditMiles: () -> Unit = onEdit,
    onEditFinish: () -> Unit,
    onDelete: () -> Unit,
    onPhotoClick: (String) -> Unit = {},
    onCamera: (loadId: String, tripId: String, loadDate: String) -> Unit = { _, _, _ -> },
    onScanner: (loadId: String, tripId: String, loadDate: String) -> Unit = { _, _, _ -> },
    onOpenPrivacy: () -> Unit = {},
) {
    val tc = LocalTruckColors.current
    val context = LocalContext.current
    val photoRepository = LocalPhotoRepository.current
    val scanRepository = LocalScanRepository.current
    val viewModel: LoadDetailViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val linkedPhotos by photoRepository.watchPhotosByLoadId(loadId).collectAsStateWithLifecycle(initialValue = emptyList())
    val linkedScans by scanRepository.watchScansByLoadId(loadId).collectAsStateWithLifecycle(initialValue = emptyList())
    val snackbarHostState = remember { SnackbarHostState() }
    val deleteFailed = stringResource(R.string.load_delete_failed)
    val saveErrorEmpty = stringResource(R.string.common_save_error, "")
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, loadId) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                LoadDetailEvent.Deleted -> onDelete()
                is LoadDetailEvent.Message -> snackbarHostState.showSnackbar(event.text)
            }
        }
    }

    Scaffold(
        containerColor = BentoGlassTheme.ScreenBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            val trip = uiState.load?.tripId?.takeIf { it.isNotBlank() }
            DashboardHero(
                title = stringResource(R.string.load_detail_title),
                subtitle = trip?.let { stringResource(R.string.load_detail_trip_line, it) },
                showBack = true,
                onBack = onBack,
                modifier = Modifier.focusAfterNavigate(
                    key = loadId,
                    enabled = !uiState.isLoading && uiState.load != null,
                ),
                actions = {
                    if (uiState.load != null) {
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(UiDimens.ToolbarTouchTarget),
                        ) {
                            Icon(
                                AppIcons.Edit,
                                contentDescription = stringResource(R.string.common_edit),
                                tint = Color.White,
                            )
                        }
                    }
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.size(UiDimens.ToolbarTouchTarget),
                    ) {
                        Icon(
                            AppIcons.Delete,
                            contentDescription = stringResource(R.string.load_detail_cd_delete),
                            tint = Color.White,
                        )
                    }
                },
            ) {
                PrivacyTrustBadge(
                    onClick = onOpenPrivacy,
                    modifier = Modifier.padding(start = 16.dp, top = 8.dp),
                )
            }
        },
    ) { padding ->
        when {
            uiState.loadError != null -> Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(uiState.loadError ?: stringResource(R.string.load_error_generic), color = tc.TextPrimary)
            }
            uiState.isLoading || uiState.load == null -> LoadDetailSkeleton(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            )
            else -> {
                val raw = uiState.load ?: return@Scaffold
                val l = remember(raw) { raw.withRouteMetrics() }
                LoadDetailLoadedContent(
                    load = l,
                    photos = linkedPhotos,
                    scans = linkedScans,
                    onEditMiles = onEditMiles,
                    onEditFinish = onEditFinish,
                    onSaveDeadhead = { miles -> viewModel.setDeadheadMiles(miles, saveErrorEmpty) },
                    onDisputeChanged = { updated -> viewModel.updateDispute(updated, saveErrorEmpty) },
                    onPhotoClick = onPhotoClick,
                    onCamera = { onCamera(l.id, l.tripId, l.date) },
                    onScanner = { onScanner(l.id, l.tripId, l.date) },
                    onScanClick = { scan -> ShareHelper(context).sharePdf(File(scan.filePath)) },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .imePadding(),
                )
            }
        }
    }
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.load_delete_confirm_title)) },
            text = { Text(stringResource(R.string.load_delete_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.delete(deleteFailed)
                }) {
                    Text(stringResource(R.string.common_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}
