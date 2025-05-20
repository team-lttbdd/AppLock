package com.example.applock.app

import android.app.Application
import com.example.applock.util.PermissionUtils
import com.example.applock.preference.MyPreferences

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MyPreferences.init(this)
        PermissionUtils.init(this)
    }
}