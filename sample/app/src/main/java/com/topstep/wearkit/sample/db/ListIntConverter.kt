package com.topstep.wearkit.sample.db

import androidx.room.TypeConverter

object ListIntConverter {
    @TypeConverter
    fun fromList(list: List<Int>?): String? {
        return if (list.isNullOrEmpty()) null else list.joinToString(",")
    }

    @TypeConverter
    fun toList(str: String?): List<Int>? {
        if (str.isNullOrEmpty()) return null
        return str.split(",").mapNotNull { it.toIntOrNull() }
    }
}
