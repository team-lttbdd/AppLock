package com.example.applock.receiver

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.applock.R
import com.example.applock.service.LockService

/**
 * DeviceAdminReceiver để ngăn chặn việc gỡ cài đặt ứng dụng
 */
class AppLockDeviceAdminReceiver : DeviceAdminReceiver() {

    private val TAG = "DeviceAdmin"

    /**
     * Được gọi khi ứng dụng bị vô hiệu hóa làm quản trị viên thiết bị
     * Đây là thời điểm quan trọng - nó xảy ra trước khi gỡ cài đặt
     */
    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        Log.i(TAG, "Yêu cầu vô hiệu hóa quyền quản trị thiết bị - có thể đang cố gắng gỡ cài đặt")

        // Hiển thị overlay bảo vệ trước khi hủy quyền admin
        val serviceIntent = Intent(context, LockService::class.java)
        serviceIntent.putExtra("action", "PROTECT_UNINSTALL")

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }

        // Trả về thông báo sẽ hiển thị cho người dùng trong hộp thoại xác nhận
        return context.getString(R.string.admin_receiver_disable_warning)
    }

    companion object {
        /**
         * Kiểm tra xem ứng dụng có quyền quản trị thiết bị không
         */
        fun isAdminActive(context: Context): Boolean {
            val devicePolicyManager = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager
            val componentName = android.content.ComponentName(context, AppLockDeviceAdminReceiver::class.java)
            return devicePolicyManager.isAdminActive(componentName)
        }
    }
}