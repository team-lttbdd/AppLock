package com.example.applock.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.widget.Toast
import com.example.applock.dao.AppInfoDatabase
import com.example.applock.model.AppInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Collections

object AppInfoUtil {
    var listAppInfo = Collections.synchronizedList(ArrayList<AppInfo>()) // Sử dụng danh sách đồng bộ
    var listLockedAppInfo = Collections.synchronizedList(ArrayList<AppInfo>()) // Sử dụng danh sách đồng bộ

    fun initInstalledApps(context: Context) {
        val packageManager = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfoList: List<ResolveInfo> = packageManager.queryIntentActivities(mainIntent, 0)
        synchronized(listAppInfo) {
            listAppInfo.clear() // Xóa danh sách cũ trong khối đồng bộ
            for (resolveInfo in resolveInfoList) {
                val activityInfo = resolveInfo.activityInfo
                if (activityInfo != null) {
                    val name: String = activityInfo.loadLabel(packageManager).toString()
                    val icon: Drawable = activityInfo.loadIcon(packageManager)
                    val packageName: String = activityInfo.packageName
                    listAppInfo.add(AppInfo(icon, name, packageName, false))
                }
            }
            listAppInfo.sortWith(compareBy { it.name })
        }
        updateLockedAppsFromDatabase(context) // Cập nhật trạng thái khóa từ cơ sở dữ liệu
    }

    private fun updateLockedAppsFromDatabase(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppInfoDatabase.getInstance(context)
            val lockedApps = withContext(Dispatchers.IO) {
                db.appInfoDAO().getLockedApp()
            }
            synchronized(listLockedAppInfo) {
                listLockedAppInfo.clear()
                listLockedAppInfo.addAll(lockedApps)
            }
            // Không cập nhật isLocked tại đây, để SplashActivity xử lý
        }
    }

    internal fun insertSortedAppInfo(sortedList: MutableList<AppInfo>, newApps: List<AppInfo>): MutableList<AppInfo> {
        sortedList.addAll(newApps)
        sortedList.sortBy { it.name }
        return sortedList
    }

    fun filterList(
        context: Context,
        text: String,
        filteredList: MutableList<AppInfo>,
        setNewList: (List<AppInfo>) -> Unit
    ) {
        val tempList = filteredList.filter {
            it.name.lowercase().contains(text.lowercase())
        }
        setNewList(tempList)
        if (tempList.isEmpty()) Toast.makeText(
            context,
            "No data found",
            Toast.LENGTH_SHORT
        ).show()
    }

    suspend fun getAllApp(context: Context): List<AppInfo> {
        return withContext(Dispatchers.IO) {
            val db = AppInfoDatabase.getInstance(context)
            db.appInfoDAO().getAllApp()
        }
    }

    suspend fun getLockedApp(context: Context): List<AppInfo> {
        return withContext(Dispatchers.IO) {
            val db = AppInfoDatabase.getInstance(context)
            db.appInfoDAO().getLockedApp()
        }
    }
}