package com.truckerload.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkerParameters
import com.truckerload.BuildConfig
import com.truckerload.R
import com.truckerload.data.local.AppDatabase
import com.truckerload.data.remote.AiService
import com.truckerload.data.remote.TelegramApi
import com.truckerload.data.repository.DieselRepository
import com.truckerload.data.repository.AiRepository
import com.truckerload.data.repository.LoadRepository
import com.truckerload.data.repository.PaycheckRepository
import com.truckerload.domain.model.Diesel
import com.truckerload.domain.model.Paycheck
import com.truckerload.data.repository.SyncLoadsResult
import com.truckerload.data.repository.SyncStatus
import com.truckerload.utils.formatDateFromUnixSeconds
import com.truckerload.utils.getWeekRange
import com.truckerload.utils.getWeekNumberAndYearFromDate
import com.truckerload.utils.getCurrentWeekNumberAndYear
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.WorkManager
import android.content.SharedPreferences
import android.util.Log
import java.util.concurrent.TimeUnit

/**
 * Fetches new Telegram messages (private chats and groups), parses with AI (load / paycheck / diesel), inserts into DB, replies in Telegram.
 * Uses last processed update_id from SharedPreferences so each message is processed once.
 * Loads are saved by message date: when the bot is in a group, each message's date is used as the load date so loads appear under the correct day.
 * To receive all group messages: add the bot to the group, then in BotFather run /setprivacy and choose "Disable" so the bot gets every message.
 */
class TelegramSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val PREFS_NAME = "telegram_sync"
        const val KEY_LAST_OFFSET = "last_update_offset"
        const val MAX_FILE_SIZE_BYTES = 20 * 1024 * 1024 // 20 MB
        private val IMAGE_MIMES = setOf("image/jpeg", "image/jpg", "image/png", "image/webp")
    }

    private fun String.contentHash(): Int = hashCode()

    override suspend fun doWork(): Result {
        val token = BuildConfig.TELEGRAM_BOT_TOKEN
        val cerebrasKey = BuildConfig.CEREBRAS_API_KEY
        if (token.isBlank() || cerebrasKey.isBlank()) {
            Log.w("TelegramSync", "Skipping: token or cerebrasKey blank")
            return Result.success()
        }

        val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastOffset = prefs.getLong(KEY_LAST_OFFSET, 0L)
        Log.d("TelegramSync", "Starting sync, lastOffset=$lastOffset")

        val telegramApi = TelegramApi(token)
        telegramApi.deleteWebhook().onFailure { e ->
            Log.w("TelegramSync", "deleteWebhook failed (webhook may not be set): ${e.message}")
        }
        val cerebrasModel = BuildConfig.CEREBRAS_MODEL
        val aiService = AiService(cerebrasKey, cerebrasModel, applicationContext)
        val aiRepository = AiRepository(aiService)
        val db = AppDatabase.getInstance(applicationContext)
        val loadRepository = LoadRepository(db)
        val paycheckRepository = PaycheckRepository(db)
        val dieselRepository = DieselRepository(db)

        val getResult = telegramApi.getUpdates(offset = if (lastOffset > 0) lastOffset else null, timeoutSeconds = 2)
        val result = getResult.getOrElse { e ->
            Log.e("TelegramSync", "getUpdates failed: ${e.message}", e)
            return Result.failure()
        }
        var nextOffset = result.nextOffset
        Log.d("TelegramSync", "getUpdates OK, updates=${result.updates.size}, nextOffset=$nextOffset")

        for (update in result.updates) {
            // Мгновенный ответ — AI-парсинг может занять несколько секунд
            telegramApi.sendMessage(
                update.chatId,
                applicationContext.getString(R.string.sync_processing)
            ).onFailure { e ->
                Log.e("TelegramSync", "sendMessage (ack) failed: ${e.message}", e)
            }

            var textToProcess = update.text.trim()
            if (textToProcess.isBlank()) {
                val fileText = when {
                    update.photoFileId != null -> {
                        telegramApi.sendMessage(update.chatId, applicationContext.getString(R.string.sync_ocr_disabled))
                        null
                    }
                    update.documentFileId != null && update.documentMimeType != null && IMAGE_MIMES.contains(update.documentMimeType) -> {
                        telegramApi.sendMessage(update.chatId, applicationContext.getString(R.string.sync_ocr_disabled))
                        null
                    }
                    update.documentFileId != null -> {
                        telegramApi.sendMessage(update.chatId, applicationContext.getString(R.string.sync_doc_not_supported))
                        null
                    }
                    else -> null
                } ?: continue
                textToProcess = fileText
            }
            if (textToProcess.isBlank()) continue

            val reply = processMessage(
                text = textToProcess,
                messageDateSeconds = update.messageDateSeconds,
                aiRepository = aiRepository,
                loadRepository = loadRepository,
                paycheckRepository = paycheckRepository,
                dieselRepository = dieselRepository,
                prefs = prefs
            )
            telegramApi.sendMessage(update.chatId, reply).onFailure { e ->
                Log.e("TelegramSync", "sendMessage failed: ${e.message}", e)
            }
        }

        prefs.edit().putLong(KEY_LAST_OFFSET, nextOffset).apply()

        // Reschedule next sync in 1 min (Offline-first: быстрее подхватываем новые сообщения)
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
        val nextWork = OneTimeWorkRequestBuilder<TelegramSyncWorker>()
            .setConstraints(constraints)
            .setInitialDelay(1, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(applicationContext).enqueue(nextWork)

        return Result.success()
    }

    private suspend fun processMessage(
        text: String,
        messageDateSeconds: Long?,
        aiRepository: AiRepository,
        loadRepository: LoadRepository,
        paycheckRepository: PaycheckRepository,
        dieselRepository: DieselRepository,
        prefs: SharedPreferences
    ): String {
        // Сценарий В: Пустое сообщение
        if (text.isBlank()) {
            return applicationContext.getString(R.string.sync_no_new_data)
        }

        // Try loads first (relay/trip messages) — CDC: один запрос на проверку Trip ID, batch insert
        aiRepository.parseLoadsFromMessage(text)
            .onSuccess { incomingLoads ->
                if (incomingLoads.isNotEmpty()) {
                    val result = loadRepository.syncLoadsCdc(incomingLoads, messageDateSeconds)
                    if (result.status == SyncStatus.SUCCESS) {
                        Log.d("TelegramSync", "CDC: inserted ${result.addedCount} loads, last=${result.lastAddedText}")
                    }
                    return when (result.status) {
                        SyncStatus.SUCCESS -> applicationContext.getString(
                            R.string.sync_added_loads,
                            result.addedCount,
                            result.lastAddedText
                        )
                        SyncStatus.DUPLICATE -> applicationContext.getString(R.string.sync_duplicate_loads)
                        SyncStatus.EMPTY -> applicationContext.getString(R.string.sync_no_new_data)
                    }
                }
            }

        // Try paycheck.
        aiRepository.parsePaycheckFromText(text)
            .onSuccess { r ->
                val (weekNumber, year) = if (messageDateSeconds != null && r.weekStartDate.isNullOrBlank())
                    getWeekNumberAndYearFromDate(formatDateFromUnixSeconds(messageDateSeconds))
                else
                    getWeekNumberAndYearFromDate(r.weekStartDate)
                if (r.netAmount <= 0) {
                    return applicationContext.getString(R.string.sync_paycheck_not_found)
                }
                if (paycheckRepository.getPaycheckForWeek(weekNumber, year) != null) {
                    return applicationContext.getString(R.string.sync_paycheck_exists, weekNumber)
                }
                val (weekStart, weekEnd, weekLabel) = getWeekRange(weekNumber, year)
                val paycheck = Paycheck(
                    id = 0,
                    weekNumber = weekNumber,
                    year = year,
                    weekLabel = weekLabel,
                    weekStartDate = weekStart,
                    weekEndDate = weekEnd,
                    driverName = r.driverName,
                    grossAmount = r.grossAmount,
                    netAmount = r.netAmount,
                    rawExtractedText = text,
                    sourceFileName = null,
                    addedAt = System.currentTimeMillis()
                )
                paycheckRepository.insertPaycheck(paycheck)
                return applicationContext.getString(
                    R.string.sync_last_paycheck,
                    String.format("%,.2f", r.netAmount),
                    weekNumber
                )
            }

        // Try diesel.
        aiRepository.parseDieselFromText(text)
            .onSuccess { r ->
                val dateForWeek = when {
                    messageDateSeconds != null && r.date.isNullOrBlank() -> formatDateFromUnixSeconds(messageDateSeconds)
                    else -> r.date
                }
                val (weekNumber, year) = getWeekNumberAndYearFromDate(dateForWeek)
                if (r.totalAmount <= 0) {
                    return applicationContext.getString(R.string.sync_diesel_not_found)
                }
                val textHash = text.contentHash()
                val lastDieselHash = prefs.getInt("last_diesel_text_hash", 0)
                if (lastDieselHash != 0 && lastDieselHash == textHash) {
                    return applicationContext.getString(R.string.sync_duplicate_diesel)
                }
                val (weekStart, weekEnd, weekLabel) = getWeekRange(weekNumber, year)
                val diesel = Diesel(
                    id = 0,
                    weekNumber = weekNumber,
                    year = year,
                    weekLabel = weekLabel,
                    weekStartDate = weekStart,
                    weekEndDate = weekEnd,
                    totalAmount = r.totalAmount,
                    gallons = r.gallons,
                    pricePerGallon = r.pricePerGallon,
                    location = r.location,
                    rawExtractedText = text,
                    sourceFileName = null,
                    addedAt = System.currentTimeMillis()
                )
                dieselRepository.insertDiesel(diesel)
                prefs.edit().putInt("last_diesel_text_hash", textHash).apply()
                return applicationContext.getString(
                    R.string.sync_last_diesel,
                    String.format("%,.2f", r.totalAmount),
                    weekNumber
                )
            }

        return applicationContext.getString(R.string.sync_parse_failed)
    }
}
