package com.example.pantrypal.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.pantrypal.data.entity.PriceHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PriceHistoryDao {
    @Upsert
    suspend fun upsert(price: PriceHistoryEntity): Long

    @Upsert
    suspend fun upsertAll(prices: List<PriceHistoryEntity>)

    @Query("SELECT * FROM price_history ORDER BY purchasedAt DESC, priceId DESC")
    fun observeAll(): Flow<List<PriceHistoryEntity>>

    @Query(
        """
        SELECT * FROM price_history
        WHERE normalizedItemName = :normalizedItemName
        ORDER BY purchasedAt DESC, priceId DESC
        """
    )
    fun observeForItem(normalizedItemName: String): Flow<List<PriceHistoryEntity>>

    @Query(
        """
        SELECT * FROM price_history
        WHERE purchasedAt >= :weekStartMillis AND purchasedAt < :weekEndExclusiveMillis
        ORDER BY purchasedAt DESC, priceId DESC
        """
    )
    fun observeForWeek(
        weekStartMillis: Long,
        weekEndExclusiveMillis: Long
    ): Flow<List<PriceHistoryEntity>>

    @Query("SELECT * FROM price_history")
    suspend fun getAllSnapshot(): List<PriceHistoryEntity>
}
