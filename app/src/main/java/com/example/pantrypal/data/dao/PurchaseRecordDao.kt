package com.example.pantrypal.data.dao

import androidx.room.*
import com.example.pantrypal.data.entity.*

@Dao
interface PurchaseRecordDao {
    @Query("SELECT * FROM purchase_records") suspend fun records(): List<PurchaseRecordEntity>
    @Query("SELECT * FROM purchase_records WHERE shoppingSyncId = :syncId AND completed = 0 LIMIT 1")
    suspend fun pending(syncId: String): PurchaseRecordEntity?
    @Query("SELECT * FROM receipt_imports") suspend fun imports(): List<ReceiptImportEntity>
    @Query("SELECT EXISTS(SELECT 1 FROM receipt_imports WHERE receiptId = :receiptId)") suspend fun imported(receiptId: String): Boolean
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun put(row: PurchaseRecordEntity)
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun markImported(row: ReceiptImportEntity)
    @Query("DELETE FROM purchase_records") suspend fun clearRecords()
    @Query("DELETE FROM receipt_imports") suspend fun clearImports()
}
