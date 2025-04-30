package com.example.applock.dao

import com.example.applock.model.AppInfo
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import androidx.room.TypeConverters


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
                    ).build()
                    INSTANCE = instance
                }
                return instance
            }
        }
    }
}