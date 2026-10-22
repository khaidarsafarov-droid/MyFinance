package com.truckerload.presentation.screens.add

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.truckerload.BuildConfig
import com.truckerload.data.preferences.AccountIds
import com.truckerload.data.preferences.AuthStore
import com.truckerload.di.UserComponentManager
import com.truckerload.presentation.MainActivity
import com.truckerload.presentation.theme.TruckerLoadTheme
import com.truckerload.utils.AppLocale
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class DieselQuickAddActivity : AppCompatActivity() {

    @Inject
    lateinit var authStore: AuthStore

    @Inject
    lateinit var userComponentManager: UserComponentManager

    private var imeBottomPx by mutableIntStateOf(0)
    private var imePinnedToTop = false

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(AppLocale.wrap(base))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setFinishOnTouchOutside(true)
        // Floating dialog windows ignore adjustResize. Track the IME and pin the
        // card to the top only while the keyboard is open.
        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { _, insets ->
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            if (imeBottomPx != ime) imeBottomPx = ime
            val params = window.attributes
            val topGravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            if (ime > 0 && !imePinnedToTop) {
                params.gravity = topGravity
                params.y = 0
                window.attributes = params
                imePinnedToTop = true
            } else if (ime == 0 && imePinnedToTop) {
                params.gravity = Gravity.CENTER
                params.y = 0
                window.attributes = params
                imePinnedToTop = false
            }
            insets
        }
        setContent {
            TruckerLoadTheme {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        }
        lifecycleScope.launch {
            authStore.awaitReady()
            val userId = resolveUserId()
            if (userId == null) {
                startActivity(
                    Intent(this@DieselQuickAddActivity, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    },
                )
                finish()
                return@launch
            }
            withContext(Dispatchers.IO) {
                userComponentManager.startSession(userId)
            }
            setContent {
                val imeBottom = with(LocalDensity.current) { imeBottomPx.toDp() }
                TruckerLoadTheme {
                    DieselQuickAddScreen(
                        onDone = { finish() },
                        onCancel = { finish() },
                        imeBottom = imeBottom,
                    )
                }
            }
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
}
