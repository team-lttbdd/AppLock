package com.example.applock.util

import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.core.net.toUri
import com.example.applock.app.MyApplication
import com.example.applock.receiver.AppLockDeviceAdminReceiver

@SuppressLint("StaticFieldLeak")
object PermissionUtil {
    private var context: Context? = null

    fun init(application: MyApplication) {
        context = application.applicationContext
    }

    // Kiểm tra xem tất cả quyền đã được cấp chưa
    fun isAllPermissionRequested(): Boolean {
        return checkUsageStatsPermission() && checkOverlayPermission() && checkDeviceAdminPermission()
    }

    // Kiểm tra quyền truy cập thống kê sử dụng
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

    // Yêu cầu quyền truy cập thống kê sử dụng
    fun requestUsageStatsPermission() {
        context?.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    }

    // Kiểm tra quyền vẽ lên các ứng dụng khác
    fun checkOverlayPermission(): Boolean {
        context?.let {
            return Settings.canDrawOverlays(it)
        }
        return false
    }

    // Yêu cầu quyền vẽ lên các ứng dụng khác
    fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            "package:${context?.packageName}".toUri()
        )
        context?.startActivity(intent.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        })
    }

    // Kiểm tra quyền quản trị thiết bị
    fun checkDeviceAdminPermission(): Boolean {
        context?.let {
            return AppLockDeviceAdminReceiver.isAdminActive(it)
        }
        return false
    }
}