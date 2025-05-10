package com.example.applock.util

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.example.applock.dao.AppInfoDatabase
import com.example.applock.model.AppInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

object AppInfoUtil {
    var listAppInfo = ArrayList<AppInfo>()
    var listLockedAppInfo = ArrayList<AppInfo>()


    fun initInstalledApps(context: Context) {
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
                val packageName : String = activityInfo.packageName

                listAppInfo.add(AppInfo(icon, name, packageName, false))

            }
        }
        listAppInfo.sortWith(compareBy { it.name })
    }

    //Binary sort
    private fun insertSortedAppInfo(sortedList: MutableList<AppInfo>, newApp: AppInfo) {
        val index = sortedList.binarySearchBy(newApp.name) { it.name }
        val insertIndex = if (index >= 0) index else -index - 1
        sortedList.add(insertIndex, newApp)
    }

    fun transferAppInfo(context: Context,
                        appInfo: AppInfo,
                        receiveList: MutableList<AppInfo>,
                        sendList: MutableList<AppInfo>,
                        setNewList: (List<AppInfo>) -> Unit) {
        // Avoid adding the same app to listLockedAppInfo multiple times when the user clicks repeatedly
        if(!receiveList.contains(appInfo)) {
            //Update the database
            CoroutineScope(Dispatchers.IO).launch {
                val db = AppInfoDatabase.getInstance(context)
                db.appInfoDAO().updateAppLockStatus(appInfo.packageName, true)
            }

            insertSortedAppInfo(receiveList, appInfo)

            //Ensure that DiffUtil can accurately detect changes between the old and new lists
            val tempList = sendList.filterNot { it == appInfo }
            setNewList(tempList)
            sendList.remove(appInfo)
        }
    }

    fun filterList(context: Context,
                   text : String,
                   filteredList: MutableList<AppInfo>,
                   setNewList: (List<AppInfo>) -> Unit) {
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
}