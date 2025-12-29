package com.example.kitchenlocal.data.converter

import androidx.room.TypeConverter
import com.example.kitchenlocal.data.entity.ConsumptionType

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
}
