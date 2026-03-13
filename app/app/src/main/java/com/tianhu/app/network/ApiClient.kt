package com.tianhu.app.network

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private const val PREFS_NAME = "api_prefs"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"
    private const val KEY_BASE_URL = "base_url"
    
    private const val DEFAULT_BASE_URL = "http://localhost:8000/api/"
    private const val TIMEOUT = 30L

    private val gson: Gson by lazy {
        GsonBuilder()
            .setLenient()
            .create()
    }

    private val loggingInterceptor by lazy {
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    private fun getHeaderInterceptor(context: Context): Interceptor {
        return Interceptor { chain ->
            val originalRequest = chain.request()
            val accessToken = getAccessToken(context)
            
            val requestBuilder = originalRequest.newBuilder()
                .header("Accept", "application/json")
            
            if (originalRequest.header("Content-Type") == null) {
                requestBuilder.header("Content-Type", "application/json")
            }
            
            if (accessToken.isNotEmpty()) {
                requestBuilder.header("Authorization", "Bearer $accessToken")
            }
            
            chain.proceed(requestBuilder.build())
        }
    }

    private fun createOkHttpClient(context: Context): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(getHeaderInterceptor(context))
            .connectTimeout(TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    private var currentBaseUrl: String = DEFAULT_BASE_URL
    private var retrofitInstance: Retrofit? = null
    private var apiService: ApiService? = null

    fun init(context: Context) {
        currentBaseUrl = getBaseUrl(context)
        createRetrofit(context)
    }

    private fun createRetrofit(context: Context) {
        retrofitInstance = Retrofit.Builder()
            .baseUrl(currentBaseUrl)
            .client(createOkHttpClient(context))
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
        apiService = null
    }

    fun getApiService(context: Context): ApiService {
        if (apiService == null) {
            apiService = createService(context, ApiService::class.java)
        }
        return apiService!!
    }

    fun <T> createService(context: Context, serviceClass: Class<T>): T {
        if (retrofitInstance == null) {
            createRetrofit(context)
        }
        return retrofitInstance!!.create(serviceClass)
    }

    fun setBaseUrl(context: Context, url: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_BASE_URL, url).apply()
        currentBaseUrl = url
        createRetrofit(context)
    }

    fun getBaseUrl(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
    }

    fun saveTokens(context: Context, accessToken: String, refreshToken: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .apply()
    }

    fun getAccessToken(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_ACCESS_TOKEN, "") ?: ""
    }

    fun getRefreshToken(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_REFRESH_TOKEN, "") ?: ""
    }

    fun clearTokens(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .apply()
    }
}
