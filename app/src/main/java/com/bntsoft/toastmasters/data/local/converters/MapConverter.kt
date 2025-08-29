package com.bntsoft.toastmasters.data.local.converters

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

class MapConverter {
    private val gson = Gson()
    private val type: Type = object : TypeToken<Map<String, Int>>() {}.type

    @TypeConverter
    fun fromString(value: String?): Map<String, Int> {
        if (value == null || value.isEmpty()) {
            return emptyMap()
        }
        return gson.fromJson(value, type) ?: emptyMap()
    }

    @TypeConverter
    fun fromMap(map: Map<String, Int>?): String {
        if (map == null || map.isEmpty()) {
            return ""
        }
        return gson.toJson(map, type)
    }
}
