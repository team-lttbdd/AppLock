package com.example.applock.util

import android.annotation.SuppressLint
import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.view.LayoutInflater

// Object quản lý kiểm tra và yêu cầu quyền của ứng dụng
@SuppressLint("StaticFieldLeak")
object PermissionUtil {
    private var context: Context? = null

    // Khởi tạo context cho PermissionUtil
    fun init(context: Context) {
        this.context = context
    }

    // Kiểm tra xem tất cả quyền cần thiết đã được cấp chưa
    fun isAllPermissisionRequested(): Boolean {
        return checkUsageStatsPermission() && checkOverlayPermission()
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

    // Kiểm tra quyền vẽ trên ứng dụng khác
    fun checkOverlayPermission(): Boolean {
        context?.let {
            return Settings.canDrawOverlays(context)
        }
        return false
    }

    // Yêu cầu quyền vẽ trên ứng dụng khác
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