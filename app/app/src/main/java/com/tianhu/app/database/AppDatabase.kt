package com.tianhu.app.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.tianhu.app.database.converters.Converters
import com.tianhu.app.database.dao.FruitVegCacheDao
import com.tianhu.app.database.dao.RecognitionRecordDao
import com.tianhu.app.database.dao.SettingDao
import com.tianhu.app.database.dao.UserInfoDao
import com.tianhu.app.database.entities.FruitVegCache
import com.tianhu.app.database.entities.RecognitionRecord
import com.tianhu.app.database.entities.Setting
import com.tianhu.app.database.entities.UserInfo

@Database(
    entities = [
        RecognitionRecord::class,
        UserInfo::class,
        Setting::class,
        FruitVegCache::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recognitionRecordDao(): RecognitionRecordDao
    abstract fun userInfoDao(): UserInfoDao
    abstract fun settingDao(): SettingDao
    abstract fun fruitVegCacheDao(): FruitVegCacheDao
}