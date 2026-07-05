package com.example.myfinance.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import com.example.myfinance.data.Trip

@Composable
fun EditTripScreen(
    trip: Trip?,
    viewModel: LogisticsViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (trip == null) {
        onBack()
        return
    }
    var date by mutableStateOf(trip.date.take(10))
    var pointA by mutableStateOf(trip.pointA)
    var pointB by mutableStateOf(trip.pointB)
    var miles by mutableStateOf(trip.miles.toString())
    var cost by mutableStateOf(trip.cost.toString())
    var startTime by mutableStateOf(trip.startTime)
    var endTime by mutableStateOf(trip.endTime)
    var orderNumber by mutableStateOf(if (trip.orderNumber == "—") "" else trip.orderNumber)

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
            value = pointA,
            onValueChange = { pointA = it },
            label = { Text("Point A") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = pointB,
            onValueChange = { pointB = it },
            label = { Text("Point B") },
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
            value = cost,
            onValueChange = { cost = it },
            label = { Text("Cost") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = startTime,
            onValueChange = { startTime = it },
            label = { Text("Start time") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = endTime,
            onValueChange = { endTime = it },
            label = { Text("End time") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = orderNumber,
            onValueChange = { orderNumber = it },
            label = { Text("Order number") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    val m = miles.toDoubleOrNull() ?: 0.0
                    val c = cost.toDoubleOrNull() ?: 0.0
                    val updated = trip.copy(
                        pointA = pointA,
                        pointB = pointB,
                        miles = m,
                        cost = c,
                        startTime = startTime.ifBlank { "—" },
                        endTime = endTime.ifBlank { "—" },
                        orderNumber = orderNumber.ifBlank { "—" },
                        date = date,
                        companyId = trip.companyId
                    )
                    viewModel.addTripToCalendar(updated)
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                androidx.compose.material3.Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text(if (trip.calendarEventId != null) "Обновить в календаре" else "Добавить в календарь")
            }
            Button(
                onClick = {
                    val m = miles.toDoubleOrNull() ?: 0.0
                    val c = cost.toDoubleOrNull() ?: 0.0
                    viewModel.updateTrip(
                        id = trip.id,
                        pointA = pointA,
                        pointB = pointB,
                        miles = m,
                        cost = c,
                        startTime = startTime.ifBlank { "—" },
                        endTime = endTime.ifBlank { "—" },
                        orderNumber = orderNumber.ifBlank { "—" },
                        date = date,
                        companyId = trip.companyId
                )
                    onSaved()
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save changes")
            }
        }
    }
}
