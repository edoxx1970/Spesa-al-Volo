package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.CartItem
import com.example.data.model.CatalogItem
import com.example.data.model.ShoppingListItem
import com.example.data.model.ShoppingSession
import com.example.data.model.Store
import kotlinx.coroutines.flow.Flow

@Dao
interface CartDao {
    @Query("SELECT * FROM cart_items ORDER BY sequence DESC")
    fun getAll(): Flow<List<CartItem>>

    @Query("SELECT * FROM cart_items WHERE code = :code LIMIT 1")
    suspend fun getByCode(code: String): CartItem?

    @Query("SELECT * FROM cart_items WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): CartItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: CartItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CartItem>)

    @Update
    suspend fun update(item: CartItem)

    @Query("DELETE FROM cart_items WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM cart_items")
    suspend fun clear()

    @Query("SELECT MAX(sequence) FROM cart_items")
    suspend fun getMaxSequence(): Int?
}

@Dao
interface CatalogDao {
    @Query("SELECT * FROM catalog_items WHERE storeId = :storeId ORDER BY name ASC")
    fun getAllForStore(storeId: String): Flow<List<CatalogItem>>

    @Query("SELECT * FROM catalog_items WHERE code = :code AND storeId = :storeId LIMIT 1")
    suspend fun getByCodeAndStore(code: String, storeId: String): CatalogItem?

    @Query("SELECT * FROM catalog_items WHERE code = :code AND storeId != :storeId AND priceCents > 0 ORDER BY priceCents ASC")
    suspend fun getElsewherePrices(code: String, storeId: String): List<CatalogItem>

    @Query("SELECT * FROM catalog_items WHERE code = :code AND storeId = '_' LIMIT 1")
    suspend fun getDefaultStoreItem(code: String): CatalogItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: CatalogItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<CatalogItem>)

    @Query("DELETE FROM catalog_items WHERE code = :code AND storeId = :storeId")
    suspend fun delete(code: String, storeId: String)

    @Query("DELETE FROM catalog_items WHERE storeId = :storeId")
    suspend fun deleteAllForStore(storeId: String)

    @Query("SELECT COUNT(*) FROM catalog_items WHERE storeId = :storeId AND priceCents > 0")
    suspend fun countPricesForStore(storeId: String): Int

    @Query("SELECT * FROM catalog_items WHERE storeId = '_'")
    suspend fun getLegacyDefaultItems(): List<CatalogItem>
}

@Dao
interface StoreDao {
    @Query("SELECT * FROM stores ORDER BY lastUsed DESC")
    fun getAll(): Flow<List<Store>>

    @Query("SELECT * FROM stores WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): Store?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(store: Store)

    @Query("DELETE FROM stores WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE stores SET lastUsed = :timestamp WHERE id = :id")
    suspend fun updateLastUsed(id: String, timestamp: Long = System.currentTimeMillis())
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM shopping_sessions ORDER BY timestamp DESC")
    fun getAll(): Flow<List<ShoppingSession>>

    @Query("SELECT * FROM shopping_sessions ORDER BY timestamp DESC")
    suspend fun getAllList(): List<ShoppingSession>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: ShoppingSession)

    @Query("DELETE FROM shopping_sessions WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM shopping_sessions")
    suspend fun getCount(): Int
}

@Dao
interface ShoppingListDao {
    @Query("SELECT * FROM shopping_list_items ORDER BY createdAt ASC")
    fun getAll(): Flow<List<ShoppingListItem>>

    @Query("SELECT * FROM shopping_list_items WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): ShoppingListItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ShoppingListItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ShoppingListItem>)

    @Update
    suspend fun update(item: ShoppingListItem)

    @Query("DELETE FROM shopping_list_items WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM shopping_list_items WHERE isManualChecked = 1 OR matchedCode IS NOT NULL")
    suspend fun deleteCompleted()
}
