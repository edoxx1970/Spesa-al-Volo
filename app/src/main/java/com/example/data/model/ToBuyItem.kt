package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing an item in a 'To-Buy' (shopping/groceries) list.
 * Supports checking off items automatically or manually when their barcode is scanned.
 */
@Entity(
    tableName = "to_buy_items",
    indices = [
        Index(value = ["scannedBarcode"]),
        Index(value = ["isChecked"])
    ]
)
data class ToBuyItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val quantity: Int = 1,
    val category: String = "Generale",
    val isChecked: Boolean = false,
    val scannedBarcode: String? = null,
    val scannedProductName: String? = null,
    val scannedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)
