package com.truckerload.share

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.truckerload.BuildConfig
import com.truckerload.data.local.DatabaseStartup
import com.truckerload.data.preferences.AccountIds
import com.truckerload.data.preferences.AuthStore
import com.truckerload.data.share.PrefsShareImportIndex
import com.truckerload.di.UserComponentManager
import com.truckerload.presentation.MainActivity
import com.truckerload.presentation.theme.TruckerLoadTheme
import com.truckerload.utils.AppLocale
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class ShareReceiverActivity : AppCompatActivity() {

    @Inject lateinit var authStore: AuthStore

    @Inject lateinit var userComponentManager: UserComponentManager

    private var checking by mutableStateOf(true)
    private var message by mutableStateOf<ShareReceiverMessage?>(null)
    private var viewLoadId by mutableStateOf<String?>(null)
    private var importJob: Job? = null

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(AppLocale.wrap(base))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TruckerLoadTheme {
                ShareReceiverScreen(
                    checking = checking,
                    message = message,
                    onDone = { finish() },
                    onViewLoad = viewLoadId?.let { id -> { openExisting(id) } },
                )
            }
        }
        if (savedInstanceState != null) {
            show(ShareReceiverMessage.UNREADABLE)
            return
        }
        consume(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consume(intent)
    }

    private fun consume(intent: Intent?) {
        importJob?.cancel()
        checking = true
        message = null
        viewLoadId = null
        val inbound = ShareIntentReader.read(intent)
        if (inbound == null) {
            show(ShareReceiverMessage.UNREADABLE)
            return
        }
        importJob = lifecycleScope.launch {
            val outcome = try {
                import(inbound)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.w(TAG, "share import failed", error)
                ShareOutcome.Unreadable
            } ?: return@launch
            when (outcome) {
                is ShareOutcome.Added -> {
                    ShareImportBus.publish(
                        ShareImportNotice(loads = outcome.loads, kind = ShareNoticeKind.ADDED),
                    )
                    openLoads()
                }
                is ShareOutcome.Duplicate -> {
                    viewLoadId = outcome.loadId
                    show(ShareReceiverMessage.DUPLICATE)
                }
                ShareOutcome.Rejected -> show(ShareReceiverMessage.REJECTED)
                ShareOutcome.TooLarge -> show(ShareReceiverMessage.TOO_LARGE)
                ShareOutcome.Unreadable -> show(ShareReceiverMessage.UNREADABLE)
            }
        }
    }

    private suspend fun import(inbound: ShareInbound): ShareOutcome? {
        if (!DatabaseStartup.awaitReadyOrFalse()) {
            show(ShareReceiverMessage.UNREADABLE)
            return null
        }
        authStore.awaitReady()
        val userId = resolveUserId()
        if (userId == null) {
            show(ShareReceiverMessage.SIGN_IN)
            return null
        }
        return withContext(Dispatchers.IO) {
            userComponentManager.startSession(userId)
            val component = userComponentManager.require()
            ShareImportPipeline(
                context = applicationContext,
                loadRepository = component.loadRepository,
                scanRepository = component.scanRepository,
                index = PrefsShareImportIndex(applicationContext, component.userId),
            ).run(inbound)
        }
    }

    private fun resolveUserId(): String? {
        authStore.currentUserIdOrNull()?.let { return it }
        if (BuildConfig.LOCAL_ONLY_MODE) {
            authStore.login(AccountIds.LOCAL_DEV, "local@truckerload.local", rememberMe = true)
            return AccountIds.LOCAL_DEV
        }
        return null
    }

    private fun show(next: ShareReceiverMessage) {
        checking = false
        message = next
        if (next != ShareReceiverMessage.DUPLICATE) viewLoadId = null
    }

    private fun openLoads() {
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(MainActivity.EXTRA_ROUTE, "home")
            },
        )
        finish()
    }

    private fun openExisting(loadId: String) {
        ShareImportBus.publish(
            ShareImportNotice(
                loads = listOf(ShareAddedLoad(loadId, tripId = "", missing = emptyList())),
                kind = ShareNoticeKind.ALREADY,
            ),
        )
        openLoads()
    }

    private companion object {
        const val TAG = "ShareReceiver"
    }
}
