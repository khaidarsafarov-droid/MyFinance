package com.truckerload.sync

import com.truckerload.BuildConfig

enum class TelegramSyncMode {
    DEVICE,
    SERVER;

    companion object {
        fun resolve(value: String?): TelegramSyncMode =
            if (value?.trim()?.equals("server", ignoreCase = true) == true) SERVER else DEVICE

        fun current(): TelegramSyncMode = resolve(BuildConfig.TELEGRAM_SYNC_MODE)

        fun isServer(): Boolean = current() == SERVER
    }
}
