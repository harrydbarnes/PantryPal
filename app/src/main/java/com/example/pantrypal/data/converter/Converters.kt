package com.example.pantrypal.data.converter

import androidx.room.TypeConverter
import com.example.pantrypal.data.entity.ConsumptionType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    @TypeConverter
    fun fromConsumptionType(value: ConsumptionType): String {
        return value.name
    }

    @TypeConverter
    fun toConsumptionType(value: String): ConsumptionType {
        return try {
            ConsumptionType.valueOf(value)
        } catch (e: IllegalArgumentException) {
            ConsumptionType.FINISHED // Default fallback
        }
    }

    @TypeConverter
    fun fromStringList(value: List<String>): String {
        val gson = Gson()
        val type = object : TypeToken<List<String>>() {}.type
        return gson.toJson(value, type)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        val gson = Gson()
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }
}
