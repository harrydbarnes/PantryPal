package com.example.pantrypal.data.dao

import androidx.room.*
import com.example.pantrypal.data.entity.ShoppingArchiveEntity
import com.example.pantrypal.data.entity.ShoppingItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingDao {
    @Query("SELECT * FROM shopping_list ORDER BY isChecked ASC, addedAt DESC")
    fun getAllShoppingItems(): Flow<List<ShoppingItemEntity>>

    @Query("SELECT * FROM shopping_archive ORDER BY completedAt DESC, archiveId DESC")
    fun getShoppingArchive(): Flow<List<ShoppingArchiveEntity>>

    @Query("SELECT * FROM shopping_list")
    suspend fun getAllShoppingItemsSnapshot(): List<ShoppingItemEntity>

    @Query("SELECT * FROM shopping_archive ORDER BY completedAt DESC, archiveId DESC")
    suspend fun getShoppingArchiveSnapshot(): List<ShoppingArchiveEntity>

    @Query("SELECT COUNT(*) FROM shopping_list WHERE isChecked = 0")
    suspend fun countOpenShoppingItems(): Int

    @Query(
        """
        SELECT COUNT(*) FROM shopping_list AS item
        WHERE item.isChecked = 0
          AND (
              item.weekId IS NULL
              OR item.weekId = :weekId
              OR item.sectionId IN (
                  SELECT sectionId FROM shopping_sections WHERE recursEveryWeek = 1
              )
          )
        """
    )
    suspend fun countOpenShoppingItemsForWeek(weekId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingArchive(items: List<ShoppingArchiveEntity>)

    @Query("DELETE FROM shopping_archive")
    suspend fun clearShoppingArchive()

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
