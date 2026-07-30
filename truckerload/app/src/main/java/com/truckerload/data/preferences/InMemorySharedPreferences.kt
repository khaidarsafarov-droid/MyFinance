package com.truckerload.data.preferences

import android.content.SharedPreferences

/**
 * Process-only prefs used when encrypted storage is unavailable in release builds.
 * Nothing is written to disk — secrets cannot leak via plaintext files.
 */
internal class InMemorySharedPreferences : SharedPreferences {
    private val lock = Any()
    private val values = linkedMapOf<String, Any?>()
    private val listeners = linkedSetOf<SharedPreferences.OnSharedPreferenceChangeListener>()

    override fun getAll(): MutableMap<String, *> = synchronized(lock) {
        LinkedHashMap(values)
    }

    override fun getString(key: String?, defValue: String?): String? = synchronized(lock) {
        values[key] as? String ?: defValue
    }

    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
        synchronized(lock) {
            @Suppress("UNCHECKED_CAST")
            (values[key] as? Set<String>)?.toMutableSet() ?: defValues
        }

    override fun getInt(key: String?, defValue: Int): Int = synchronized(lock) {
        values[key] as? Int ?: defValue
    }

    override fun getLong(key: String?, defValue: Long): Long = synchronized(lock) {
        values[key] as? Long ?: defValue
    }

    override fun getFloat(key: String?, defValue: Float): Float = synchronized(lock) {
        values[key] as? Float ?: defValue
    }

    override fun getBoolean(key: String?, defValue: Boolean): Boolean = synchronized(lock) {
        values[key] as? Boolean ?: defValue
    }

    override fun contains(key: String?): Boolean = synchronized(lock) {
        values.containsKey(key)
    }

    override fun edit(): SharedPreferences.Editor = Editor()

    override fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?,
    ) {
        if (listener == null) return
        synchronized(lock) { listeners.add(listener) }
    }

    override fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?,
    ) {
        if (listener == null) return
        synchronized(lock) { listeners.remove(listener) }
    }

    private inner class Editor : SharedPreferences.Editor {
        private val pending = linkedMapOf<String, Any?>()
        private var clearAll = false

        override fun putString(key: String?, value: String?): SharedPreferences.Editor {
            if (key != null) pending[key] = value
            return this
        }

        override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor {
            if (key != null) pending[key] = values?.toSet()
            return this
        }

        override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
            if (key != null) pending[key] = value
            return this
        }

        override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
            if (key != null) pending[key] = value
            return this
        }

        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
            if (key != null) pending[key] = value
            return this
        }

        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
            if (key != null) pending[key] = value
            return this
        }

        override fun remove(key: String?): SharedPreferences.Editor {
            if (key != null) pending[key] = REMOVE
            return this
        }

        override fun clear(): SharedPreferences.Editor {
            clearAll = true
            pending.clear()
            return this
        }

        override fun commit(): Boolean {
            apply()
            return true
        }

        override fun apply() {
            val changedKeys = mutableListOf<String>()
            synchronized(lock) {
                if (clearAll) {
                    changedKeys += values.keys
                    values.clear()
                }
                pending.forEach { (key, value) ->
                    if (value === REMOVE) {
                        if (values.remove(key) != null) changedKeys += key
                    } else {
                        values[key] = value
                        changedKeys += key
                    }
                }
                pending.clear()
                clearAll = false
                val snapshot = listeners.toList()
                changedKeys.forEach { key ->
                    snapshot.forEach { it.onSharedPreferenceChanged(this@InMemorySharedPreferences, key) }
                }
            }
        }
    }

    private companion object {
        private val REMOVE = Any()
    }
}
