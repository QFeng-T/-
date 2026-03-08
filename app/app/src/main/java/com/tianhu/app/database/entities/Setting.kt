package com.tianhu.app.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "setting")
data class Setting(
    @PrimaryKey
    val config_key: String,
    val config_value: String,
    val update_time: Long
)