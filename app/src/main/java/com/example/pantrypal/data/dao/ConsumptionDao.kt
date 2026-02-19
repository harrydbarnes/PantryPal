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

    @Query("SELECT ch.*, i.name, i.category FROM consumption_history ch INNER JOIN items i ON ch.itemId = i.itemId ORDER BY ch.date DESC")
    fun getAllHistoryWithItemFlow(): Flow<List<ConsumptionWithItem>>

    @Query("SELECT * FROM consumption_history ORDER BY date DESC")
    fun getAllHistoryFlow(): Flow<List<ConsumptionEntity>>

    @Query("SELECT * FROM consumption_history")
    suspend fun getAllHistory(): List<ConsumptionEntity>

    @Query("SELECT itemId FROM consumption_history WHERE type = :type GROUP BY itemId HAVING COUNT(*) >= 2 AND (MAX(date) + (MAX(date) - MIN(date)) * 1.0 / (COUNT(*) - 1)) < :currentTime")
    suspend fun getRestockCandidates(currentTime: Long, type: ConsumptionType = ConsumptionType.FINISHED): List<Long>
}
