package com.example.applock.app

import android.app.Application
import com.example.applock.util.PermissionUtil
import com.example.applock.preference.MyPreferences

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MyPreferences.init(this)
        PermissionUtil.init(this)
    }
}