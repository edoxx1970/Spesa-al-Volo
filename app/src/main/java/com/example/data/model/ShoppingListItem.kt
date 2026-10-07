package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Item in the shopping list (lista della spesa).
 * Automatically ticks off when matching product is scanned into cart.
 */
@Entity(tableName = "shopping_list_items")
data class ShoppingListItem(
    @PrimaryKey
    val id: String,
    val text: String,
    val isManualChecked: Boolean = false,
    val matchedCode: String? = null,
    val matchedName: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
