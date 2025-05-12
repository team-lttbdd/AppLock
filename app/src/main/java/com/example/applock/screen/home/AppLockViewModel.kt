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

class AppLockViewModel : ViewModel() {
    private val _allApps = MutableLiveData<MutableList<AppInfo>>(mutableListOf())
    val allApps: LiveData<MutableList<AppInfo>> get() = _allApps

    private val _lockedApps = MutableLiveData<MutableList<AppInfo>>(mutableListOf())
    val lockedApps: LiveData<MutableList<AppInfo>> get() = _lockedApps

    fun updateLockedApps(newLockedApps: MutableList<AppInfo>) {
        _lockedApps.value = newLockedApps
    }

    fun updateAllApps(newAllApps: MutableList<AppInfo>) {
        _allApps.value = newAllApps
    }
    fun loadInitialData(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppInfoDatabase.getInstance(context)
            val allApps = db.appInfoDAO().getAllApp().toMutableList()
            val lockedApps = db.appInfoDAO().getLockedApp().toMutableList()
            _allApps.postValue(allApps)
            _lockedApps.postValue(lockedApps)
        }
    }
}