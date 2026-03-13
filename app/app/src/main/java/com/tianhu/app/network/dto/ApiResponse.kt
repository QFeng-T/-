package com.tianhu.app.network.dto

data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String = ""
)
