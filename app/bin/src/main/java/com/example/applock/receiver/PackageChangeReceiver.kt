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

// BroadcastReceiver xử lý sự kiện cài đặt/gỡ ứng dụng
class PackageChangeReceiver(private val viewModel: AppLockViewModel) : BroadcastReceiver() {

    private val TAG = "PackageChangeReceiver"

    override fun onReceive(context: Context?, intent: Intent?) {
        val packageName = intent?.data?.schemeSpecificPart ?: return
        if (context == null) return
        when (intent.action) {
            Intent.ACTION_PACKAGE_REMOVED -> {
                Log.d(TAG, "Ứng dụng đã gỡ: $packageName")
                CoroutineScope(Dispatchers.IO).launch {
                    val db = AppInfoDatabase.getInstance(context)
                    db.appInfoDAO().deleteAppInfoByPackageName(packageName)
                    withContext(Dispatchers.Main) {
                        viewModel.removeFromAllApps(packageName)
                        viewModel.updateLockedApps(
                            viewModel.lockedApps.value?.filterNot { it.packageName == packageName }?.toMutableList() ?: mutableListOf()
                        )
                    }
                }
            }
            Intent.ACTION_PACKAGE_ADDED -> {
                Log.d(TAG, "Ứng dụng đã thêm: $packageName")
                CoroutineScope(Dispatchers.IO).launch {
                    val db = AppInfoDatabase.getInstance(context)
                    if (db.appInfoDAO().getAppInfoByPackageName(packageName) == null) {
                        AppInfoUtil.getAppInfoByPackageName(context, packageName)?.let { appInfo ->
                            db.appInfoDAO().insertAppInfo(appInfo)
                            withContext(Dispatchers.Main) {
                                viewModel.addToAllApps(appInfo)
                            }
                        }
                    }
                }
            }
        }
    }
}