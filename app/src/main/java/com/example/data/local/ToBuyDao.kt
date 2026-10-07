package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ToBuyItem
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the 'To-Buy' list.
 * Includes reactive queries, check-off updates on scan, and maintenance operations.
 */
@Dao
interface ToBuyDao {

    @Query("SELECT * FROM to_buy_items ORDER BY isChecked ASC, createdAt DESC")
    fun getAllItems(): Flow<List<ToBuyItem>>

    @Query("SELECT * FROM to_buy_items WHERE isChecked = 0 ORDER BY createdAt DESC")
    fun getPendingItems(): Flow<List<ToBuyItem>>

    @Query("SELECT * FROM to_buy_items WHERE isChecked = 1 ORDER BY scannedAt DESC")
    fun getCheckedItems(): Flow<List<ToBuyItem>>

    @Query("SELECT * FROM to_buy_items WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ToBuyItem?

    @Query("SELECT * FROM to_buy_items WHERE isChecked = 0 AND scannedBarcode = :barcode LIMIT 1")
    suspend fun findPendingByBarcode(barcode: String): ToBuyItem?

    @Query("SELECT * FROM to_buy_items WHERE isChecked = 0")
    suspend fun getAllPendingList(): List<ToBuyItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ToBuyItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ToBuyItem>): List<Long>

    @Update
    suspend fun updateItem(item: ToBuyItem)

    @Delete
    suspend fun deleteItem(item: ToBuyItem)

    @Query("DELETE FROM to_buy_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM to_buy_items WHERE isChecked = 1")
    suspend fun deleteCheckedItems()

    @Query("DELETE FROM to_buy_items")
    suspend fun clearAll()

    /**
     * Checks off an item when its barcode is scanned, recording the barcode, product name and timestamp.
     */
    @Query("""
        UPDATE to_buy_items 
        SET isChecked = 1, 
            scannedBarcode = :barcode, 
            scannedProductName = :productName, 
            scannedAt = :timestamp 
        WHERE id = :id
    """)
    suspend fun checkOffItem(
        id: Long,
        barcode: String,
        productName: String?,
        timestamp: Long = System.currentTimeMillis()
    )

    /**
     * Manually unchecks an item, clearing the scanned barcode data.
     */
    @Query("""
        UPDATE to_buy_items 
        SET isChecked = 0, 
            scannedBarcode = NULL, 
            scannedProductName = NULL, 
            scannedAt = NULL 
        WHERE id = :id
    """)
    suspend fun uncheckItem(id: Long)
}
