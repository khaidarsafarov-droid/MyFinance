package com.example.myfinance.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "logistics_tracker")

class AppRepository(private val context: Context) {

    private val gson = Gson()
    private val companiesKey = stringPreferencesKey("companies")
    private val loadsKey = stringPreferencesKey("loads") // legacy: migrate to weeklyTotals
    private val weeklyTotalsKey = stringPreferencesKey("weekly_totals")
    private val tripsKey = stringPreferencesKey("trips")
    private val companyChangesKey = stringPreferencesKey("company_changes")
    private val goalKey = stringPreferencesKey("goal")

    private val companiesType = object : TypeToken<List<Company>>() {}.type
    private val goalType = object : TypeToken<Goal>() {}.type
    private val weeklyTotalsType = object : TypeToken<List<WeeklyTotal>>() {}.type
    private val tripsType = object : TypeToken<List<Trip>>() {}.type
    private val companyChangesType = object : TypeToken<List<CompanyChange>>() {}.type
    private val legacyLoadsType = object : TypeToken<List<LegacyLoad>>() {}.type

    val appData: Flow<AppData> = context.dataStore.data.map { prefs -> loadAppData(prefs) }

    private fun <T> parseList(json: String?, type: java.lang.reflect.Type, default: () -> T): T {
        if (json.isNullOrBlank()) return default()
        return try {
            @Suppress("UNCHECKED_CAST")
            gson.fromJson<T>(json, type) as T
        } catch (_: Exception) {
            default()
        }
    }

    private fun <T> parseObj(json: String?, type: java.lang.reflect.Type, default: () -> T?): T? {
        if (json.isNullOrBlank()) return default()
        return try {
            @Suppress("UNCHECKED_CAST")
            gson.fromJson<T>(json, type) as T?
        } catch (_: Exception) {
            default()
        }
    }

    suspend fun addCompany(name: String, startDate: String? = null) {
        val id = generateId()
        context.dataStore.edit { prefs ->
            val current = loadAppData(prefs)
            val newCompanies = current.companies.map { it.copy(isCurrent = false) } +
                Company(id = id, name = name, isCurrent = true)
            val newChange = CompanyChange(
                id = generateId(),
                date = startDate ?: isoNow(),
                companyId = id,
                companyName = name
            )
            saveAppData(
                prefs,
                current.copy(
                    companies = newCompanies,
                    companyChanges = current.companyChanges + newChange
                )
            )
        }
    }

    suspend fun setCurrentCompany(companyId: String) {
        val company = appData.first().companies.find { it.id == companyId } ?: return
        context.dataStore.edit { prefs ->
            val current = loadAppData(prefs)
            val newCompanies = current.companies.map {
                it.copy(isCurrent = it.id == companyId)
            }
            val newChange = CompanyChange(
                id = generateId(),
                date = isoNow(),
                companyId = company.id,
                companyName = company.name
            )
            saveAppData(
                prefs,
                current.copy(
                    companies = newCompanies,
                    companyChanges = current.companyChanges + newChange
                )
            )
        }
    }

    suspend fun addWeeklyTotal(weeklyTotal: WeeklyTotal) {
        val withId = weeklyTotal.copy(id = generateId())
        context.dataStore.edit { prefs ->
            val current = loadAppData(prefs)
            saveAppData(prefs, current.copy(weeklyTotals = listOf(withId) + current.weeklyTotals))
        }
    }

    suspend fun updateWeeklyTotal(weeklyTotal: WeeklyTotal) {
        if (weeklyTotal.id.isBlank()) return
        context.dataStore.edit { prefs ->
            val current = loadAppData(prefs)
            val updated = current.weeklyTotals.map { if (it.id == weeklyTotal.id) weeklyTotal else it }
            if (updated.any { it.id == weeklyTotal.id }) saveAppData(prefs, current.copy(weeklyTotals = updated))
        }
    }

