package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SpesaDatabase
import com.example.data.local.ToBuyDao
import com.example.data.model.ToBuyItem
import com.example.data.repository.ToBuyRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ToBuyDatabaseTest {

    private lateinit var database: SpesaDatabase
    private lateinit var toBuyDao: ToBuyDao
    private lateinit var repository: ToBuyRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, SpesaDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        toBuyDao = database.toBuyDao()
        repository = ToBuyRepository(toBuyDao)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun testInsertAndRetrievePendingItems() = runBlocking {
        val id1 = repository.addItem("Latte intero", quantity = 2, category = "Latticini")
        val id2 = repository.addItem("Uova fresche", quantity = 1, category = "Alimentari")

        assertTrue(id1 > 0)
        assertTrue(id2 > 0)

        val pending = repository.pendingItems.first()
        assertEquals(2, pending.size)
        assertTrue(pending.any { it.name == "Latte intero" && !it.isChecked })
        assertTrue(pending.any { it.name == "Uova fresche" && !it.isChecked })
    }

    @Test
    fun testCheckOffItemOnScan() = runBlocking {
        val id = repository.addItem("Biscotti frollini")

        // User scans barcode "8001111000059" for "Biscotti frollini 350 g"
        val matched = repository.matchAndCheckOffByScan(
            scannedBarcode = "8001111000059",
            productName = "Biscotti frollini 350 g"
        )

        assertNotNull(matched)
        assertEquals(id, matched?.id)
        assertTrue(matched!!.isChecked)
        assertEquals("8001111000059", matched.scannedBarcode)
        assertEquals("Biscotti frollini 350 g", matched.scannedProductName)
        assertNotNull(matched.scannedAt)

        // Item should now be in checkedItems and not in pendingItems
        val pending = repository.pendingItems.first()
        assertEquals(0, pending.size)

        val checked = repository.checkedItems.first()
        assertEquals(1, checked.size)
        assertEquals("Biscotti frollini", checked.first().name)
        assertEquals("8001111000059", checked.first().scannedBarcode)
    }

    @Test
    fun testUncheckItem() = runBlocking {
        val id = repository.addItem("Penne rigate")
        repository.checkOffItem(id, "8001111000028", "Penne rigate 500 g")

        val checkedBefore = repository.checkedItems.first()
        assertEquals(1, checkedBefore.size)

        // Uncheck manually
        repository.uncheckItem(id)

        val pendingAfter = repository.pendingItems.first()
        assertEquals(1, pendingAfter.size)
        assertFalse(pendingAfter.first().isChecked)
        assertNull(pendingAfter.first().scannedBarcode)
        assertNull(pendingAfter.first().scannedAt)
    }

    @Test
    fun testDeleteCheckedItems() = runBlocking {
        val id1 = repository.addItem("Pomodori pelati")
        val id2 = repository.addItem("Mozzarella")

        repository.checkOffItem(id1, "8001111000035", "Pomodori pelati")

        assertEquals(1, repository.pendingItems.first().size)
        assertEquals(1, repository.checkedItems.first().size)

        // Clear completed
        repository.clearCompletedItems()

        val allItems = repository.allItems.first()
        assertEquals(1, allItems.size)
        assertEquals(id2, allItems.first().id)
        assertEquals("Mozzarella", allItems.first().name)
    }
}
