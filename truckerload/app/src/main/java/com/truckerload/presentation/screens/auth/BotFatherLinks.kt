package com.truckerload.presentation.screens.auth

import android.content.Context
import android.content.Intent
import android.net.Uri

object BotFatherLinks {
    const val WEB_URL = "https://t.me/BotFather"
    const val APP_URL = "tg://resolve?domain=BotFather"

    fun open(context: Context) {
        val app = viewIntent(APP_URL)
        val web = viewIntent(WEB_URL)
        val started = runCatching { context.startActivity(app) }.isSuccess
        if (!started) {
            runCatching { context.startActivity(web) }
        }
    }

    private fun viewIntent(url: String): Intent =
        Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
}
