package com.example.applock.util

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings

object PermissionUtils {
    private var context: Context? = null

    fun init(context: Context) {
        this.context = context
    }

    fun isAllPermissisionRequested() : Boolean {
        return checkUsageStatsPermission() && checkOverlayPermission()
    }

    fun checkUsageStatsPermission(): Boolean {
        context?.let { context ->
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            } else {
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            }
            return mode == AppOpsManager.MODE_ALLOWED
        }
        return false
    }

    fun requestUsageStatsPermission() {
        context?.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    }

    fun checkOverlayPermission(): Boolean {
        context?.let {
            return Settings.canDrawOverlays(context)
        }
        return false
    }

    fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            android.net.Uri.parse("package:${context?.packageName}")
        )
        context?.startActivity(intent.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    }
}