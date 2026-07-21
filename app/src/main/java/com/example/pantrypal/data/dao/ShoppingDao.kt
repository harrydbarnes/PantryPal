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

    @Query("DELETE FROM shopping_list WHERE isChecked = 1 AND (weekId = :weekId OR weekId IS NULL) AND sectionId IN (SELECT sectionId FROM shopping_sections WHERE recursEveryWeek = 0)")
    suspend fun deleteCheckedWeekItems(weekId: String)

    @Query("UPDATE shopping_list SET isChecked = 0 WHERE isChecked = 1 AND sectionId IN (SELECT sectionId FROM shopping_sections WHERE recursEveryWeek = 1)")
    suspend fun resetCheckedRecurringItems()

    @Query("DELETE FROM shopping_list WHERE sectionId = :sectionId")
    suspend fun deleteItemsInSection(sectionId: Long)

    @Query("DELETE FROM shopping_list WHERE sectionId = :sectionId AND weekId = :weekId")
    suspend fun deleteItemsInSectionForWeek(sectionId: Long, weekId: String)
}
