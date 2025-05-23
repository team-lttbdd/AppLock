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

// BroadcastReceiver xử lý sự kiện cài/gỡ ứng dụng
class PackageChangeReceiver(private val viewModel: AppLockViewModel) : BroadcastReceiver() {

    private val TAG = "PackageChangeReceiver"

    override fun onReceive(context: Context?, intent: Intent?) {
        val packageName = intent?.data?.schemeSpecificPart
        if (packageName != null && context != null) {
            when (intent.action) {
                Intent.ACTION_PACKAGE_REMOVED -> {
                    Log.d(TAG, "Package removed: $packageName")
                    // Xóa ứng dụng khỏi cơ sở dữ liệu và cập nhật ViewModel
                    CoroutineScope(Dispatchers.IO).launch {
                        val db = AppInfoDatabase.getInstance(context)
                        db.appInfoDAO().deleteAppInfoByPackageName(packageName)

                        // Cập nhật danh sách trong ViewModel
                        withContext(Dispatchers.Main) {
                            val currentAllApps = viewModel.allApps.value?.toMutableList() ?: mutableListOf()
                            currentAllApps.removeAll { it.packageName == packageName }
                            viewModel.updateAllApps(currentAllApps)

                            val currentLockedApps = viewModel.lockedApps.value?.toMutableList() ?: mutableListOf()
                            currentLockedApps.removeAll { it.packageName == packageName }
                            viewModel.updateLockedApps(currentLockedApps)
                        }
                    }
                }
                Intent.ACTION_PACKAGE_ADDED -> {
                    Log.d(TAG, "Package added: $packageName")
                    // Thêm ứng dụng mới vào cơ sở dữ liệu và cập nhật ViewModel
                    CoroutineScope(Dispatchers.IO).launch {
                        val db = AppInfoDatabase.getInstance(context)
                        val existingApp = db.appInfoDAO().getAppInfoByPackageName(packageName)
                        if (existingApp == null) {
                            val appInfo = AppInfoUtil.getAppInfoByPackageName(context, packageName)
                            if (appInfo != null) {
                                db.appInfoDAO().insertAppInfo(appInfo)

                                // Cập nhật danh sách chưa khóa trong ViewModel
                                withContext(Dispatchers.Main) {
                                    val currentAllApps = viewModel.allApps.value?.toMutableList() ?: mutableListOf()
                                    currentAllApps.add(appInfo)
                                    viewModel.updateAllApps(currentAllApps)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}