package com.truckerload.presentation.screens.add

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.presentation.di.LocalAiRepository
import com.truckerload.presentation.di.LocalLoadRepository
import com.truckerload.presentation.theme.LocalTruckColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLoadScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit,
    onOptimisticInsert: ((com.truckerload.domain.model.Load) -> Unit)? = null,
    onRevertOptimistic: ((String) -> Unit)? = null
) {
    val tc = LocalTruckColors.current
    var rawText by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val loadRepository = LocalLoadRepository.current
    val aiRepository = LocalAiRepository.current

    Scaffold(
        containerColor = tc.Background,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_load_title), color = tc.TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back), tint = tc.TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = tc.Background,
                    titleContentColor = tc.TextPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = tc.CardBackground,
                border = androidx.compose.foundation.BorderStroke(1.dp, tc.Divider)
            ) {
                OutlinedTextField(
                    value = rawText,
                    onValueChange = { rawText = it; error = null },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    label = { Text(stringResource(R.string.add_load_input_label)) },
                    placeholder = { Text(stringResource(R.string.add_load_input_placeholder)) },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = tc.AccentPrimary,
                        unfocusedBorderColor = tc.Divider,
                        focusedLabelColor = tc.AccentPrimary,
                        unfocusedLabelColor = tc.TextSecondary,
                        focusedTextColor = tc.TextPrimary,
                        unfocusedTextColor = tc.TextPrimary,
                        focusedContainerColor = tc.CardBackground,
                        unfocusedContainerColor = tc.CardBackground,
                        cursorColor = tc.AccentPrimary
                    )
                )
            }
            if (aiRepository != null) {
                Button(
                    onClick = {
                        if (rawText.isBlank()) return@Button
                        isSaving = true
                        error = null
                        scope.launch {
                            aiRepository.parseLoadFromMessage(rawText)
                                .onSuccess { load ->
                                    onOptimisticInsert?.invoke(load)
                                    onSaved()
                                    CoroutineScope(Dispatchers.Default).launch {
                                        try {
                                            withContext(Dispatchers.IO) { loadRepository.insertLoad(load) }
                                        } catch (e: Exception) {
                                            withContext(Dispatchers.Main) {
                                                onRevertOptimistic?.invoke(load.id)
                                                android.widget.Toast.makeText(
                                                    context,
                                                    context.getString(R.string.common_save_error, e.message.orEmpty()),
                                                    android.widget.Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }
                                    }
                                }
                                .onFailure {
                                    error = it.message ?: context.getString(R.string.add_load_parse_failed)
                                    isSaving = false
                                }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = rawText.isNotBlank() && !isSaving
                ) {
                    Text(if (isSaving) stringResource(R.string.add_load_saving) else stringResource(R.string.add_load_save_offline))
                }
            }
            Text(
                if (aiRepository != null)
                    stringResource(R.string.add_load_hint_online)
                else
                    stringResource(R.string.add_load_hint_gemini),
                style = MaterialTheme.typography.bodySmall,
                color = tc.TextSecondary,
                modifier = Modifier.padding(top = 8.dp)
            )
            error?.let { Text(it, color = tc.AccentExpense, modifier = Modifier.padding(top = 8.dp)) }
        }
    }
}
