package com.example.applock.screen.home

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.applock.dao.AppInfoDatabase
import com.example.applock.model.AppInfo
import com.example.applock.util.AppInfoUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ViewModel quản lý dữ liệu ứng dụng cho AllAppFragment và LockedAppFragment
class AppLockViewModel : ViewModel() {
    private val _allApps = MutableLiveData<MutableList<AppInfo>>(mutableListOf())
    val allApps: LiveData<MutableList<AppInfo>> get() = _allApps

    private val _lockedApps = MutableLiveData<MutableList<AppInfo>>(mutableListOf())
    val lockedApps: LiveData<MutableList<AppInfo>> get() = _lockedApps

    // Cập nhật danh sách ứng dụng đã khóa
    fun updateLockedApps(newLockedApps: MutableList<AppInfo>) {
        _lockedApps.value = newLockedApps.distinctBy { it.packageName }
            .sortedBy { it.name }
            .toMutableList()
    }

    // Cập nhật danh sách ứng dụng chưa khóa
    fun updateAllApps(newAllApps: MutableList<AppInfo>) {
        _allApps.value = newAllApps.distinctBy { it.packageName }
            .sortedBy { it.name }
            .toMutableList()
    }

    // Tải dữ liệu ban đầu từ database
    fun loadInitialData(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppInfoDatabase.getInstance(context)
            val allAppsFromDb = db.appInfoDAO().getAllApp()
            val lockedAppsFromDb = db.appInfoDAO().getLockedApp()
            AppInfoUtil.listAppInfo.clear()
            AppInfoUtil.listAppInfo.addAll(allAppsFromDb)
            AppInfoUtil.listLockedAppInfo.clear()
            AppInfoUtil.listLockedAppInfo.addAll(lockedAppsFromDb)
            withContext(Dispatchers.Main) {
                updateAllApps(allAppsFromDb.filter { !it.isLocked }.toMutableList())
                updateLockedApps(lockedAppsFromDb.toMutableList())
            }
        }
    }

    // Làm mới dữ liệu từ AppInfoUtil
    fun refreshData() {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main) {
                updateAllApps(AppInfoUtil.listAppInfo.filter { !it.isLocked }.toMutableList())
                updateLockedApps(AppInfoUtil.listLockedAppInfo.toMutableList())
            }
        }
    }

    // Cập nhật trạng thái khóa của ứng dụng
    fun updateAppLockStatus(context: Context, appInfo: AppInfo, isLocked: Boolean) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppInfoDatabase.getInstance(context)
            db.appInfoDAO().updateAppLockStatus(appInfo.packageName, isLocked)
            withContext(Dispatchers.Main) {
                appInfo.isLocked = isLocked
                if (isLocked) {
                    if (!AppInfoUtil.listLockedAppInfo.any { it.packageName == appInfo.packageName }) {
                        AppInfoUtil.listLockedAppInfo.add(appInfo)
                    }
                    AppInfoUtil.listAppInfo.find { it.packageName == appInfo.packageName }?.isLocked = true
                } else {
                    AppInfoUtil.listLockedAppInfo.removeAll { it.packageName == appInfo.packageName }
                    AppInfoUtil.listAppInfo.find { it.packageName == appInfo.packageName }?.isLocked = false
                }
                refreshData()
            }
        }
    }

    fun removeFromAllApps(packageName: String) {
        val currentList = _allApps.value?.toMutableList() ?: mutableListOf()
        currentList.removeAll { it.packageName == packageName }
        updateAllApps(currentList)
    }

    fun addToAllApps(appInfo: AppInfo) {
        val currentList = _allApps.value?.toMutableList() ?: mutableListOf()
        if (!currentList.any { it.packageName == appInfo.packageName }) {
            currentList.add(appInfo)
            updateAllApps(currentList)
        }
    }
}