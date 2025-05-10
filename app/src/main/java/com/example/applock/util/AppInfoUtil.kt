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
                val packageName: String = activityInfo.packageName

                listAppInfo.add(AppInfo(icon, name, packageName, false))

            }
        }
        listAppInfo.sortWith(compareBy { it.name })
    }

    private fun insertSortedAppInfo(
        sortedList: MutableList<AppInfo>,
        newApps: List<AppInfo>) {

        sortedList.addAll(newApps)
        sortedList.sortBy { it.name }
    }

    fun transferAppInfo(
        context: Context,
        positionArray: BooleanArray,
        receiveList: MutableList<AppInfo>,
        sendList: MutableList<AppInfo>,
        setNewList: (List<AppInfo>) -> Unit) {

        val transferList: MutableList<AppInfo> = mutableListOf()
        //Update the database
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppInfoDatabase.getInstance(context)
            for(i in positionArray.indices) {
                if(positionArray[i])
                    db.appInfoDAO().updateAppLockStatus(sendList[i].packageName, true)
            }
        }

        for(i in positionArray.indices) {
            if(positionArray[i]) transferList.add(sendList[i])
        }
        insertSortedAppInfo(receiveList, transferList)

        //Ensure that DiffUtil can accurately detect changes between the old and new lists
        val tempList = sendList.filterNot {it in transferList}
        setNewList(tempList)
        sendList.clear()
        sendList.addAll(tempList)
    }

    fun filterList(
        context: Context,
        text: String,
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
