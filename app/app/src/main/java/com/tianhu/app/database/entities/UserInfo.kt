package com.tianhu.app.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tianhu.app.database.enums.LoginType

@Entity(
    tableName = "user_info",
    indices = [
        Index(name = "idx_login_type", value = ["login_type"])
    ]
)
data class UserInfo(
    @PrimaryKey
    val user_id: String,
    val nickname: String,
    val avatar_uri: String?,
    val login_type: LoginType,
    val phone_number: String?,
    val last_login_time: Long,
    val cloud_sync_switch: Boolean
)