package com.example.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordDao {

    @Query("SELECT * FROM records ORDER BY updatedAt DESC")
    fun getAllRecords(): Flow<List<RecordEntity>>

    @Query("SELECT * FROM records WHERE id = :id LIMIT 1")
    fun getRecordById(id: String): Flow<RecordEntity?>

    @Query("SELECT * FROM records WHERE id = :id LIMIT 1")
    suspend fun getRecordByIdSync(id: String): RecordEntity?

    @Query("""
        SELECT * FROM records 
        WHERE title LIKE '%' || :query || '%' 
           OR description LIKE '%' || :query || '%' 
           OR referenceInfo LIKE '%' || :query || '%' 
           OR tags LIKE '%' || :query || '%'
        ORDER BY updatedAt DESC
    """)
    fun searchRecords(query: String): Flow<List<RecordEntity>>

    @Query("SELECT * FROM records WHERE category = :category ORDER BY updatedAt DESC")
    fun getRecordsByCategory(category: String): Flow<List<RecordEntity>>

    @Query("SELECT COUNT(*) FROM records")
    fun getRecordCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM records WHERE category = :category")
    fun getRecordCountByCategory(category: String): Flow<Int>

    @Query("SELECT * FROM records ORDER BY updatedAt DESC LIMIT :limit")
    fun getRecentRecords(limit: Int = 5): Flow<List<RecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: RecordEntity): Long

    @Update
    suspend fun updateRecord(record: RecordEntity): Int

    @Query("DELETE FROM records WHERE id = :id")
    suspend fun deleteRecordById(id: String): Int

    @Query("DELETE FROM records")
    suspend fun clearAllRecords(): Int

    @Query("SELECT COUNT(*) FROM records WHERE LOWER(title) = LOWER(:title) AND id != :excludeId")
    suspend fun countDuplicateTitle(title: String, excludeId: String): Int
}
