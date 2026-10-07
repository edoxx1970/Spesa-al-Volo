package com.example.data.repository

import com.example.data.local.ToBuyDao
import com.example.data.model.ToBuyItem
import com.example.util.FormatUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Repository for managing To-Buy list operations and auto-checking on barcode scan.
 */
class ToBuyRepository(private val toBuyDao: ToBuyDao) {

    val allItems: Flow<List<ToBuyItem>> = toBuyDao.getAllItems()
    val pendingItems: Flow<List<ToBuyItem>> = toBuyDao.getPendingItems()
    val checkedItems: Flow<List<ToBuyItem>> = toBuyDao.getCheckedItems()

    suspend fun addItem(
        name: String,
        quantity: Int = 1,
        category: String = "Generale"
    ): Long = withContext(Dispatchers.IO) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return@withContext -1L
        toBuyDao.insertItem(
            ToBuyItem(
                name = trimmed,
                quantity = maxOf(1, quantity),
                category = category,
                isChecked = false
            )
        )
    }

    suspend fun addMultipleItems(commaSeparatedNames: String) = withContext(Dispatchers.IO) {
        val items = commaSeparatedNames.split(Regex("""[,;\n]+"""))
            .map { it.trim().replace(Regex("""\s+"""), " ") }
            .filter { it.isNotEmpty() }
            .map {
                ToBuyItem(
                    name = it,
                    quantity = 1,
                    isChecked = false
                )
            }
        if (items.isNotEmpty()) {
            toBuyDao.insertAll(items)
        }
    }

    suspend fun checkOffItem(
        id: Long,
        barcode: String,
        productName: String?
    ) = withContext(Dispatchers.IO) {
        toBuyDao.checkOffItem(id, barcode, productName, System.currentTimeMillis())
    }

    suspend fun uncheckItem(id: Long) = withContext(Dispatchers.IO) {
        toBuyDao.uncheckItem(id)
    }

    suspend fun deleteItem(id: Long) = withContext(Dispatchers.IO) {
        toBuyDao.deleteById(id)
    }

    suspend fun clearCompletedItems() = withContext(Dispatchers.IO) {
        toBuyDao.deleteCheckedItems()
    }

    /**
     * Finds a pending to-buy item matching either the scanned barcode or product name,
     * and automatically marks it as checked off.
     * Returns the matched item if found, or null otherwise.
     */
    suspend fun matchAndCheckOffByScan(
        scannedBarcode: String,
        productName: String?
    ): ToBuyItem? = withContext(Dispatchers.IO) {
        val pending = toBuyDao.getAllPendingList()
        // 1. Exact barcode match if previously linked
        val directMatch = pending.find { it.scannedBarcode == scannedBarcode }
        if (directMatch != null) {
            toBuyDao.checkOffItem(directMatch.id, scannedBarcode, productName)
            return@withContext directMatch.copy(
                isChecked = true,
                scannedBarcode = scannedBarcode,
                scannedProductName = productName,
                scannedAt = System.currentTimeMillis()
            )
        }

        // 2. Name fuzzy / keyword match against scanned product name
        if (!productName.isNullOrBlank()) {
            val nameMatched = pending.find { FormatUtils.nameMatch(it.name, productName) }
            if (nameMatched != null) {
                toBuyDao.checkOffItem(nameMatched.id, scannedBarcode, productName)
                return@withContext nameMatched.copy(
                    isChecked = true,
                    scannedBarcode = scannedBarcode,
                    scannedProductName = productName,
                    scannedAt = System.currentTimeMillis()
                )
            }
        }

        null
    }
}
