package com.example.applock.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.applock.model.AppInfo

@Dao
interface AppInfoDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppInfo(appInfo: AppInfo) : Long

    @Update
    suspend fun updateAppInfo(appInfo: AppInfo) : Int

    @Query("UPDATE appInfo_data_tab SET appInfo_isLocked = :isLocked WHERE appInfo_packageName = :packageName")
    suspend fun updateAppLockStatus(packageName: String, isLocked: Boolean): Int

    @Delete
    suspend fun deleteAppInfo(appInfo: AppInfo) : Int

    @Query("DELETE FROM appInfo_data_tab")
    suspend fun deleteAll()

    @Query("SELECT * FROM appInfo_data_tab WHERE appInfo_isLocked = 0")
    suspend fun getAllApp(): List<AppInfo>

    @Query("SELECT * FROM appInfo_data_tab WHERE appInfo_isLocked = 1")
    suspend fun getLockedApp(): List<AppInfo>
}