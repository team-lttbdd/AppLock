package com.example.applock.util

import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.widget.Toast
import com.example.applock.dao.AppInfoDatabase
import com.example.applock.model.AppInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Utility để quản lý thông tin ứng dụng
object AppInfoUtil {
    // Danh sách toàn bộ ứng dụng
    var listAppInfo = ArrayList<AppInfo>()
    // Danh sách ứng dụng đã khóa
    var listLockedAppInfo = ArrayList<AppInfo>()

    // Tải danh sách ứng dụng đã cài đặt từ hệ thống
    fun initInstalledApps(context: Context) {
        val packageManager = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfoList: List<ResolveInfo> = packageManager.queryIntentActivities(mainIntent, 0)
        listAppInfo.clear()

        // Lấy thông tin ứng dụng (tên, biểu tượng, packageName)
        for (resolveInfo in resolveInfoList) {
            val activityInfo = resolveInfo.activityInfo
            if (activityInfo != null) {
                val name: String = activityInfo.loadLabel(packageManager).toString()
                val icon: Drawable = activityInfo.loadIcon(packageManager)
                val packageName: String = activityInfo.packageName
                listAppInfo.add(AppInfo(icon, name, packageName, false))
            }
        }
        listAppInfo.sortWith(compareBy { it.name }) // Sắp xếp theo tên
        updateLockedAppsFromDatabase(context) // Cập nhật trạng thái khóa
    }

    // Cập nhật trạng thái khóa từ database
    private fun updateLockedAppsFromDatabase(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppInfoDatabase.getInstance(context)
            val lockedApps = withContext(Dispatchers.IO) {
                db.appInfoDAO().getLockedApp()
            }
            listLockedAppInfo.clear()
            listLockedAppInfo.addAll(lockedApps)
            // Cập nhật trạng thái isLocked trong listAppInfo
            listAppInfo.forEach { app ->
                app.isLocked = listLockedAppInfo.any { it.packageName == app.packageName }
            }
        }
    }

    // Thêm và sắp xếp danh sách ứng dụng
    internal fun insertSortedAppInfo(sortedList: MutableList<AppInfo>, newApps: List<AppInfo>): MutableList<AppInfo> {
        sortedList.addAll(newApps)
        sortedList.sortBy { it.name }
        return sortedList
    }

    // Lọc danh sách ứng dụng theo tên
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

    // Lấy danh sách ứng dụng chưa khóa từ database
    suspend fun getAllApp(context: Context): List<AppInfo> {
        return withContext(Dispatchers.IO) {
            val db = AppInfoDatabase.getInstance(context)
            val appsFromDb = db.appInfoDAO().getAllApp()
            // Lọc bỏ những ứng dụng không còn cài đặt trên thiết bị
            val packageManager = context.packageManager
            appsFromDb.filter { isPackageInstalled(it.packageName, packageManager) }
        }
    }

    // Lấy danh sách ứng dụng đã khóa từ database
    suspend fun getLockedApp(context: Context): List<AppInfo> {
        return withContext(Dispatchers.IO) {
            val db = AppInfoDatabase.getInstance(context)
            val lockedAppsFromDb = db.appInfoDAO().getLockedApp()
            // Lọc bỏ những ứng dụng không còn cài đặt trên thiết bị
            val packageManager = context.packageManager
            lockedAppsFromDb.filter { isPackageInstalled(it.packageName, packageManager) }
        }
    }

    // Kiểm tra xem một package có được cài đặt trên thiết bị không
    private fun isPackageInstalled(packageName: String, packageManager: android.content.pm.PackageManager): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: android.content.pm.PackageManager.NameNotFoundException) {
            false
        }
    }

    // Lấy thông tin ứng dụng theo package name
    fun getAppInfoByPackageName(context: Context, packageName: String): AppInfo? {
        val packageManager = context.packageManager
        return try {
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            val name = packageManager.getApplicationLabel(applicationInfo).toString()
            val icon = packageManager.getApplicationIcon(applicationInfo)
            AppInfo(icon, name, packageName, false) // Mặc định isLocked = false
        } catch (e: android.content.pm.PackageManager.NameNotFoundException) {
            null // Trả về null nếu không tìm thấy ứng dụng
        }
    }
}