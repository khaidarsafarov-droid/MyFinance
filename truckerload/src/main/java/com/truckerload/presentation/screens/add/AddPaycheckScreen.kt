package com.truckerload.presentation.screens.add

import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.truckerload.domain.model.Paycheck
import com.truckerload.presentation.di.LocalPaycheckRepository
import com.truckerload.utils.formatDateTimeForDisplay
import com.truckerload.utils.getWeekNumberAndYearFromTimestamp
import com.truckerload.utils.getWeekRange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPaycheckScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit
) {
    val paycheckRepository = LocalPaycheckRepository.current
    val scope = rememberCoroutineScope()
    val (initialWeek, initialYear) = com.truckerload.utils.getCurrentWeekNumberAndYear()
    var weekNumber by remember { mutableStateOf(initialWeek) }
    var year by remember { mutableStateOf(initialYear) }
    var netAmount by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var recordedAtMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val (_, _, weekLabel) = getWeekRange(weekNumber, year)
    val (weekStart, weekEnd, _) = getWeekRange(weekNumber, year)

    if (showDatePicker) {
        val cal = Calendar.getInstance().apply { timeInMillis = recordedAtMillis }
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = recordedAtMillis,
            yearRange = IntRange(cal.get(Calendar.YEAR) - 2, cal.get(Calendar.YEAR) + 1)
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let { ms ->
                        val c = Calendar.getInstance().apply { timeInMillis = recordedAtMillis }
                        val c2 = Calendar.getInstance().apply { timeInMillis = ms }
                        c.set(Calendar.YEAR, c2.get(Calendar.YEAR))
                        c.set(Calendar.MONTH, c2.get(Calendar.MONTH))
                        c.set(Calendar.DAY_OF_MONTH, c2.get(Calendar.DAY_OF_MONTH))
                        recordedAtMillis = c.timeInMillis
                    }
                    showDatePicker = false
                    showTimePicker = true
                }) { Text("OK") }
            }
        ) { DatePicker(state = dateState) }
    }
    if (showTimePicker) {
        val cal = Calendar.getInstance().apply { timeInMillis = recordedAtMillis }
        val timeState = rememberTimePickerState(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE),
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val c = Calendar.getInstance().apply { timeInMillis = recordedAtMillis }
                    c.set(Calendar.HOUR_OF_DAY, timeState.hour)
                    c.set(Calendar.MINUTE, timeState.minute)
                    recordedAtMillis = c.timeInMillis
                    showTimePicker = false
                }) { Text("OK") }
            },
            text = { TimePicker(state = timeState) }
        )
    }
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Сохранить запись") },
            text = {
                Column {
                    Text("Дата и время записи:", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .clickable { showDatePicker = true },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(formatDateTimeForDisplay(recordedAtMillis), style = MaterialTheme.typography.titleMedium)
                        IconButton(onClick = { showDatePicker = true }) { Icon(Icons.Default.Edit, contentDescription = "Изменить") }
                    }
                    if (error != null) {
                        Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val amount = netAmount.toDoubleOrNull() ?: 0.0
                    if (amount <= 0) return@Button
                    val (w, y) = getWeekNumberAndYearFromTimestamp(recordedAtMillis)
                    val (ws, we, wl) = getWeekRange(w, y)
                    val paycheck = Paycheck(0, w, y, wl, ws, we, null, null, amount, "", null, recordedAtMillis)
                    scope.launch {
                        try {
                            withContext(Dispatchers.IO) {
                                paycheckRepository.insertPaycheck(paycheck)
                            }
                            showSaveDialog = false
                            onSaved()
                        } catch (e: Exception) {
                            error = e.message ?: "Ошибка сохранения"
                        }
                    }
                }) { Text("Сохранить") }
            },
            dismissButton = { TextButton(onClick = { showSaveDialog = false }) { Text("Отмена") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Добавить зарплату") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text("📅 Выберите неделю", style = MaterialTheme.typography.titleSmall)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                IconButton(onClick = { if (weekNumber > 1) weekNumber-- else { weekNumber = 52; year-- } }) { Icon(Icons.Default.KeyboardArrowLeft, contentDescription = null) }
                Text("Week $weekNumber • $weekLabel", style = MaterialTheme.typography.bodyMedium)
                IconButton(onClick = { if (weekNumber < 52) weekNumber++ else { weekNumber = 1; year++ } }) { Icon(Icons.Default.KeyboardArrowRight, contentDescription = null) }
            }
            OutlinedTextField(
                value = netAmount,
                onValueChange = { netAmount = it },
                label = { Text("Введите сумму") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            Button(onClick = {
                val amount = netAmount.toDoubleOrNull() ?: 0.0
                if (amount <= 0) return@Button
                error = null
                recordedAtMillis = System.currentTimeMillis()
                showSaveDialog = true
            }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Сохранить") }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
        }
    }
}
