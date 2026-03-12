package com.example.timecard.data.cache

import java.util.concurrent.ConcurrentHashMap

object FileCache {
    private val cache = ConcurrentHashMap<String, String>()

    fun get(key: String): String? = cache[key]

    fun put(key: String, value: String) {
        cache[key] = value
    }

    fun clear() {
        cache.clear()
    }
}
