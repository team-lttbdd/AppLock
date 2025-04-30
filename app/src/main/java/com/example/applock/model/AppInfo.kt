package com.example.applock.model

import android.graphics.drawable.Drawable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "appInfo_data_tab")
data class AppInfo(
    val icon: Drawable,
    val name: String
)
