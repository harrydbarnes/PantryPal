package com.example.pantrypal.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.pantrypal.data.entity.ShoppingSectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingSectionDao {
    @Query("SELECT * FROM shopping_sections ORDER BY sortOrder, sectionId")
    fun getAllSections(): Flow<List<ShoppingSectionEntity>>

    @Insert
    suspend fun insertSection(section: ShoppingSectionEntity): Long

    @Update
    suspend fun updateSection(section: ShoppingSectionEntity)

    @Delete
    suspend fun deleteSection(section: ShoppingSectionEntity)
}
