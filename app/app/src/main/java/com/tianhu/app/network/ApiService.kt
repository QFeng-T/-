package com.tianhu.app.network

import com.tianhu.app.network.dto.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @POST("auth/send-code")
    suspend fun sendCode(@Body request: SendCodeRequest): Response<ApiResponse<Unit>>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<ApiResponse<LoginResponse>>

    @POST("auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<ApiResponse<RefreshTokenResponse>>

    @POST("auth/logout")
    suspend fun logout(): Response<ApiResponse<Unit>>

    @GET("users/me")
    suspend fun getCurrentUser(): Response<ApiResponse<UserDto>>

    @PUT("users/me")
    suspend fun updateUser(@Body user: Map<String, Any>): Response<ApiResponse<UserDto>>

    @Multipart
    @POST("users/avatar")
    suspend fun uploadAvatar(@Part avatar: MultipartBody.Part): Response<ApiResponse<String>>

    @Multipart
    @POST("predictions/upload")
    suspend fun uploadAndPredict(@Part file: MultipartBody.Part): Response<ApiResponse<PredictionResponse>>

    @GET("predictions")
    suspend fun getPredictions(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 20
    ): Response<ApiResponse<List<RecognitionRecordDto>>>

    @GET("predictions/{id}")
    suspend fun getPrediction(@Path("id") id: Long): Response<ApiResponse<RecognitionRecordDto>>

    @DELETE("predictions/{id}")
    suspend fun deletePrediction(@Path("id") id: Long): Response<ApiResponse<Unit>>

    @POST("sync/upload")
    suspend fun uploadSyncData(@Body request: SyncUploadRequest): Response<ApiResponse<SyncRecordResponse>>

    @POST("sync/download")
    suspend fun downloadSyncData(@Body request: SyncDownloadRequest): Response<ApiResponse<SyncResponse>>

    @GET("sync/status")
    suspend fun getSyncStatus(): Response<ApiResponse<SyncStatusResponse>>

    @GET("settings")
    suspend fun getUserSettings(): Response<ApiResponse<UserSettingsDto>>

    @PUT("settings")
    suspend fun updateUserSettings(@Body request: UpdateSettingsRequest): Response<ApiResponse<UserSettingsDto>>

    @GET("version/check")
    suspend fun checkVersion(
        @Query("current_version") currentVersion: String,
        @Query("version_code") versionCode: Int
    ): Response<ApiResponse<VersionCheckResponse>>
}
