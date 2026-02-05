package com.obscura.wallpapers.core.data.local

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.obscura.wallpapers.core.data.model.Resolution

/**
 * Room数据库类型转换器
 */
class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return gson.toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return try {
            gson.fromJson(value, listType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun fromResolution(resolution: com.obscura.wallpapers.core.data.model.Resolution?): String? {
        return resolution?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toResolution(value: String?): com.obscura.wallpapers.core.data.model.Resolution? {
        return value?.let { gson.fromJson(it, Resolution::class.java) }
    }
}