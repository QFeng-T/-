package com.tianhu.app.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.tianhu.app.database.entities.Setting

@Dao
interface SettingDao {
    @Insert
    fun insert(setting: Setting)

    @Update
    fun update(setting: Setting)

    @Query("SELECT * FROM setting WHERE config_key = :configKey")
    fun getByKey(configKey: String): Setting?

    @Query("DELETE FROM setting WHERE config_key = :configKey")
    fun deleteByKey(configKey: String)

    @Query("SELECT * FROM setting")
    fun getAll(): List<Setting>
}