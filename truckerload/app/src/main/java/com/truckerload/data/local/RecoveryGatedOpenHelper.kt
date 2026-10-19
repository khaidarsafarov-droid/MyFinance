package com.truckerload.data.local

import android.util.Log
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

/**
 * Room builds this helper during [androidx.room.RoomDatabase.Builder.build].
 * The SQLCipher file is not opened until [getWritableDatabase], which Room runs
 * on its connection pool. That call waits for journal recovery first.
 */
internal class RecoveryGatedOpenHelperFactory(
    private val appContext: android.content.Context,
    private val dbName: String,
    private val useSqlCipher: Boolean,
) : SupportSQLiteOpenHelper.Factory {
    override fun create(
        configuration: SupportSQLiteOpenHelper.Configuration,
    ): SupportSQLiteOpenHelper {
        return RecoveryGatedOpenHelper(appContext, dbName, useSqlCipher, configuration)
    }
}

internal class RecoveryGatedOpenHelper(
    private val appContext: android.content.Context,
    private val dbName: String,
    private val useSqlCipher: Boolean,
    private val configuration: SupportSQLiteOpenHelper.Configuration,
) : SupportSQLiteOpenHelper {
    private val lock = Any()
    private var delegate: SupportSQLiteOpenHelper? = null
    private var walEnabled: Boolean? = null

    @Volatile
    var isClosed: Boolean = false
        private set

    override val databaseName: String?
        get() = configuration.name

    override fun setWriteAheadLoggingEnabled(enabled: Boolean) {
        walEnabled = enabled
        delegate?.setWriteAheadLoggingEnabled(enabled)
    }

    override val writableDatabase: SupportSQLiteDatabase
        get() = open { it.writableDatabase }

    override val readableDatabase: SupportSQLiteDatabase
        get() = open { it.readableDatabase }

    override fun close() {
        synchronized(lock) {
            isClosed = true
            delegate?.close()
            delegate = null
        }
    }

    private fun open(
        block: (SupportSQLiteOpenHelper) -> SupportSQLiteDatabase,
    ): SupportSQLiteDatabase {
        if (JournalRecoveryGate.deferCurrentThread()) {
            val future = RecoveryOpenThreads.submit { openOnThisThread(block) }
            val waitMs = JournalRecoveryGate.mainOpenWaitMillis
            return try {
                future.get(waitMs, TimeUnit.MILLISECONDS)
            } catch (timeout: TimeoutException) {
                Log.w(
                    TAG,
                    "Main-thread Room open still waiting for journal recovery; continuing on IO",
                )
                future.get()
            } catch (error: ExecutionException) {
                throw error.cause ?: error
            }
        }
        return openOnThisThread(block)
    }

    private fun openOnThisThread(
        block: (SupportSQLiteOpenHelper) -> SupportSQLiteDatabase,
    ): SupportSQLiteDatabase {
        val helper = prepare()
        return block(helper)
    }

    private fun prepare(): SupportSQLiteOpenHelper {
        delegate?.let { return it }
        synchronized(lock) {
            delegate?.let { return it }
            check(!isClosed) { "Database helper is closed" }
            // Background opener blocks until recovery releases. Main never reaches
            // here while the gate is held; [open] moved that call onto IO.
            JournalRecoveryGate.awaitBeforeOpen()
            val created = if (useSqlCipher) {
                val passphrase = DatabasePassphraseStore.forDatabase(appContext, dbName)
                    .getOrCreate()
                PlaintextDatabaseEncryptor.ensureEncrypted(appContext, dbName, passphrase)
                PlaintextDatabaseEncryptor.restoreClearedUserVersion(
                    appContext,
                    dbName,
                    passphrase,
                )
                PlaintextDatabaseEncryptor.openHelperFactory(passphrase).create(configuration)
            } else {
                FrameworkSQLiteOpenHelperFactory().create(configuration)
            }
            walEnabled?.let { created.setWriteAheadLoggingEnabled(it) }
            delegate = created
            return created
        }
    }

    private companion object {
        const val TAG = "RecoveryOpen"
    }
}

internal object RecoveryOpenThreads {
    private val executor: ExecutorService = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "room-recovery-open").apply { isDaemon = true }
    }

    fun <T> submit(block: () -> T) = executor.submit(Callable(block))
}
