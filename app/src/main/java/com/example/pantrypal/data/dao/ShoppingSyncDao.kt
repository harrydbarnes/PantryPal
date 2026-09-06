package com.example.pantrypal.data.dao

import androidx.room.*
import com.example.pantrypal.data.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingSyncDao {
    @Query("SELECT * FROM shopping_layout") fun observeLayout(): Flow<List<ShoppingLayoutEntity>>
    @Query("SELECT * FROM shopping_layout") suspend fun layout(): List<ShoppingLayoutEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun putLayout(row: ShoppingLayoutEntity)
    @Query("DELETE FROM shopping_layout WHERE layoutKey = :key") suspend fun deleteLayout(key: String)
    @Query("SELECT * FROM shopping_sync_queue") suspend fun pending(): List<ShoppingSyncQueueEntity>
    @Query("DELETE FROM shopping_sync_queue WHERE recordKey = :key AND token = :token") suspend fun acknowledge(key: String, token: String)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun enqueue(row: ShoppingSyncQueueEntity)
    @Query("SELECT * FROM shopping_sync_batch WHERE id = 1") suspend fun batch(): ShoppingSyncBatchEntity?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveBatch(batch: ShoppingSyncBatchEntity)
    @Query("DELETE FROM shopping_sync_batch") suspend fun clearBatch()
    @Query("DELETE FROM shopping_sync_queue") suspend fun clearQueue()
    @Query("SELECT * FROM shopping_sections") suspend fun sections(): List<ShoppingSectionEntity>
    @Query("SELECT * FROM shopping_list WHERE syncId = :syncId LIMIT 1") suspend fun item(syncId: String): ShoppingItemEntity?
    @Query("SELECT * FROM shopping_sections WHERE syncId = :syncId LIMIT 1") suspend fun section(syncId: String): ShoppingSectionEntity?
    @Query("DELETE FROM shopping_list WHERE syncId = :syncId") suspend fun deleteItem(syncId: String)
    @Query("DELETE FROM shopping_sections WHERE syncId = :syncId AND systemKey IS NULL") suspend fun deleteSection(syncId: String)
}
