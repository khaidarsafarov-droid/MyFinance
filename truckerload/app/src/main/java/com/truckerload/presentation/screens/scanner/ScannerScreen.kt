package com.truckerload.presentation.screens.scanner

import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import com.truckerload.presentation.components.TlButton as Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.truckerload.R
import com.truckerload.presentation.di.LocalScanRepository
import com.truckerload.utils.DocumentScannerService
import kotlinx.coroutines.delay

@Composable
fun ScannerFlowScreen(
    onFinished: () -> Unit,
    onOpenGallery: () -> Unit,
    onCameraFallback: () -> Unit = onFinished,
    viewModel: ScannerViewModel = viewModel(
        factory = ScannerViewModel.Factory(
            LocalContext.current,
            LocalScanRepository.current,
        ),
    ),
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var scannerLaunched by remember { mutableStateOf(false) }

    val scannerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            viewModel.onScanResult(result.data)
        } else {
            viewModel.onScanCancelled()
        }
    }

    LaunchedEffect(activity) {
        if (scannerLaunched) return@LaunchedEffect
        if (activity == null) {
            viewModel.onScanStartFailed()
            return@LaunchedEffect
        }
        if (!DocumentScannerService.isAvailable(context)) {
            viewModel.onScanStartFailed()
            return@LaunchedEffect
        }
        scannerLaunched = true
        val scanner = DocumentScannerService(context).createScanner()
        scanner.getStartScanIntent(activity)
            .addOnSuccessListener { intentSender ->
                scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
            }
            .addOnFailureListener {
                viewModel.onScanStartFailed()
            }
    }

    LaunchedEffect(uiState.errorKey) {
        when (uiState.errorKey) {
            "scan_cancelled" -> {
                snackbarHostState.showSnackbar(context.getString(R.string.scan_cancelled))
                delay(1200)
                onFinished()
            }
            "scan_error" -> {
                snackbarHostState.showSnackbar(context.getString(R.string.scan_error))
                delay(1200)
                onFinished()
            }
            else -> Unit
        }
    }

    LaunchedEffect(uiState.statusMessage) {
        when {
            uiState.statusMessage == "scan_success" ->
                snackbarHostState.showSnackbar(context.getString(R.string.scan_success))
            uiState.statusMessage?.startsWith("scan_saved_phone:") == true -> {
                val path = uiState.statusMessage!!.removePrefix("scan_saved_phone:")
                snackbarHostState.showSnackbar(context.getString(R.string.scan_saved_to_phone, path))
            }
        }
    }

    when {
        uiState.pendingScan != null -> {
            ScanResultScreen(
                pending = uiState.pendingScan!!,
                onSaveToApp = viewModel::saveToApp,
                onSaveToPhone = viewModel::saveToPhone,
                onShare = { ShareHelperWrapper.share(context, uiState.pendingScan!!.file) },
                onOpenGallery = onOpenGallery,
                onClose = {
                    viewModel.clearPendingScan()
                    onFinished()
                },
            )
        }
        uiState.isProcessing -> {
            ScannerLoadingScreen(message = stringResource(R.string.scanning))
        }
        activity == null || !DocumentScannerService.isAvailable(context) -> {
            val gmsUnavailable = activity != null && !DocumentScannerService.isAvailable(context)
            Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(
                            if (gmsUnavailable) R.string.scanner_unavailable else R.string.scan_error,
                        ),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    if (gmsUnavailable) {
                        Text(
                            text = stringResource(R.string.scanner_unavailable_msg),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        Button(
                            onClick = { DocumentScannerService.openPlayServicesUpdate(context) },
                            modifier = Modifier.padding(top = 16.dp),
                        ) {
                            Text(stringResource(R.string.scanner_update))
                        }
                        Button(
                            onClick = onCameraFallback,
                            modifier = Modifier.padding(top = 8.dp),
                        ) {
                            Text(stringResource(R.string.scanner_camera_fallback))
                        }
                    }
                    Button(onClick = onFinished, modifier = Modifier.padding(top = 16.dp)) {
                        Text(stringResource(R.string.common_back))
                    }
                }
            }
        }
        else -> {
            Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun ScannerLoadingScreen(message: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

private object ShareHelperWrapper {
    fun share(context: android.content.Context, file: java.io.File) {
        com.truckerload.utils.ShareHelper(context).sharePdf(file)
    }
}
