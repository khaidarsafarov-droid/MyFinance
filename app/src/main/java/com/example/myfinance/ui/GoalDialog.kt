package com.example.myfinance.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.myfinance.data.Goal
import java.util.Calendar
import java.util.Locale

@Composable
fun GoalDialog(
    existingGoal: Goal?,
    onDismiss: () -> Unit,
    onSave: (targetAmount: Double, periodStart: String, periodEnd: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var targetText by rememberSaveable { mutableStateOf(existingGoal?.targetAmount?.toString() ?: "") }
    var periodStart by rememberSaveable { mutableStateOf(existingGoal?.periodStart ?: "") }
    var periodEnd by rememberSaveable { mutableStateOf(existingGoal?.periodEnd ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                if (existingGoal != null) "Edit goal" else "Set net profit goal",
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = targetText,
                onValueChange = { targetText = it },
                label = { Text("Target amount ($)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = {
                    val c = Calendar.getInstance(Locale.US)
                    periodStart = "%04d-%02d-01".format(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1)
                    c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH))
                    periodEnd = "%04d-%02d-%02d".format(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
                }) { Text("This month") }
                TextButton(onClick = {
                    val c = Calendar.getInstance(Locale.US)
                    c.add(Calendar.MONTH, -1)
                    periodStart = "%04d-%02d-01".format(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1)
                    c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH))
                    periodEnd = "%04d-%02d-%02d".format(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
                }) { Text("Last month") }
            }
            OutlinedTextField(
                value = periodStart,
                onValueChange = { periodStart = it },
                label = { Text("Period start (YYYY-MM-DD)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = periodEnd,
                onValueChange = { periodEnd = it },
                label = { Text("Period end (YYYY-MM-DD)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    val target = targetText.toDoubleOrNull() ?: return@Button
                    if (periodStart.isNotBlank() && periodEnd.isNotBlank()) {
                        onSave(target, periodStart.trim(), periodEnd.trim())
                        onDismiss()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save")
            }
        }
    }
}