    suspend fun deleteWeeklyTotal(id: String) {
        context.dataStore.edit { prefs ->
            val current = loadAppData(prefs)
            saveAppData(prefs, current.copy(weeklyTotals = current.weeklyTotals.filter { it.id != id }))
        }
    }

    suspend fun addTrip(trip: Trip) {
        val withId = trip.copy(id = generateId())
        context.dataStore.edit { prefs ->
            val current = loadAppData(prefs)
            saveAppData(prefs, current.copy(trips = listOf(withId) + current.trips))
        }
    }

    suspend fun updateTrip(trip: Trip) {
        if (trip.id.isBlank()) return
        context.dataStore.edit { prefs ->
            val current = loadAppData(prefs)
            val updated = current.trips.map { if (it.id == trip.id) trip else it }
            if (updated.any { it.id == trip.id }) saveAppData(prefs, current.copy(trips = updated))
        }
    }

    suspend fun deleteTrip(id: String) {
        context.dataStore.edit { prefs ->
            val current = loadAppData(prefs)
            saveAppData(prefs, current.copy(trips = current.trips.filter { it.id != id }))
        }
    }

    suspend fun setGoal(goal: Goal) {
        context.dataStore.edit { prefs ->
            val current = loadAppData(prefs)
            saveAppData(prefs, current.copy(goal = goal))
        }
    }

    suspend fun clearGoal() {
        context.dataStore.edit { prefs ->
            val current = loadAppData(prefs)
            saveAppData(prefs, current.copy(goal = null))
        }
    }

    suspend fun markGoalNotified(goal: Goal) {
        context.dataStore.edit { prefs ->
            val current = loadAppData(prefs)
            if (current.goal?.id == goal.id) {
                saveAppData(prefs, current.copy(goal = goal.markNotified(isoNow())))
            }
        }
    }

    private fun loadAppData(prefs: Preferences): AppData {
        val companies = parseList(prefs[companiesKey], companiesType) { emptyList<Company>() }
        val companyChanges = parseList(prefs[companyChangesKey], companyChangesType) { emptyList<CompanyChange>() }
        var weeklyTotals = parseList(prefs[weeklyTotalsKey], weeklyTotalsType) { emptyList<WeeklyTotal>() }
        if (weeklyTotals.isEmpty()) {
            val legacy = parseList(prefs[loadsKey], legacyLoadsType) { emptyList<LegacyLoad>() }
            if (legacy.isNotEmpty()) {
                weeklyTotals = legacy.map { l ->
                    WeeklyTotal(
                        id = l.id,
                        date = l.date,
                        gross = l.gross,
                        miles = 0.0,
                        salaryIn = l.profit,
                        diesel = l.diesel,
                        companyIds = if (l.companyId.isNotBlank()) listOf(l.companyId) else emptyList()
                    )
                }
            }
        }
        val trips = parseList(prefs[tripsKey], tripsType) { emptyList<Trip>() }
        val goal = parseObj(prefs[goalKey], goalType) { null }
        return AppData(
            companies = companies,
            weeklyTotals = weeklyTotals,
            trips = trips,
            companyChanges = companyChanges,
            goal = goal
        )
    }

    private fun saveAppData(prefs: androidx.datastore.preferences.core.MutablePreferences, data: AppData) {
        prefs[companiesKey] = gson.toJson(data.companies)
        prefs[weeklyTotalsKey] = gson.toJson(data.weeklyTotals)
        prefs[tripsKey] = gson.toJson(data.trips)
        prefs[companyChangesKey] = gson.toJson(data.companyChanges)
        prefs[goalKey] = gson.toJson(data.goal)
    }

    private fun generateId(): String = "${System.currentTimeMillis()}-${java.util.UUID.randomUUID().toString().take(8)}"
    private fun isoNow(): String = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }.format(java.util.Date())
}

/** Legacy shape for migration from old "loads" (weekly totals). */
private data class LegacyLoad(
    val id: String,
    val date: String,
    val gross: Double,
    val profit: Double,
    val diesel: Double,
    val companyId: String
)
