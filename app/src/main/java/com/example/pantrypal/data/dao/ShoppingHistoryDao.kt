package com.example.pantrypal.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.pantrypal.data.entity.ShoppingHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingHistoryDao {
    @Query("SELECT * FROM shopping_history ORDER BY lastUsedAt DESC")
    fun getHistory(): Flow<List<ShoppingHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun remember(entry: ShoppingHistoryEntity)
}
