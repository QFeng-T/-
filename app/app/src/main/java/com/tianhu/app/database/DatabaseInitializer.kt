package com.tianhu.app.database

import android.content.Context
import com.tianhu.app.database.entities.Setting
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object DatabaseInitializer {
    private val presetConfigs = mapOf(
        "system_language" to "zh-CN",
        "record_auto_save" to "1",
        "record_expire_days" to "90",
        "recognition_timeout" to "5000",
        "image_compress_quality" to "80"
    )

    fun initialize(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = DatabaseManager.getInstance(context)
            val settingDao = db.settingDao()

            presetConfigs.forEach { (key, value) ->
                val existing = settingDao.getByKey(key)
                if (existing == null) {
                    val setting = Setting(
                        config_key = key,
                        config_value = value,
                        update_time = System.currentTimeMillis()
                    )
                    settingDao.insert(setting)
                }
            }
        }
    }
}