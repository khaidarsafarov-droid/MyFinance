package com.truckerload.presentation.screens.add

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.truckerload.presentation.components.TlButton as Button
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
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.truckerload.R
import com.truckerload.presentation.di.LocalAiRepository
import com.truckerload.presentation.di.LocalLoadRepository
import com.truckerload.presentation.theme.AppTextFieldDefaults
import com.truckerload.presentation.components.PickupAlarmDialog
import com.truckerload.presentation.theme.BentoGlassCard
import com.truckerload.presentation.theme.BentoGlassTheme
import com.truckerload.presentation.theme.LocalTruckColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLoadScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit,
    onOptimisticInsert: ((com.truckerload.domain.model.Load) -> Unit)? = null,
    onRevertOptimistic: ((String) -> Unit)? = null,
) {
    val tc = LocalTruckColors.current
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val loadRepository = LocalLoadRepository.current
    val aiRepository = LocalAiRepository.current
    val viewModel: AddLoadViewModel = viewModel(
        factory = AddLoadViewModel.Factory(application, loadRepository, aiRepository),
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val parseFailed = stringResource(R.string.add_load_parse_failed)

    LaunchedEffect(uiState.savedLoad, uiState.alarmPrompt) {
        if (uiState.savedLoad != null && uiState.alarmPrompt == null) {
            viewModel.clearSaved()
            onSaved()
        }
    }

    uiState.alarmPrompt?.let { prompt ->
        PickupAlarmDialog(
            prompt = prompt,
            onDismiss = {
                viewModel.clearAlarmPrompt()
                viewModel.clearSaved()
                onSaved()
            },
        )
    }

    Scaffold(
        containerColor = BentoGlassTheme.ScreenBackground,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_load_title), color = tc.TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                            tint = tc.TextPrimary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BentoGlassTheme.ScreenBackground,
                    titleContentColor = tc.TextPrimary,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BentoGlassCard(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = uiState.rawText,
                    onValueChange = viewModel::setRawText,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .padding(12.dp),
                    label = { Text(stringResource(R.string.add_load_input_label)) },
                    placeholder = { Text(stringResource(R.string.add_load_input_placeholder)) },
                    shape = RoundedCornerShape(BentoGlassTheme.CellRadius),
                    colors = AppTextFieldDefaults.outlined(),
                )
            }
            Button(
                onClick = {
                    viewModel.save(
                        parseFailedFallback = parseFailed,
                        saveErrorFormatter = { msg ->
                            context.getString(R.string.common_save_error, msg)
                        },
                        onOptimisticInsert = onOptimisticInsert,
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = uiState.rawText.isNotBlank() && !uiState.isSaving && aiRepository != null,
            ) {
                Text(
                    if (uiState.isSaving) {
                        stringResource(R.string.add_load_saving)
                    } else {
                        stringResource(R.string.add_load_save_offline)
                    },
                )
            }
            Text(
                stringResource(R.string.add_load_hint_online),
                style = MaterialTheme.typography.bodySmall,
                color = tc.TextSecondary,
                modifier = Modifier.padding(top = 8.dp),
            )
            uiState.error?.let {
                Text(it, color = tc.AccentExpense, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}
