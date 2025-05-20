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
        val packageName = intent?.data?.schemeSpecificPart
        if (packageName != null && context != null) {
            when (intent.action) {
                Intent.ACTION_PACKAGE_REMOVED -> {
                    Log.d(TAG, "Package removed: $packageName")
                    // Xóa ứng dụng khỏi database và cập nhật ViewModel
                    CoroutineScope(Dispatchers.IO).launch {
                        val db = AppInfoDatabase.getInstance(context)
                        // Sử dụng hàm xóa theo package name
                        db.appInfoDAO().deleteAppInfoByPackageName(packageName)

                        // Cập nhật lại dữ liệu trong ViewModel
                        // Thay vì tải lại toàn bộ, cập nhật danh sách trong ViewModel
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
                    // Thêm ứng dụng mới vào database và cập nhật ViewModel
                    CoroutineScope(Dispatchers.IO).launch {
                        val db = AppInfoDatabase.getInstance(context)
                        // Kiểm tra xem ứng dụng đã tồn tại trong database chưa
                        val existingApp = db.appInfoDAO().getAppInfoByPackageName(packageName)
                        if (existingApp == null) {
                            // Lấy thông tin ứng dụng mới
                            val appInfo = AppInfoUtil.getAppInfoByPackageName(context, packageName)
                            // Kiểm tra nếu lấy được thông tin ứng dụng (không null)
                            if (appInfo != null) {
                                // Thêm ứng dụng mới vào database (mặc định chưa khóa)
                                db.appInfoDAO().insertAppInfo(appInfo)

                                // Cập nhật danh sách trong ViewModel
                                withContext(Dispatchers.Main) {
                                    val currentAllApps = viewModel.allApps.value?.toMutableList() ?: mutableListOf()
                                    currentAllApps.add(appInfo) // Thêm ứng dụng mới vào danh sách chưa khóa
                                    viewModel.updateAllApps(currentAllApps) // ViewModel sẽ tự sắp xếp
                                    // Không cần cập nhật danh sách lockedApps vì ứng dụng mới mặc định chưa khóa
                                }
                            }
                        }
                    }
                }
            }
        }
    }
} 