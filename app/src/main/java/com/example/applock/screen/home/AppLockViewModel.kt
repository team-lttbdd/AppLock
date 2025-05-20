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
    // LiveData lưu danh sách ứng dụng chưa khóa
    private val _allApps = MutableLiveData<MutableList<AppInfo>>(mutableListOf())
    val allApps: LiveData<MutableList<AppInfo>> get() = _allApps

    // LiveData lưu danh sách ứng dụng đã khóa
    private val _lockedApps = MutableLiveData<MutableList<AppInfo>>(mutableListOf())
    val lockedApps: LiveData<MutableList<AppInfo>> get() = _lockedApps

    // Cập nhật danh sách ứng dụng đã khóa
    fun updateLockedApps(newLockedApps: MutableList<AppInfo>) {
        val uniqueApps = newLockedApps.distinctBy { it.packageName }.toMutableList()
        uniqueApps.sortBy { it.name }
        _lockedApps.value = uniqueApps
    }

    // Cập nhật danh sách ứng dụng chưa khóa
    fun updateAllApps(newAllApps: MutableList<AppInfo>) {
        val uniqueApps = newAllApps.distinctBy { it.packageName }.toMutableList()
        uniqueApps.sortBy { it.name }
        _allApps.value = uniqueApps
    }

    // Tải dữ liệu ban đầu từ AppInfoUtil
    fun loadInitialData(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            // Clear existing data
            _allApps.postValue(mutableListOf())
            _lockedApps.postValue(mutableListOf())

            // Get data from database first
            val db = AppInfoDatabase.getInstance(context)
            val allAppsFromDb = db.appInfoDAO().getAllApp()
            val lockedAppsFromDb = db.appInfoDAO().getLockedApp()
            
            // Update AppInfoUtil with database data
            AppInfoUtil.listAppInfo.clear()
            AppInfoUtil.listAppInfo.addAll(allAppsFromDb)
            
            // Update locked apps list
            AppInfoUtil.listLockedAppInfo.clear()
            AppInfoUtil.listLockedAppInfo.addAll(lockedAppsFromDb)

            // Filter apps based on lock status
            val allApps = allAppsFromDb.filter { !it.isLocked }
                .distinctBy { it.packageName }
                .toMutableList()
            val lockedApps = lockedAppsFromDb
                .distinctBy { it.packageName }
                .toMutableList()

            // Sort lists
            allApps.sortBy { it.name }
            lockedApps.sortBy { it.name }

            // Update LiveData on main thread
            withContext(Dispatchers.Main) {
                _allApps.value = allApps
                _lockedApps.value = lockedApps
            }
        }
    }

    // Refresh data from AppInfoUtil
    fun refreshData() {
        CoroutineScope(Dispatchers.IO).launch {
            // Get fresh data from AppInfoUtil
            val allApps = AppInfoUtil.listAppInfo.filter { !it.isLocked }
                .distinctBy { it.packageName }
                .toMutableList()
            val lockedApps = AppInfoUtil.listLockedAppInfo
                .distinctBy { it.packageName }
                .toMutableList()

            // Sort lists
            allApps.sortBy { it.name }
            lockedApps.sortBy { it.name }

            // Update LiveData on main thread
            withContext(Dispatchers.Main) {
                _allApps.value = allApps
                _lockedApps.value = lockedApps
            }
        }
    }

    // Update app lock status
    fun updateAppLockStatus(appInfo: AppInfo, isLocked: Boolean) {
        appInfo.isLocked = isLocked
        refreshData()
    }

    fun removeFromAllApps(packageName: String) {
        val currentList = _allApps.value?.toMutableList() ?: mutableListOf()
        currentList.removeAll { it.packageName == packageName }
        _allApps.value = currentList
    }

    fun addToAllApps(appInfo: AppInfo) {
        val currentList = _allApps.value?.toMutableList() ?: mutableListOf()
        if (!currentList.any { it.packageName == appInfo.packageName }) {
            currentList.add(appInfo)
            currentList.sortBy { it.name }
            _allApps.value = currentList
        }
    }
}