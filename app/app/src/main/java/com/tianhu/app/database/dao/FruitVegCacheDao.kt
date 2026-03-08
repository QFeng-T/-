package com.tianhu.app.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.tianhu.app.database.entities.FruitVegCache

@Dao
interface FruitVegCacheDao {
    @Insert
    fun insert(cache: FruitVegCache)

    @Update
    fun update(cache: FruitVegCache)

    @Query("SELECT * FROM fruit_veg_cache WHERE fruit_veg_name = :name")
    fun getByName(name: String): FruitVegCache?

    @Query("SELECT * FROM fruit_veg_cache")
    fun getAll(): List<FruitVegCache>

    @Query("DELETE FROM fruit_veg_cache WHERE id = :id")
    fun deleteById(id: Long)

    @Query("DELETE FROM fruit_veg_cache WHERE cache_time < :timeThreshold")
    fun deleteExpiredCache(timeThreshold: Long)
}