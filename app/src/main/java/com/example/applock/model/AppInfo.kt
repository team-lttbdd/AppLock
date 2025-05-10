package com.example.applock.model

import android.graphics.drawable.Drawable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "appInfo_data_tab")
data class AppInfo(
    @ColumnInfo(name = "appInfo_icon")
    val icon: Drawable,
    @ColumnInfo(name = "appInfo_name")
    val name: String,
    @PrimaryKey
    @ColumnInfo(name = "appInfo_packageName")
    val packageName: String,
    @ColumnInfo(name = "appInfo_isLocked")
    var isLocked: Boolean
)


