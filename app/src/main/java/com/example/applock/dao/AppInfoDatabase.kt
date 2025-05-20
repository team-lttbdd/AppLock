package com.example.applock.dao

import com.example.applock.model.AppInfo
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.applock.util.AppInfoUtil
import com.example.applock.util.Converters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Database Room để lưu trữ thông tin ứng dụng
@Database(entities = [AppInfo::class], version = 1)
@TypeConverters(Converters::class)
abstract class AppInfoDatabase : RoomDatabase() {

    // Trả về DAO để tương tác với database
    abstract fun appInfoDAO(): AppInfoDAO

    companion object {
        @Volatile
        private var INSTANCE: AppInfoDatabase? = null

        // Tạo hoặc lấy instance của database (Singleton)
        fun getInstance(context: Context): AppInfoDatabase {
            synchronized(this) {
                var instance = INSTANCE
                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        AppInfoDatabase::class.java,
                        "appInfo_data_db"
                    )
                        .fallbackToDestructiveMigration() // Xóa database nếu phiên bản thay đổi
                        .addCallback(object : RoomDatabase.Callback() {
                            // Khởi tạo dữ liệu khi database được tạo
                            override fun onCreate(db: SupportSQLiteDatabase) {
                                super.onCreate(db)
                                CoroutineScope(Dispatchers.IO).launch {
                                    val dao = INSTANCE?.appInfoDAO() // Lấy DAO từ instance mới
                                    dao?.let {
                                        val packageManager = context.applicationContext.packageManager
                                        val mainIntent = android.content.Intent(android.content.Intent.ACTION_MAIN, null).apply {
                                            addCategory(android.content.Intent.CATEGORY_LAUNCHER)
                                        }
                                        val resolveInfoList: List<android.content.pm.ResolveInfo> = packageManager.queryIntentActivities(mainIntent, 0)
                                        val initialAppList = resolveInfoList.mapNotNull { resolveInfo ->
                                            val activityInfo = resolveInfo.activityInfo
                                            if (activityInfo != null) {
                                                val name: String = activityInfo.loadLabel(packageManager).toString()
                                                val icon: android.graphics.drawable.Drawable = activityInfo.loadIcon(packageManager)
                                                val packageName: String = activityInfo.packageName
                                                // Mặc định tất cả ứng dụng là chưa khóa khi database được tạo lần đầu
                                                AppInfo(icon, name, packageName, false)
                                            } else null
                                        }
                                        // Chèn tất cả ứng dụng vào database
                                        initialAppList.forEach { app ->
                                            it.insertAppInfo(app)
                                        }
                                    }
                                }
                            }
                        })
                        .build()
                    INSTANCE = instance
                }
                return instance
            }
        }
    }
}