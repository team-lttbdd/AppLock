package com.example.applock.preference

import android.content.Context
import android.content.SharedPreferences

object MyPreferences {
    private lateinit var prefs: SharedPreferences
    private const val PREFS_NAME = "shared_preferences"

    const val PREF_LANGUAGE = "pref_language"
    const val PREF_LOCK_PATTERN = "pref_lock_pattern"
    const val IS_HIDE_DRAW_PATTERN = "isHidePattern"
    const val PREF_STEALTH_MODE = "pref_stealth_mode"

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun read(key: String, value: String?): String? {
        return prefs.getString(key, value)
    }

    fun write(key: String, value: String) {
        val prefsEditor = prefs.edit()
        with(prefsEditor) {
            putString(key, value)
            commit()
        }
    }

    fun read(key: String, value: Long): Long {
        return prefs.getLong(key, value)
    }

    fun write(key: String, value: Long) {
        val prefsEditor = prefs.edit()
        with(prefsEditor) {
            putLong(key, value)
            commit()
        }
    }

    fun read(key: String, value: Boolean): Boolean {
        return prefs.getBoolean(key, value)
    }

    fun write(key: String, value: Boolean) {
        val prefsEditor = prefs.edit()
        with(prefsEditor) {
            putBoolean(key, value)
            commit()
        }
    }

    fun remove(key: String) {
        val prefsEditor = prefs.edit()
        with(prefsEditor) {
            remove(key)
            commit()
        }
    }
}
