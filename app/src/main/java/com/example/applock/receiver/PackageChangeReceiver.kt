package com.example.applock.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.applock.dao.AppInfoDatabase
import com.example.applock.screen.home.AppLockViewModel
import com.example.applock.util.AppInfoUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PackageChangeReceiver(private val viewModel: AppLockViewModel) : BroadcastReceiver() {

    private val TAG = "PackageChangeReceiver"

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_PACKAGE_REMOVED) {
            val packageName = intent.data?.schemeSpecificPart
            Log.d(TAG, "Package removed: $packageName")
            if (packageName != null && context != null) {
                // Xóa ứng dụng khỏi database và cập nhật ViewModel
                CoroutineScope(Dispatchers.IO).launch {
                    val db = AppInfoDatabase.getInstance(context)
                    // Sử dụng hàm xóa theo package name
                    db.appInfoDAO().deleteAppInfoByPackageName(packageName)

                    // Cập nhật lại dữ liệu trong ViewModel
                    withContext(Dispatchers.Main) {
                         viewModel.loadInitialData(context)
                    }
                }
            }
        }
    }
} 