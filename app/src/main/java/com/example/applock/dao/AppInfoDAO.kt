package com.example.applock.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.applock.model.AppInfo

// DAO để tương tác với bảng appInfo_data_tab trong database
@Dao
interface AppInfoDAO {
    // Thêm thông tin ứng dụng, thay thế nếu đã tồn tại
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppInfo(appInfo: AppInfo): Long

    // Cập nhật thông tin ứng dụng
    @Update
    suspend fun updateAppInfo(appInfo: AppInfo): Int

    // Cập nhật trạng thái khóa của ứng dụng theo packageName
    @Query("UPDATE appInfo_data_tab SET appInfo_isLocked = :isLocked WHERE appInfo_packageName = :packageName")
    suspend fun updateAppLockStatus(packageName: String, isLocked: Boolean): Int

    // Xóa thông tin ứng dụng
    @Delete
    suspend fun deleteAppInfo(appInfo: AppInfo): Int

    // Xóa thông tin ứng dụng theo packageName
    @Query("DELETE FROM appInfo_data_tab WHERE appInfo_packageName = :packageName")
    suspend fun deleteAppInfoByPackageName(packageName: String): Int

    // Xóa toàn bộ bảng
    @Query("DELETE FROM appInfo_data_tab")
    suspend fun deleteAll()

    // Lấy danh sách ứng dụng chưa khóa
    @Query("SELECT * FROM appInfo_data_tab WHERE appInfo_isLocked = 0")
    suspend fun getAllApp(): List<AppInfo>

    // Lấy danh sách ứng dụng đã khóa
    @Query("SELECT * FROM appInfo_data_tab WHERE appInfo_isLocked = 1")
    suspend fun getLockedApp(): List<AppInfo>

    // Kiểm tra trạng thái khóa của ứng dụng
    @Query("SELECT appInfo_isLocked FROM appInfo_data_tab WHERE appInfo_packageName = :packageName")
    suspend fun isAppLocked(packageName: String): Boolean

    // Lấy thông tin ứng dụng theo package name
    @Query("SELECT * FROM appInfo_data_tab WHERE appInfo_packageName = :packageName")
    suspend fun getAppInfoByPackageName(packageName: String): AppInfo?
}