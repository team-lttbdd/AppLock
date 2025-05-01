package com.example.applock.util

import android.content.Context
import com.example.applock.preference.MyPreferences
import java.util.Locale

object LanguageUtil {
    fun setLanguage(context: Context?) {
        if (context == null) return
        var language: String? = MyPreferences.read(MyPreferences.PREF_LANGUAGE, null)
        if (language == null) {
            language = Locale.getDefault().language
        }
        val newLocale = Locale(language!!.lowercase(Locale.getDefault()))
        Locale.setDefault(newLocale)
        val res = context.resources
        val conf = res.configuration
        conf.setLocale(newLocale)
        res.updateConfiguration(conf, res.displayMetrics)
    }
}
