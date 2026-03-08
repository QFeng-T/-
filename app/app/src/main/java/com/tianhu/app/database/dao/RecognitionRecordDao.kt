package com.tianhu.app.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.tianhu.app.database.entities.RecognitionRecord
import com.tianhu.app.database.enums.SyncStatus

@Dao
interface RecognitionRecordDao {
    @Insert
    fun insert(record: RecognitionRecord)

    @Update
    fun update(record: RecognitionRecord)

    @Query("DELETE FROM recognition_record WHERE id = :id")
    fun deleteById(id: Long)

    @Query("DELETE FROM recognition_record WHERE user_id = :userId")
    fun deleteByUserId(userId: String)

    @Query("SELECT * FROM recognition_record WHERE user_id = :userId ORDER BY create_time DESC")
    fun getAllByUserId(userId: String): List<RecognitionRecord>

    @Query("SELECT * FROM recognition_record WHERE user_id = :userId AND is_collected = 1 ORDER BY create_time DESC")
    fun getCollectedByUserId(userId: String): List<RecognitionRecord>

    @Query("SELECT * FROM recognition_record WHERE id = :id")
    fun getById(id: Long): RecognitionRecord?

    @Query("DELETE FROM recognition_record WHERE create_time < :timeThreshold AND user_id = :userId")
    fun deleteExpiredRecords(timeThreshold: Long, userId: String)

    @Query("SELECT * FROM recognition_record WHERE user_id = :userId AND sync_status = :syncStatus ORDER BY create_time DESC")
    fun getBySyncStatus(userId: String, syncStatus: SyncStatus): List<RecognitionRecord>

    @Query("UPDATE recognition_record SET sync_status = :syncStatus WHERE id = :id")
    fun updateSyncStatus(id: Long, syncStatus: SyncStatus)
}