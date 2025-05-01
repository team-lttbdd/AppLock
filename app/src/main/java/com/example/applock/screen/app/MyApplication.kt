package com.example.applock.screen.app

import android.app.Application
import com.example.applock.preference.MyPreferences

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MyPreferences.init(this)
    }
}