package com.example.applock.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import com.example.applock.service.LockService

// BroadcastReceiver xử lý sự kiện thay đổi ngôn ngữ
class LocaleChangeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_LOCALE_CHANGED) {
            Log.d("LocaleChangeReceiver", "Ngôn ngữ thay đổi, cập nhật overlay")
            LockService.startService(context)
        }
    }

    companion object {
        // Đăng ký receiver cho sự kiện thay đổi ngôn ngữ
        fun register(context: Context): LocaleChangeReceiver {
            val receiver = LocaleChangeReceiver()
            val filter = IntentFilter(Intent.ACTION_LOCALE_CHANGED)
            context.registerReceiver(receiver, filter)
            return receiver
        }
    }
}