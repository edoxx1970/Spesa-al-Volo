package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.CartItem
import com.example.data.model.CatalogItem
import com.example.data.model.ShoppingListItem
import com.example.data.model.ShoppingSession
import com.example.data.model.Store
import com.example.data.model.ToBuyItem

@Database(
    entities = [
        CartItem::class,
        CatalogItem::class,
        Store::class,
        ShoppingSession::class,
        ShoppingListItem::class,
        ToBuyItem::class
    ],
    version = 2,
    exportSchema = false
)
abstract class SpesaDatabase : RoomDatabase() {
    abstract fun cartDao(): CartDao
    abstract fun catalogDao(): CatalogDao
    abstract fun storeDao(): StoreDao
    abstract fun historyDao(): HistoryDao
    abstract fun shoppingListDao(): ShoppingListDao
    abstract fun toBuyDao(): ToBuyDao

    companion object {
        @Volatile
        private var INSTANCE: SpesaDatabase? = null

        fun getDatabase(context: Context): SpesaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SpesaDatabase::class.java,
                    "spesa_al_volo.db"
                ).fallbackToDestructiveMigration(dropAllTables = true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
