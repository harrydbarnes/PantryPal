package com.example.pantrypal.data.converter

import androidx.room.TypeConverter
import com.example.pantrypal.data.entity.ConsumptionType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    @TypeConverter fun fromStoreTag(value: com.example.pantrypal.data.entity.StoreTag): String = value.name
    @TypeConverter fun toStoreTag(value: String): com.example.pantrypal.data.entity.StoreTag =
        runCatching { com.example.pantrypal.data.entity.StoreTag.valueOf(value) }.getOrDefault(com.example.pantrypal.data.entity.StoreTag.EITHER)

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
        return gson.toJson(value, stringListType)
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        return try {
            gson.fromJson(value, stringListType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    companion object {
        private val gson = Gson()
        private val stringListType = object : TypeToken<List<String>>() {}.type
    }
}
