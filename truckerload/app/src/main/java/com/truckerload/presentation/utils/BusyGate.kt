package com.truckerload.presentation.utils
import android.database.sqlite.SQLiteException
import java.io.IOException


import com.truckerload.utils.logFailure
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * Runs [block] only if [busy] was false, so a double tap on Save/Delete cannot run twice.
 * [busy] is released even when [block] fails; failures are logged, cancellation is propagated.
 * Returns false when another action was already running.
 */
fun CoroutineScope.launchExclusive(
    busy: MutableStateFlow<Boolean>,
    tag: String,
    onError: (Exception) -> Unit = {},
    block: suspend () -> Unit,
): Boolean {
    if (!busy.compareAndSet(expect = false, update = true)) return false
    launch {
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: SQLiteException) {
            logFailure(tag, "action failed", e)
            onError(e)
        
        } catch (e: IOException) {
            logFailure(tag, "action failed", e)
            onError(e)
        
        } catch (e: IllegalArgumentException) {
            logFailure(tag, "action failed", e)
            onError(e)
        
        } catch (e: CancellationException) {
            throw e
        } catch (e: IllegalStateException) {
            logFailure(tag, "action failed", e)
            onError(e)
        
        } finally {
            busy.value = false
        }
    }
    return true
}
