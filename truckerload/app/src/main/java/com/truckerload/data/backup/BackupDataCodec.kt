package com.truckerload.data.backup

import com.google.gson.Gson
import com.google.gson.GsonBuilder

/** Pure JSON codec for [BackupData] — unit-testable without Room/Android. */
object BackupDataCodec {
    val gson: Gson = GsonBuilder().create()

    fun toJson(data: BackupData): String = gson.toJson(data)

    fun fromJson(json: String): BackupData? =
        runCatching { gson.fromJson(json, BackupData::class.java) }.getOrNull()
            ?.takeIf { it.version >= 1 }
}
