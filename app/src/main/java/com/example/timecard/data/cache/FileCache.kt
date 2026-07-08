package com.example.timecard.data.cache

import android.util.LruCache

object FileCache {
    private const val MAX_ENTRIES = 500
    private val cache = LruCache<String, String>(MAX_ENTRIES)

    fun get(key: String): String? = cache.get(key)

    fun put(key: String, value: String) {
        cache.put(key, value)
    }

    fun clear() {
        cache.evictAll()
    }
}

