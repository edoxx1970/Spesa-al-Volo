package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * An item in the currently active shopping cart (conto della spesa).
 */
@Entity(tableName = "cart_items")
data class CartItem(
    @PrimaryKey
    val id: String,
    val code: String,
    val showCode: String = code,
    val name: String,
    val priceCents: Int,
    val quantity: Int = 1,
    val kind: String = "ean", // "ean", "banco", "qr", "manuale"
    val plu: String? = null,
    val tag: String? = null, // "QR", "BANCO", "A MANO"
    val weight: String? = null, // e.g. "0,215 kg"
    val sequence: Int = 0
) {
    val totalPriceCents: Int
        get() = priceCents * maxOf(quantity, 1)

    val isCrossedOut: Boolean
        get() = quantity == 0
}

/**
 * Summary totals for the active cart.
 */
data class CartTotals(
    val totalCents: Int = 0,
    val totalPieces: Int = 0,
    val totalRows: Int = 0
)
