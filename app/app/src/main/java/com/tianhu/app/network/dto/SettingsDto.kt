package com.tianhu.app.network.dto

data class UserSettingsDto(
    val auto_save: Boolean,
    val language: String,
    val cloud_sync_enabled: Boolean,
    val updated_at: String
)

data class UpdateSettingsRequest(
    val auto_save: Boolean? = null,
    val language: String? = null,
    val cloud_sync_enabled: Boolean? = null
)
