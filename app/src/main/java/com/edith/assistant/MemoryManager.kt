package com.edith.assistant

import android.content.Context

/**
 * Simple persistent memory store.
 * From the notes: "program memory - it should remember everything whatever
 * I say" -> user says "remember X is Y", Edith recalls it later with "what is X".
 */
class MemoryManager(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("edith_memory", Context.MODE_PRIVATE)

    fun remember(key: String, value: String) {
        prefs.edit().putString(normalize(key), value).apply()
    }

    fun recall(key: String): String? {
        return prefs.getString(normalize(key), null)
    }

    fun forget(key: String) {
        prefs.edit().remove(normalize(key)).apply()
    }

    fun allMemories(): Map<String, String> {
        @Suppress("UNCHECKED_CAST")
        return prefs.all as Map<String, String>
    }

    private fun normalize(key: String) = key.trim().lowercase()
}
