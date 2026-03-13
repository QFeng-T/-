package com.tianhu.app.network.dto

data class SendCodeRequest(
    val phone_number: String
)

data class LoginRequest(
    val phone_number: String,
    val code: String
)

data class LoginResponse(
    val access_token: String,
    val refresh_token: String,
    val user: UserDto
)

data class RefreshTokenRequest(
    val refresh_token: String
)

data class RefreshTokenResponse(
    val access_token: String,
    val refresh_token: String
)

data class UserDto(
    val id: Int,
    val uid: String?,
    val username: String,
    val email: String?,
    val phone_number: String?,
    val nickname: String?,
    val avatar_path: String?,
    val login_type: String,
    val cloud_sync_switch: Boolean,
    val created_at: String,
    val updated_at: String
)
