package com.example.myfinance.ui

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myfinance.data.AppData
import com.example.myfinance.data.Company
import com.example.myfinance.data.Goal
import com.example.myfinance.data.Trip
import com.example.myfinance.data.WeeklyTotal
import com.example.myfinance.calendar.CalendarHelper
import com.example.myfinance.data.AppRepository
import com.example.myfinance.gemini.ParsedTrip
import com.example.myfinance.gemini.ParsedWeeklyTotal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LogisticsViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = AppRepository(application.applicationContext)

    val appData = repo.appData.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppData()
    )

    val hasCompanies: Boolean
        get() = appData.value.companies.isNotEmpty()

    fun getCurrentCompany(): Company? = appData.value.companies.find { it.isCurrent }

    fun addCompany(name: String, startDate: String? = null) {
        viewModelScope.launch { repo.addCompany(name, startDate) }
    }

    fun setCurrentCompany(companyId: String) {
        viewModelScope.launch { repo.setCurrentCompany(companyId) }
    }

    /** Add a weekly total (manual or from AI). companyIds can be empty to use current company. */
    fun addWeeklyTotal(
        date: String,
        gross: Double,
        miles: Double,
        salaryIn: Double,
        diesel: Double,
        companyIds: List<String>
    ) {
        val ids = if (companyIds.isEmpty()) listOfNotNull(getCurrentCompany()?.id) else companyIds
        if (ids.isEmpty()) return
        val wt = WeeklyTotal(
            id = "",
            date = date,
            gross = gross,
            miles = miles,
            salaryIn = salaryIn,
            diesel = diesel,
            companyIds = ids
        )
        viewModelScope.launch { repo.addWeeklyTotal(wt) }
    }

    /** True if a weekly total with same date and same gross/salaryIn/diesel already exists. */
    fun findDuplicateWeeklyTotal(date: String, gross: Double, salaryIn: Double, diesel: Double): WeeklyTotal? {
        val dateNorm = date.take(10)
        return appData.value.weeklyTotals.find { wt ->
            wt.date.take(10) == dateNorm &&
                wt.gross == gross && wt.salaryIn == salaryIn && wt.diesel == diesel
        }
    }

    /** True if a trip with same date, pointA, pointB and cost already exists. */
    fun findDuplicateTrip(date: String, pointA: String, pointB: String, cost: Double): Trip? {
        val dateNorm = date.take(10)
        return appData.value.trips.find { t ->
            t.date.take(10) == dateNorm &&
                t.pointA.equals(pointA, ignoreCase = true) &&
                t.pointB.equals(pointB, ignoreCase = true) &&
                kotlin.math.abs(t.cost - cost) < 0.01
        }
    }

    /** @return true if added, false if duplicate skipped (use forceAdd = true to add anyway). */
    fun addWeeklyTotalFromParsed(parsed: ParsedWeeklyTotal, companies: List<Company>, forceAdd: Boolean = false): Boolean {
        val companyIds = if (parsed.companyNames.isEmpty()) {
            listOfNotNull(getCurrentCompany()?.id)
        } else {
            parsed.companyNames.mapNotNull { name ->
                companies.find { it.name.equals(name, ignoreCase = true) }?.id
            }.ifEmpty { listOfNotNull(getCurrentCompany()?.id) }
        }
        if (companyIds.isEmpty()) return false
        if (!forceAdd && findDuplicateWeeklyTotal(parsed.date, parsed.gross, parsed.salaryIn, parsed.diesel) != null)
            return false
        addWeeklyTotal(
            date = parsed.date,
            gross = parsed.gross,
            miles = parsed.miles,
            salaryIn = parsed.salaryIn,
            diesel = parsed.diesel,
            companyIds = companyIds
        )
        return true
    }

    fun deleteWeeklyTotal(id: String) {
        viewModelScope.launch { repo.deleteWeeklyTotal(id) }
    }

    /**
     * Add a trip. Call [onAdded] after save so UI can navigate back only when Loads list is updated.
     */
    fun addTrip(
        pointA: String,
        pointB: String,
        miles: Double,
        cost: Double,
        startTime: String,
        endTime: String,
        orderNumber: String,
        date: String,
        companyId: String? = null,
        onAdded: (() -> Unit)? = null
    ) {
        val trip = Trip(
            id = "",
            pointA = pointA,
            pointB = pointB,
            miles = miles,
            cost = cost,
            startTime = startTime,
            endTime = endTime,
            orderNumber = orderNumber,
            date = date.ifBlank { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date()) },
            companyId = companyId
        )
        viewModelScope.launch {
            repo.addTrip(trip) {
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    Toast.makeText(getApplication(), "Календарь: доступ запрещён", Toast.LENGTH_SHORT).show()
                }
            }
            onAdded?.let { withContext(Dispatchers.Main) { it() } }
        }
    }

    /** @return true if added, false if duplicate skipped (use forceAdd = true to add anyway). */
    fun addTripFromParsed(parsed: ParsedTrip, companyId: String? = null, forceAdd: Boolean = false): Boolean {
        if (!forceAdd && findDuplicateTrip(parsed.date, parsed.pointA, parsed.pointB, parsed.cost) != null)
            return false
        addTrip(
            pointA = parsed.pointA,
            pointB = parsed.pointB,
            miles = parsed.miles,
            cost = parsed.cost,
            startTime = parsed.startTime,
            endTime = parsed.endTime,
            orderNumber = parsed.orderNumber,
            date = parsed.date,
            companyId = companyId
        )
        return true
    }

    fun updateWeeklyTotal(
        id: String,
        date: String,
        gross: Double,
        miles: Double,
        salaryIn: Double,
        diesel: Double,
        companyIds: List<String>
    ) {
        val current = appData.value.weeklyTotals.find { it.id == id } ?: return
        val wt = current.copy(date = date, gross = gross, miles = miles, salaryIn = salaryIn, diesel = diesel, companyIds = companyIds.ifEmpty { current.companyIds })
        viewModelScope.launch { repo.updateWeeklyTotal(wt) }
    }

    fun updateTrip(
        id: String,
        pointA: String,
        pointB: String,
        miles: Double,
        cost: Double,
        startTime: String,
        endTime: String,
        orderNumber: String,
        date: String,
        companyId: String?
    ) {
        val current = appData.value.trips.find { it.id == id } ?: return
        val trip = current.copy(
            pointA = pointA, pointB = pointB, miles = miles, cost = cost,
            startTime = startTime, endTime = endTime, orderNumber = orderNumber, date = date, companyId = companyId
        )
        viewModelScope.launch { repo.updateTrip(trip) }
    }

    fun deleteTrip(id: String) {
        viewModelScope.launch { repo.deleteTrip(id) }
    }

    /** Manually add trip to calendar. Shows Toast on success/failure. */
    fun addTripToCalendar(trip: com.example.myfinance.data.Trip) {
        viewModelScope.launch {
            val ok = repo.addTripToCalendar(trip)
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                Toast.makeText(
                    getApplication(),
                    if (ok) "Добавлено в календарь" else "Календарь: доступ запрещён",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /** Sync all trips to device calendar. Returns count added. */
    fun syncAllTripsToCalendar(onResult: (Int) -> Unit) {
        viewModelScope.launch {
            val count = repo.syncAllTripsToCalendar()
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                Toast.makeText(
                    getApplication(),
                    when {
                        count > 0 -> "Добавлено в календарь: $count шт."
                        CalendarHelper.hasPermission(getApplication()) -> "Все рейсы уже в календаре"
                        else -> "Календарь: доступ запрещён"
                    },
                    Toast.LENGTH_SHORT
                ).show()
                onResult(count)
            }
        }
    }

    fun setGoal(targetAmount: Double, periodStart: String, periodEnd: String) {
        val goal = Goal(
            id = java.util.UUID.randomUUID().toString(),
            targetAmount = targetAmount,
            periodStart = periodStart,
            periodEnd = periodEnd
        )
        viewModelScope.launch { repo.setGoal(goal) }
    }

    fun updateGoal(targetAmount: Double, periodStart: String, periodEnd: String) {
        val current = appData.value.goal ?: return
        viewModelScope.launch {
            repo.setGoal(current.copy(targetAmount = targetAmount, periodStart = periodStart, periodEnd = periodEnd))
        }
    }

    fun clearGoal() {
        viewModelScope.launch { repo.clearGoal() }
    }

    fun markGoalNotified(goal: Goal) {
        viewModelScope.launch { repo.markGoalNotified(goal) }
    }
}
