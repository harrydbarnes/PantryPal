package com.example.pantrypal.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.pantrypal.data.entity.ConsumptionEntity
import com.example.pantrypal.data.entity.ConsumptionType
import kotlinx.coroutines.flow.Flow

@Dao
interface ConsumptionDao {
    @Insert
    suspend fun insertConsumption(consumption: ConsumptionEntity)

    @Query("SELECT * FROM consumption_history WHERE type = :type ORDER BY date DESC")
    fun getConsumptionByType(type: ConsumptionType): Flow<List<ConsumptionEntity>>

    @Query("SELECT * FROM consumption_history WHERE itemId = :itemId ORDER BY date DESC")
    suspend fun getHistoryForItem(itemId: Long): List<ConsumptionEntity>

    @Query("SELECT * FROM consumption_history ORDER BY date DESC")
    fun getAllHistoryFlow(): Flow<List<ConsumptionEntity>>

    @Query("SELECT * FROM consumption_history")
    suspend fun getAllHistory(): List<ConsumptionEntity>
}
