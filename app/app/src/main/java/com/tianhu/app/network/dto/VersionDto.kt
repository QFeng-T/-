package com.tianhu.app.network.dto

data class VersionCheckResponse(
    val latest_version: String,
    val version_code: Int,
    val update_url: String?,
    val release_notes: String?,
    val force_update: Boolean
)
