package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Closed shopping trip stored in history (storico delle spese).
 */
@Entity(tableName = "shopping_sessions")
data class ShoppingSession(
    @PrimaryKey
    val id: String,
    val timestamp: Long,
    val storeId: String? = null,
    val storeName: String = "",
    val totalCents: Int = 0,
    val totalPieces: Int = 0,
    val totalRows: Int = 0,
    val budgetCents: Int = 0,
    val itemsJson: String = "[]" // JSON representation of items for detail display
)

data class SessionItemDto(
    val code: String,
    val name: String,
    val quantity: Int,
    val priceCents: Int
) {
    val totalPriceCents: Int
        get() = priceCents * quantity
}
