package com.truckerload.data.sync

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.truckerload.R
import com.truckerload.contract.DeviceSlotPolicy
import com.truckerload.data.preferences.AuthStore
import java.io.IOException

class DeviceSlotTakenException(
    val formFactor: String,
    message: String,
) : IOException(message)

sealed class DeviceSlotResult {
    data object Allowed : DeviceSlotResult()
    data object Skipped : DeviceSlotResult()
    data class SlotTaken(val formFactor: String) : DeviceSlotResult()
    data class Unavailable(val cause: String) : DeviceSlotResult()
}

/**
 * Binds an account to one phone and one tablet on the sync backend.
 * No-op when the cloud backend is not configured.
 */
class DeviceSlotBinder(context: Context) {
    private val app = context.applicationContext

    suspend fun registerWithAccessToken(
        accessToken: String,
        replaceOccupant: Boolean = false,
    ): DeviceSlotResult {
        val client = AccountCloudBackendFactory.remoteClientOrNull(app) ?: return DeviceSlotResult.Skipped
        if (accessToken.isBlank()) return DeviceSlotResult.Skipped
        val identity = DeviceIdentity(app)
        return try {
            client.registerDevice(identity.id(), identity.formFactor(), replaceOccupant)
            DeviceSlotResult.Allowed
        } catch (error: DeviceSlotTakenException) {
            DeviceSlotResult.SlotTaken(error.formFactor)
        } catch (error: Exception) {
            Log.w(TAG, "Device registration failed (login continues)", error)
            DeviceSlotResult.Unavailable(error.message ?: "register_failed")
        }
    }

    suspend fun registerCurrentDevice(
        required: Boolean,
        replaceOccupant: Boolean = false,
    ): DeviceSlotResult {
        val token = AuthStore(app).accessTokenOrNull()
        if (token.isNullOrBlank()) {
            return DeviceSlotResult.Skipped
        }
        return registerWithAccessToken(token, replaceOccupant)
    }

    suspend fun unregisterCurrentDevice() {
        if (AuthStore(app).accessTokenOrNull().isNullOrBlank()) return
        val client = AccountCloudBackendFactory.remoteClientOrNull(app) ?: return
        runCatching { client.unregisterDevice() }
        runCatching { client.deletePushToken() }
    }

    fun userMessage(result: DeviceSlotResult.SlotTaken): String =
        app.getString(
            if (result.formFactor == DeviceSlotPolicy.TABLET) {
                R.string.auth_error_device_slot_tablet
            } else {
                R.string.auth_error_device_slot_phone
            },
        )

    fun userMessageUnavailable(): String =
        app.getString(R.string.auth_error_device_slot_unavailable)

    companion object {
        private const val TAG = "DeviceSlotBinder"
    }
}

/** One-shot message shown on the login screen after a bound session is kicked off. */
class DeviceSlotDenialStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun save(message: String) {
        prefs.edit { putString(KEY_MESSAGE, message) }
    }

    fun consume(): String? {
        val message = prefs.getString(KEY_MESSAGE, null)?.takeIf { it.isNotBlank() }
        if (message != null) prefs.edit { remove(KEY_MESSAGE) }
        return message
    }

    companion object {
        private const val PREFS = "truckerload_device_slot_denial"
        private const val KEY_MESSAGE = "message"
    }
}

object DeviceSlotLogin {
    /**
     * Registers this device before the session is persisted so a slot conflict
     * never flashes the main shell.
     */
    suspend fun beforeSessionPersisted(
        context: Context,
        accessToken: String?,
        replaceOccupant: Boolean = false,
    ): Result<Unit> {
        val token = accessToken?.takeIf { it.isNotBlank() } ?: return Result.success(Unit)
        val binder = DeviceSlotBinder(context)
        return when (val result = binder.registerWithAccessToken(token, replaceOccupant)) {
            DeviceSlotResult.Allowed, DeviceSlotResult.Skipped, is DeviceSlotResult.Unavailable ->
                Result.success(Unit)
            is DeviceSlotResult.SlotTaken ->
                Result.failure(
                    DeviceSlotTakenException(
                        formFactor = result.formFactor,
                        message = binder.userMessage(result),
                    ),
                )
        }
    }

    /** Post-login refresh (e.g. legacy Google launcher path). Never logs the user out. */
    suspend fun afterSessionPersisted(context: Context, authStore: AuthStore): Result<Unit> {
        val binder = DeviceSlotBinder(context)
        return when (val result = binder.registerCurrentDevice(required = false)) {
            DeviceSlotResult.Allowed, DeviceSlotResult.Skipped, is DeviceSlotResult.Unavailable ->
                Result.success(Unit)
            is DeviceSlotResult.SlotTaken -> {
                authStore.logout()
                Result.failure(
                    DeviceSlotTakenException(
                        formFactor = result.formFactor,
                        message = binder.userMessage(result),
                    ),
                )
            }
        }
    }
}
