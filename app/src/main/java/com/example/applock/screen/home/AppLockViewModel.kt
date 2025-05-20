package com.example.applock.screen.home

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.applock.dao.AppInfoDatabase
import com.example.applock.model.AppInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
        newLockedApps.sortBy { it.name } // Sắp xếp danh sách mới
        _lockedApps.value = newLockedApps
    }

    // Cập nhật danh sách ứng dụng chưa khóa
    fun updateAllApps(newAllApps: MutableList<AppInfo>) {
        newAllApps.sortBy { it.name } // Sắp xếp danh sách mới
        _allApps.value = newAllApps
    }

    // Tải dữ liệu ban đầu từ database
    fun loadInitialData(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppInfoDatabase.getInstance(context)
            val allApps = db.appInfoDAO().getAllApp().toMutableList()
            val lockedApps = db.appInfoDAO().getLockedApp().toMutableList()

            // Sắp xếp danh sách theo tên trước khi cập nhật LiveData
            allApps.sortBy { it.name }
            lockedApps.sortBy { it.name }

            _allApps.postValue(allApps) // Cập nhật LiveData
            _lockedApps.postValue(lockedApps)
        }
    }
}