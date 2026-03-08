package com.tianhu.app.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "fruit_veg_cache",
    indices = [
        Index(value = ["fruit_veg_name"], unique = true)
    ]
)
data class FruitVegCache(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val fruit_veg_name: String,
    val category: String,
    val storage_method: String,
    val edible_suggestion: String,
    val cache_time: Long
)