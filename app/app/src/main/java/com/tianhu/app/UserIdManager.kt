package com.tianhu.app

import android.content.Context
import kotlin.random.Random

object UserIdManager {

    private const val PREFS_NAME = "app_prefs"
    private const val KEY_CURRENT_USER_ID = "current_user_id"
    private const val KEY_LAST_UID = "last_uid"
    private const val GUEST_PREFIX = "guest_"

    fun getCurrentUserId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var userId = prefs.getString(KEY_CURRENT_USER_ID, null)
        
        if (userId == null) {
            userId = generateGuestUserId()
            prefs.edit().putString(KEY_CURRENT_USER_ID, userId).apply()
        }
        
        return userId
    }

    fun generateNewUserId(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastUid = prefs.getInt(KEY_LAST_UID, 0)
        val newUid = lastUid + 1
        
        val userId = newUid.toString().padStart(8, '0')
        prefs.edit()
            .putString(KEY_CURRENT_USER_ID, userId)
            .putInt(KEY_LAST_UID, newUid)
            .apply()
        
        return userId
    }

    private fun generateGuestUserId(): String {
        val randomNum = Random.nextLong(100000, 999999)
        return "$GUEST_PREFIX$randomNum"
    }

    fun isGuestUser(userId: String): Boolean {
        return userId.startsWith(GUEST_PREFIX)
    }

    fun clearCurrentUser(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_CURRENT_USER_ID).apply()
    }
}
