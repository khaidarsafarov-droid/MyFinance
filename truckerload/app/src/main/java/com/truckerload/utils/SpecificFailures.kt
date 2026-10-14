package com.truckerload.utils

import android.content.ActivityNotFoundException
import android.database.sqlite.SQLiteException
import android.util.Log
import com.google.gson.JsonIOException
import com.google.gson.JsonSyntaxException
import com.truckerload.data.backup.GoogleDriveApiClient.DriveError
import com.truckerload.domain.import.model.ImportException
import kotlinx.coroutines.CancellationException
import org.json.JSONException
import java.io.IOException
import java.time.format.DateTimeParseException

/**
 * Handles the failures a screen or worker can show or retry.
 * [CancellationException] is rethrown. Any other exception propagates.
 */
inline fun <T> runReported(
    block: () -> T,
    onFailure: (Exception) -> T,
): T {
    try {
        return block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: ImportException) {
        return onFailure(e)
    } catch (e: DriveError) {
        return onFailure(e)
    } catch (e: SQLiteException) {
        return onFailure(e)
    } catch (e: IOException) {
        return onFailure(e)
    } catch (e: JSONException) {
        return onFailure(e)
    } catch (e: JsonSyntaxException) {
        return onFailure(e)
    } catch (e: JsonIOException) {
        return onFailure(e)
    } catch (e: DateTimeParseException) {
        return onFailure(e)
    } catch (e: ActivityNotFoundException) {
        return onFailure(e)
    } catch (e: SecurityException) {
        return onFailure(e)
    } catch (e: IllegalArgumentException) {
        return onFailure(e)
    } catch (e: IllegalStateException) {
        return onFailure(e)
    }
}

suspend inline fun <T> runReportedSuspending(
    block: suspend () -> T,
    onFailure: (Exception) -> T,
): T {
    try {
        return block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: ImportException) {
        return onFailure(e)
    } catch (e: DriveError) {
        return onFailure(e)
    } catch (e: SQLiteException) {
        return onFailure(e)
    } catch (e: IOException) {
        return onFailure(e)
    } catch (e: JSONException) {
        return onFailure(e)
    } catch (e: JsonSyntaxException) {
        return onFailure(e)
    } catch (e: JsonIOException) {
        return onFailure(e)
    } catch (e: DateTimeParseException) {
        return onFailure(e)
    } catch (e: ActivityNotFoundException) {
        return onFailure(e)
    } catch (e: SecurityException) {
        return onFailure(e)
    } catch (e: IllegalArgumentException) {
        return onFailure(e)
    } catch (e: IllegalStateException) {
        return onFailure(e)
    }
}

fun logFailure(tag: String, message: String, error: Throwable) {
    Log.e(tag, LogRedactor.redact(message), LogRedactor.redactThrowable(error))
}
