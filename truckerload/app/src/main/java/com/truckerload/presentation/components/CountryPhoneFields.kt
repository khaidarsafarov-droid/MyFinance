package com.truckerload.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.truckerload.R
import com.truckerload.domain.geo.CountryCatalog
import com.truckerload.domain.geo.CountryInfo
import com.truckerload.presentation.theme.AppTextFieldDefaults
import com.truckerload.presentation.theme.LocalTruckColors
import java.util.Locale

@Composable
fun CountryPickerField(
    selected: CountryInfo,
    onSelected: (CountryInfo) -> Unit,
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.home_country),
) {
    val isRussian = remember {
        Locale.getDefault().language.equals("ru", ignoreCase = true)
    }
    var showDialog by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = "${selected.displayName(isRussian)} (${selected.iso2})",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = AppTextFieldDefaults.outlined(),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable { showDialog = true },
        )
    }

    if (showDialog) {
        val filtered = remember(query) {
            val q = query.trim()
            if (q.isEmpty()) CountryCatalog.countries
            else CountryCatalog.countries.filter {
                it.nameEn.contains(q, ignoreCase = true) ||
                    it.nameRu.contains(q, ignoreCase = true) ||
                    it.iso2.contains(q, ignoreCase = true) ||
                    it.dialCode.contains(q.filter { ch -> ch.isDigit() })
            }
        }
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(label) },
            text = {
                Column {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text(stringResource(R.string.common_search)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = AppTextFieldDefaults.outlined(),
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp)
                            .padding(top = 8.dp),
                    ) {
                        items(filtered, key = { it.iso2 }) { country ->
                            Text(
                                text = "${country.displayName(isRussian)}  +${country.dialCode}",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSelected(country)
                                        showDialog = false
                                        query = ""
                                    }
                                    .padding(vertical = 12.dp, horizontal = 4.dp),
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}

@Composable
fun PhoneWithCountryField(
    country: CountryInfo,
    nationalNumber: String,
    onCountryChange: (CountryInfo) -> Unit,
    onNationalNumberChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.social_phone),
) {
    val tc = LocalTruckColors.current
    val isRussian = remember {
        Locale.getDefault().language.equals("ru", ignoreCase = true)
    }
    var showDialPicker by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = tc.TextSecondary, style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.weight(0.42f)) {
                OutlinedTextField(
                    value = "+${country.dialCode}",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.phone_country_code)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = AppTextFieldDefaults.outlined(),
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { showDialPicker = true },
                )
            }
            OutlinedTextField(
                value = nationalNumber,
                onValueChange = { onNationalNumberChange(it.filter { ch -> ch.isDigit() || ch == ' ' }) },
                label = { Text(stringResource(R.string.phone_national_number)) },
                modifier = Modifier.weight(0.58f),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                colors = AppTextFieldDefaults.outlined(),
            )
        }
    }

    if (showDialPicker) {
        val filtered = remember(query) {
            val q = query.trim()
            if (q.isEmpty()) CountryCatalog.countries
            else CountryCatalog.countries.filter {
                it.nameEn.contains(q, ignoreCase = true) ||
                    it.nameRu.contains(q, ignoreCase = true) ||
                    it.iso2.contains(q, ignoreCase = true) ||
                    it.dialCode.contains(q.filter { ch -> ch.isDigit() })
            }
        }
        AlertDialog(
            onDismissRequest = { showDialPicker = false },
            title = { Text(stringResource(R.string.phone_country_code)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = { Text(stringResource(R.string.common_search)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = AppTextFieldDefaults.outlined(),
                    )
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp)
                            .padding(top = 8.dp),
                    ) {
                        items(filtered, key = { it.iso2 + it.dialCode }) { item ->
                            Text(
                                text = "${item.displayName(isRussian)}  +${item.dialCode}",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onCountryChange(item)
                                        showDialPicker = false
                                        query = ""
                                    }
                                    .padding(vertical = 12.dp, horizontal = 4.dp),
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialPicker = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            },
        )
    }
}
