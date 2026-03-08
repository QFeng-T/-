package com.tianhu.app.database.converters

import androidx.room.TypeConverter
import com.tianhu.app.database.enums.LoginType
import com.tianhu.app.database.enums.RecognitionType
import com.tianhu.app.database.enums.SyncStatus

class Converters {
    @TypeConverter
    fun fromRecognitionType(type: RecognitionType): Int {
        return type.value
    }

    @TypeConverter
    fun toRecognitionType(value: Int): RecognitionType {
        return RecognitionType.fromValue(value)
    }

    @TypeConverter
    fun fromSyncStatus(status: SyncStatus): Int {
        return status.value
    }

    @TypeConverter
    fun toSyncStatus(value: Int): SyncStatus {
        return SyncStatus.fromValue(value)
    }

    @TypeConverter
    fun fromLoginType(type: LoginType): String {
        return type.value
    }

    @TypeConverter
    fun toLoginType(value: String): LoginType {
        return LoginType.fromValue(value)
    }
}