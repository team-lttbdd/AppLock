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

@Database(entities = [AppInfo::class], version = 1)
@TypeConverters(Converters::class)
abstract class AppInfoDatabase : RoomDatabase() {

    abstract fun appInfoDAO(): AppInfoDAO

    companion object {
        @Volatile
        private var INSTANCE: AppInfoDatabase? = null

        fun getInstance(context: Context): AppInfoDatabase {
            synchronized(this) {
                var instance = INSTANCE
                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        AppInfoDatabase::class.java,
                        "appInfo_data_db"
                    )
                        .fallbackToDestructiveMigration()
                        .addCallback(object : RoomDatabase.Callback() {
                            override fun onCreate(db: SupportSQLiteDatabase) {
                                super.onCreate(db)
                                CoroutineScope(Dispatchers.IO).launch {
                                    val appInfoUtil = AppInfoUtil
                                    appInfoUtil.initInstalledApps(context) // Gọi trực tiếp, không dùng callback
                                    val dao = instance?.appInfoDAO()
                                    dao?.let {
                                        launch {
                                            dao.deleteAll()
                                            AppInfoUtil.listAppInfo.forEach { app ->
                                                dao.insertAppInfo(app)
                                            }
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