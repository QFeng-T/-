package com.tianhu.app.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.tianhu.app.database.entities.UserInfo
import com.tianhu.app.database.enums.LoginType

@Dao
interface UserInfoDao {
    @Insert
    fun insert(user: UserInfo)

    @Update
    fun update(user: UserInfo)

    @Query("SELECT * FROM user_info WHERE user_id = :userId")
    fun getByUserId(userId: String): UserInfo?

    @Query("DELETE FROM user_info WHERE user_id = :userId")
    fun deleteByUserId(userId: String)

    @Query("SELECT * FROM user_info WHERE login_type = :loginType")
    fun getByLoginType(loginType: LoginType): List<UserInfo>
}