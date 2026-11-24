package com.truckerload.data.repository

/**
 * What a finished journal write asks the app to do next. Repositories only report
 * the write; the app scope decides what that means (widget refresh, auto backup,
 * maintenance reminder re-plan). Keeps WorkManager, widgets and alarms out of `data/`.
 *
 * The application installs its handler once with [install]. Repositories default to
 * [Installed], which forwards to whatever is installed at the time of the write. Until
 * then, and in tests that do not install one, writes have no side effects.
 */
interface JournalWriteEffects {

    /** Journal rows changed. Refresh what shows them. */
    fun onDataChanged()

    /** A user-visible write landed. Queue an auto backup. */
    fun onBackupWorthyWrite()

    /** Loads or maintenance tasks changed. Mileage reminders may move. */
    fun onMileageInputsChanged()

    object None : JournalWriteEffects {
        override fun onDataChanged() = Unit
        override fun onBackupWorthyWrite() = Unit
        override fun onMileageInputsChanged() = Unit
    }

    /** Forwards to the handler the application installed. */
    object Installed : JournalWriteEffects {
        override fun onDataChanged() = current.onDataChanged()
        override fun onBackupWorthyWrite() = current.onBackupWorthyWrite()
        override fun onMileageInputsChanged() = current.onMileageInputsChanged()
    }

    companion object {
        @Volatile
        private var current: JournalWriteEffects = None

        fun install(effects: JournalWriteEffects) {
            current = effects
        }
    }
}
