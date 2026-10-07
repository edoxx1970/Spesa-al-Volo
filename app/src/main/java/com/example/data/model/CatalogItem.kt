package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * An item in the store's price list (listino prezzi).
 * Scoped by storeId so prices remain separate for each supermarket.
 */
@Entity(
    tableName = "catalog_items",
    indices = [
        Index(value = ["code", "storeId"], unique = true),
        Index(value = ["code"])
    ]
)
data class CatalogItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val code: String,
    val storeId: String = "_", // "_" indicates default / unassigned store
    val name: String,
    val priceCents: Int,
    val kind: String = "ean",
    val plu: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

data class CheaperComparison(
    val storeName: String,
    val priceCents: Int
)
