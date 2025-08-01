package com.bntsoft.toastmasters.data.local.converters

import androidx.room.TypeConverter

class PreferredRoleConverter {

    @TypeConverter
    fun fromStringList(list: List<String>): String {
        return list.joinToString(separator = ",")
    }

    @TypeConverter
    fun toStringList(data: String): List<String> {
        return if (data.isEmpty()) emptyList() else data.split(",")
    }

}
