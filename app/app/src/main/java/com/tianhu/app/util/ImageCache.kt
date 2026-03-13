package com.tianhu.app.util

import android.graphics.Bitmap
import android.util.LruCache

object ImageCache {

    private const val MAX_MEMORY_CACHE_SIZE = 8 * 1024 * 1024
    private val memoryCache: LruCache<String, Bitmap>

    init {
        val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        val cacheSize = (maxMemory / 8).coerceAtMost(MAX_MEMORY_CACHE_SIZE / 1024)
        
        memoryCache = object : LruCache<String, Bitmap>(cacheSize) {
            override fun sizeOf(key: String, bitmap: Bitmap): Int {
                return bitmap.byteCount / 1024
            }
        }
    }

    fun put(key: String, bitmap: Bitmap) {
        if (get(key) == null) {
            memoryCache.put(key, bitmap)
        }
    }

    fun get(key: String): Bitmap? {
        return memoryCache.get(key)
    }

    fun remove(key: String) {
        memoryCache.remove(key)
    }

    fun clear() {
        memoryCache.evictAll()
    }
}
