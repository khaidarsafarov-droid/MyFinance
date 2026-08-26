package com.truckerload.presentation.screens.scanner

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.domain.model.ScanDocumentCategory
import com.truckerload.presentation.icons.AppIcons
import com.truckerload.presentation.theme.LocalTruckColors
import com.truckerload.utils.ClipboardUtils
import com.truckerload.utils.PDFGenerator
import com.truckerload.utils.PhotoManager
import com.truckerload.utils.ocr.LanguageDetector
import com.truckerload.presentation.components.TlButton as Button
import com.truckerload.presentation.components.TlOutlinedButton as OutlinedButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanResultScreen(
    pending: PendingScan,
    sessionCount: Int = 1,
    isAttachedToLoad: Boolean = false,
    onSaveToApp: () -> Unit,
    onSaveToPhone: () -> Unit,
    onShare: () -> Unit,
    onAddAnother: () -> Unit,
    onOpenGallery: () -> Unit,
    onCategoryChange: (ScanDocumentCategory) -> Unit = {},
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val tc = LocalTruckColors.current
    val scroll = rememberScrollState()
    var ocrExpanded by remember(pending.timestamp, pending.file.absolutePath) {
        mutableStateOf(false)
    }
    val hasOcr = pending.ocrText.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.scan_results)) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            AppIcons.ArrowBack,
                            contentDescription = stringResource(R.string.common_back),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(scroll),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = pending.file.name,
                style = MaterialTheme.typography.titleMedium,
                color = tc.TextPrimary,
            )
            Text(
                text = stringResource(R.string.scan_pages, pending.pageCount),
                style = MaterialTheme.typography.bodyMedium,
                color = tc.TextSecondary,
            )
            if (sessionCount > 1) {
                Text(
                    text = stringResource(R.string.scan_session_count, sessionCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = tc.AccentPrimary,
                )
            }
            Text(
                text = stringResource(
                    R.string.scan_size,
                    PDFGenerator.formatFileSize(pending.file.length()),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = tc.TextSecondary,
            )
            Text(
                text = PhotoManager.formatDateTime(pending.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = tc.TextSecondary,
            )

            if (isAttachedToLoad && !pending.savedToDb) {
                Text(
                    text = stringResource(R.string.scan_send_then_save_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = tc.AccentPrimary,
                )
            }

            Text(
                text = stringResource(R.string.scan_category_pick),
                style = MaterialTheme.typography.titleSmall,
                color = tc.TextPrimary,
            )
            ScanCategoryPickerChips(
                selected = pending.category,
                onSelect = onCategoryChange,
                enabled = !pending.savedToDb,
            )

            Text(
                text = stringResource(R.string.ocr_text),
                style = MaterialTheme.typography.titleSmall,
                color = tc.TextPrimary,
            )
            if (!hasOcr) {
                Text(
                    text = stringResource(R.string.no_text_found),
                    style = MaterialTheme.typography.bodyMedium,
                    color = tc.TextSecondary,
                )
            } else {
                val detectedLang = remember(pending.ocrText) {
                    LanguageDetector.detect(pending.ocrText)
                }
                Text(
                    text = when (detectedLang) {
                        "ru" -> stringResource(R.string.russian_text)
                        "en" -> stringResource(R.string.english_text)
                        else -> if (pending.usedRussianEngine) {
                            stringResource(R.string.ocr_russian_detected)
                        } else {
                            stringResource(R.string.ocr_mixed_text)
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = when (detectedLang) {
                        "ru" -> tc.AccentProfit
                        "en" -> tc.AccentPrimary
                        else -> tc.TextSecondary
                    },
                )
                TextButton(
                    onClick = { ocrExpanded = !ocrExpanded },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = stringResource(
                                if (ocrExpanded) R.string.ocr_hide_text else R.string.ocr_show_text,
                            ),
                            color = tc.AccentPrimary,
                        )
                        Icon(
                            imageVector = if (ocrExpanded) {
                                AppIcons.ExpandLess
                            } else {
                                AppIcons.ExpandMore
                            },
                            contentDescription = null,
                            tint = tc.AccentPrimary,
                        )
                    }
                }
                AnimatedVisibility(visible = ocrExpanded) {
                    Text(
                        text = pending.ocrText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = tc.TextSecondary,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
                OutlinedButton(
                    onClick = { ClipboardUtils.copyTextWithToast(context, pending.ocrText) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.copy_text))
                }
            }

            if (isAttachedToLoad) {
                Button(
                    onClick = onShare,
                    enabled = !pending.savedToDb,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = tc.AccentSecondary),
                ) {
                    Text(
                        if (sessionCount > 1) {
                            stringResource(R.string.scan_share_all)
                        } else {
                            stringResource(R.string.send_to)
                        },
                    )
                }
                OutlinedButton(
                    onClick = onSaveToApp,
                    enabled = !pending.savedToDb,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        stringResource(
                            if (pending.savedToDb) {
                                R.string.scan_already_saved
                            } else {
                                R.string.save_to_load_without_share
                            },
                        ),
                    )
                }
                OutlinedButton(
                    onClick = onAddAnother,
                    enabled = !pending.savedToDb,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.scan_add_another))
                }
            } else {
                Button(
                    onClick = onSaveToApp,
                    enabled = !pending.savedToDb,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = tc.AccentProfit),
                ) {
                    Text(
                        stringResource(
                            if (pending.savedToDb) R.string.scan_already_saved else R.string.save_to_app,
                        ),
                    )
                }
                OutlinedButton(
                    onClick = onAddAnother,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.scan_add_another))
                }
                Button(
                    onClick = onSaveToPhone,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = tc.AccentPrimary),
                ) {
                    Text(stringResource(R.string.save_to_phone))
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = onShare,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = tc.AccentSecondary),
                    ) {
                        Text(
                            if (sessionCount > 1) {
                                stringResource(R.string.scan_share_all)
                            } else {
                                stringResource(R.string.send_to)
                            },
                        )
                    }
                    OutlinedButton(
                        onClick = onOpenGallery,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.scans_gallery))
                    }
                }
            }
            OutlinedButton(
                onClick = onClose,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    stringResource(
                        if (pending.savedToDb) R.string.common_done else R.string.common_cancel,
                    ),
                )
            }
        }
    }
}
