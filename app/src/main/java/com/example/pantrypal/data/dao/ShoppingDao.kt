package com.example.pantrypal.data.dao

import androidx.room.*
import com.example.pantrypal.data.entity.ShoppingItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingDao {
    @Query("SELECT * FROM shopping_list ORDER BY isChecked ASC, addedAt DESC")
    fun getAllShoppingItems(): Flow<List<ShoppingItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingItem(item: ShoppingItemEntity): Long

    @Update
    suspend fun updateShoppingItem(item: ShoppingItemEntity)

    @Delete
    suspend fun deleteShoppingItem(item: ShoppingItemEntity)

    @Query("DELETE FROM shopping_list WHERE isChecked = 1")
    suspend fun deleteCheckedItems()
}
