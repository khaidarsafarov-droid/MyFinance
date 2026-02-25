package com.example.myfinance.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import com.example.myfinance.data.WeeklyTotal

@Composable
fun EditWeeklyTotalScreen(
    weeklyTotal: WeeklyTotal?,
    viewModel: LogisticsViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (weeklyTotal == null) {
        onBack()
        return
    }
    var date by mutableStateOf(weeklyTotal.date.take(10))
    var gross by mutableStateOf(weeklyTotal.gross.toString())
    var miles by mutableStateOf(weeklyTotal.miles.toString())
    var salaryIn by mutableStateOf(weeklyTotal.salaryIn.toString())
    var diesel by mutableStateOf(weeklyTotal.diesel.toString())

    Column(
        modifier = modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        OutlinedTextField(
            value = date,
            onValueChange = { date = it },
            label = { Text("Date (yyyy-MM-dd)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = gross,
            onValueChange = { gross = it },
            label = { Text("Gross") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = miles,
            onValueChange = { miles = it },
            label = { Text("Miles") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = salaryIn,
            onValueChange = { salaryIn = it },
            label = { Text("Salary came in") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = diesel,
            onValueChange = { diesel = it },
            label = { Text("Diesel") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                val g = gross.toDoubleOrNull() ?: 0.0
                val m = miles.toDoubleOrNull() ?: 0.0
                val s = salaryIn.toDoubleOrNull() ?: 0.0
                val d = diesel.toDoubleOrNull() ?: 0.0
                viewModel.updateWeeklyTotal(
                    id = weeklyTotal.id,
                    date = date,
                    gross = g,
                    miles = m,
                    salaryIn = s,
                    diesel = d,
                    companyIds = weeklyTotal.companyIds
                )
                onSaved()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Save changes")
        }
    }
}
