package com.example.applock.util


import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import com.example.applock.model.AppInfo

object AppInfoUtil {
    val listAppInfo by lazy { ArrayList<AppInfo>() }

    fun initInstalledApps(context: Context) {
        listAppInfo.clear()

        val packageManager = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfoList: List<ResolveInfo> = packageManager.queryIntentActivities(mainIntent, 0)

        for (resolveInfo in resolveInfoList) {
            val activityInfo = resolveInfo.activityInfo
            if (activityInfo != null) {
                val name: String = activityInfo.loadLabel(packageManager).toString()
                val icon: Drawable = activityInfo.loadIcon(packageManager)

                listAppInfo.add(AppInfo(icon, name))
            }
        }

        listAppInfo.sortWith(compareBy { it.name })
    }
}