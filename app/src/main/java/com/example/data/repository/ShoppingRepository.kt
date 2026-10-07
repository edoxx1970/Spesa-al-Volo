package com.example.data.repository

import android.content.Context
import com.example.data.local.PreferencesManager
import com.example.data.local.SpesaDatabase
import com.example.data.local.UserSettings
import com.example.data.model.CartItem
import com.example.data.model.CartTotals
import com.example.data.model.CatalogItem
import com.example.data.model.CheaperComparison
import com.example.data.model.SessionItemDto
import com.example.data.model.ShoppingListItem
import com.example.data.model.ShoppingSession
import com.example.data.model.Store
import com.example.util.BarcodeParser
import com.example.util.FeedbackType
import com.example.util.FormatUtils
import com.example.util.ParsedScanResult
import com.example.util.SoundFeedback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

sealed class UiEvent {
    data class ShowToast(val title: String, val subtitle: String = "", val isError: Boolean = false, val canUndo: Boolean = false) : UiEvent()
    data class PromptNewItem(val scanResult: ParsedScanResult) : UiEvent()
    data class BudgetAlert(val percentage: Int, val remainingCents: Int) : UiEvent()
}

class ShoppingRepository(
    private val context: Context,
    private val database: SpesaDatabase,
    private val prefs: PreferencesManager,
    private val soundFeedback: SoundFeedback
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    val cartItems: Flow<List<CartItem>> = database.cartDao().getAll()

    val cartTotals: Flow<CartTotals> = cartItems.map { list ->
        var cents = 0
        var pieces = 0
        var rows = 0
        for (item in list) {
            if (item.quantity > 0) {
                cents += item.priceCents * item.quantity
                pieces += item.quantity
                rows++
            }
        }
        CartTotals(totalCents = cents, totalPieces = pieces, totalRows = rows)
    }

    val shoppingList: Flow<List<ShoppingListItem>> = database.shoppingListDao().getAll()
    val historySessions: Flow<List<ShoppingSession>> = database.historyDao().getAll()
    val stores: Flow<List<Store>> = database.storeDao().getAll()

    val settings: StateFlow<UserSettings> = prefs.settings

    private val _isRemoveMode = MutableStateFlow(false)
    val isRemoveMode: StateFlow<Boolean> = _isRemoveMode.asStateFlow()

    private val _isDemoMode = MutableStateFlow(false)
    val isDemoMode: StateFlow<Boolean> = _isDemoMode.asStateFlow()

    private val _events = MutableSharedFlow<UiEvent>()
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    private var undoSnapshot: List<CartItem>? = null
    private var lastBudgetAlertLevel = 0 // 0 = ok, 1 = 90% reached, 2 = exceeded

    init {
        scope.launch {
            initDefaultStoresAndDemo()
        }
    }

    private suspend fun initDefaultStoresAndDemo() {
        val currentStores = database.storeDao().getAll().first()
        if (currentStores.isEmpty()) {
            val initialStores = listOf(
                Store(id = "s_esselunga", name = "Esselunga", address = "Via Roma", lastUsed = System.currentTimeMillis()),
                Store(id = "s_coop", name = "Coop", address = "Corso Italia", lastUsed = System.currentTimeMillis() - 1000),
                Store(id = "s_conad", name = "Conad", address = "Piazza Garibaldi", lastUsed = System.currentTimeMillis() - 2000)
            )
            for (s in initialStores) {
                database.storeDao().insert(s)
            }
            if (prefs.settings.value.currentStoreId == null) {
                prefs.setCurrentStoreId(initialStores.first().id)
            }
        }

        val hasSeen = prefs.settings.value.hasSeenDemo
        val currentCart = database.cartDao().getAll().first()
        if (!hasSeen && currentCart.isEmpty()) {
            loadDemoCart()
        }
    }

    private suspend fun loadDemoCart() {
        _isDemoMode.value = true
        val demo = listOf(
            CartItem(id = "d_latte", code = "8001111000011", showCode = "8001111000011", name = "Latte intero 1 L", priceCents = 129, quantity = 1, kind = "ean", sequence = 7),
            CartItem(id = "d_biscotti", code = "8001111000059", showCode = "8001111000059", name = "Biscotti frollini 350 g", priceCents = 249, quantity = 0, kind = "ean", sequence = 6),
            CartItem(id = "d_penne", code = "8001111000028", showCode = "8001111000028", name = "Penne rigate 500 g", priceCents = 99, quantity = 2, kind = "ean", sequence = 5),
            CartItem(id = "d_pelati", code = "8001111000035", showCode = "8001111000035", name = "Pomodori pelati 400 g", priceCents = 89, quantity = 3, kind = "ean", sequence = 4),
            CartItem(id = "d_prosciutto", code = "2100412003489", showCode = "2100412003489", name = "Prosciutto cotto (banco)", priceCents = 348, quantity = 1, kind = "banco", plu = "100412", tag = "BANCO", sequence = 3),
            CartItem(id = "d_mortadella", code = "8001111000073#215#000215", showCode = "8001111000073", name = "Mortadella (banco)", priceCents = 215, quantity = 1, kind = "banco", plu = "8001111000073", tag = "QR", weight = "0,215 kg", sequence = 2),
            CartItem(id = "d_mozzarella", code = "8001111000042", showCode = "8001111000042", name = "Mozzarella 125 g", priceCents = 115, quantity = 1, kind = "ean", sequence = 1)
        )
        database.cartDao().insertAll(demo)
    }

    suspend fun clearDemoCart() {
        if (_isDemoMode.value) {
            database.cartDao().clear()
            _isDemoMode.value = false
            prefs.setHasSeenDemo(true)
        }
    }

    fun toggleRemoveMode() {
        val next = !_isRemoveMode.value
        _isRemoveMode.value = next
        scope.launch {
            if (next) {
                _events.emit(UiEvent.ShowToast("Spara l'articolo da togliere", "Il prossimo codice letto viene tolto dal conto", isError = true))
            }
        }
    }

    fun setRemoveMode(enabled: Boolean) {
        _isRemoveMode.value = enabled
    }

    suspend fun handleScannedCode(rawCode: String) = withContext(Dispatchers.IO) {
        val currentSettings = prefs.settings.value
        val parsed = BarcodeParser.readScan(rawCode, currentSettings.weighedBarcodeEnabled) ?: return@withContext

        if (_isDemoMode.value) {
            clearDemoCart()
        }

        saveSnapshot()

        if (_isRemoveMode.value) {
            _isRemoveMode.value = false
            removeOneByCode(parsed.key, parsed.show)
            return@withContext
        }

        val existing = database.cartDao().getByCode(parsed.key)
        if (existing != null) {
            val updated = existing.copy(
                quantity = existing.quantity + 1,
                sequence = (database.cartDao().getMaxSequence() ?: 0) + 1
            )
            database.cartDao().update(updated)
            val verb = if (existing.quantity == 0) "Rimesso" else "+1"
            afterItemChanged(updated, verb, FeedbackType.ADD)
            return@withContext
        }

        // GS1 QR with embedded price
        if (parsed.kind == "gs1" && parsed.priceCents != null) {
            val storeId = currentSettings.currentStoreId ?: "_"
            val savedInCat = parsed.base?.let { database.catalogDao().getByCodeAndStore(it, storeId) }
                ?: parsed.base?.let { database.catalogDao().getByCodeAndStore("plu:$it", storeId) }

            val name = savedInCat?.name ?: "Articolo a peso · ${parsed.base ?: parsed.key}"
            val newItem = CartItem(
                id = UUID.randomUUID().toString(),
                code = parsed.key,
                showCode = parsed.show,
                name = name,
                priceCents = parsed.priceCents,
                quantity = 1,
                kind = "banco",
                plu = parsed.base,
                tag = "QR",
                weight = parsed.weightKg,
                sequence = (database.cartDao().getMaxSequence() ?: 0) + 1
            )
            database.cartDao().insert(newItem)
            afterItemChanged(newItem, "Aggiunto", FeedbackType.ADD)
            return@withContext
        }

        // Weighed barcode EAN-13 starting with 2
        if (parsed.kind == "banco" && parsed.priceCents != null && parsed.plu != null) {
            val storeId = currentSettings.currentStoreId ?: "_"
            val savedInCat = database.catalogDao().getByCodeAndStore("plu:${parsed.plu}", storeId)
            val name = savedInCat?.name ?: "Banco · art. ${parsed.plu}"
            val newItem = CartItem(
                id = UUID.randomUUID().toString(),
                code = parsed.key,
                showCode = parsed.show,
                name = name,
                priceCents = parsed.priceCents,
                quantity = 1,
                kind = "banco",
                plu = parsed.plu,
                tag = "BANCO",
                sequence = (database.cartDao().getMaxSequence() ?: 0) + 1
            )
            database.cartDao().insert(newItem)
            afterItemChanged(newItem, "Aggiunto", FeedbackType.ADD)
            return@withContext
        }

        // Look up in store catalog
        val storeId = currentSettings.currentStoreId ?: "_"
        val catalogItem = database.catalogDao().getByCodeAndStore(parsed.key, storeId)
            ?: database.catalogDao().getDefaultStoreItem(parsed.key)

        if (catalogItem != null && catalogItem.priceCents > 0) {
            val newItem = CartItem(
                id = UUID.randomUUID().toString(),
                code = parsed.key,
                showCode = parsed.show,
                name = catalogItem.name,
                priceCents = catalogItem.priceCents,
                quantity = 1,
                kind = parsed.kind,
                sequence = (database.cartDao().getMaxSequence() ?: 0) + 1
            )
            database.cartDao().insert(newItem)
            afterItemChanged(newItem, "Aggiunto", FeedbackType.ADD)
            return@withContext
        }

        // Unknown item: ask user
        soundFeedback.play(FeedbackType.ASK, currentSettings.soundEnabled)
        _events.emit(UiEvent.PromptNewItem(parsed))
    }

    private suspend fun removeOneByCode(code: String, showCode: String) {
        val line = database.cartDao().getByCode(code)
        if (line == null || line.quantity <= 0) {
            soundFeedback.play(FeedbackType.WARN, prefs.settings.value.soundEnabled)
            _events.emit(UiEvent.ShowToast("Questo articolo non è nel conto", BarcodeParser.shortCode(showCode), isError = true))
            return
        }
        val updated = line.copy(quantity = maxOf(0, line.quantity - 1))
        database.cartDao().update(updated)
        val verb = if (updated.quantity == 0) "Depennato" else "Tolto uno"
        afterItemChanged(updated, verb, FeedbackType.REMOVE)
    }

    suspend fun incrementItem(itemId: String) = withContext(Dispatchers.IO) {
        saveSnapshot()
        val line = database.cartDao().getById(itemId) ?: return@withContext
        val updated = line.copy(
            quantity = line.quantity + 1,
            sequence = (database.cartDao().getMaxSequence() ?: 0) + 1
        )
        database.cartDao().update(updated)
        val verb = if (line.quantity == 0) "Rimesso" else "+1"
        afterItemChanged(updated, verb, FeedbackType.ADD)
    }

    suspend fun decrementItem(itemId: String) = withContext(Dispatchers.IO) {
        saveSnapshot()
        val line = database.cartDao().getById(itemId) ?: return@withContext
        val updated = line.copy(quantity = maxOf(0, line.quantity - 1))
        database.cartDao().update(updated)
        val verb = if (updated.quantity == 0) "Depennato" else "Tolto uno"
        afterItemChanged(updated, verb, FeedbackType.REMOVE)
    }

    suspend fun addManualItem(name: String, priceCents: Int, quantity: Int) = withContext(Dispatchers.IO) {
        clearDemoCart()
        saveSnapshot()
        val randomCode = "MAN-" + UUID.randomUUID().toString().take(6).uppercase()
        val item = CartItem(
            id = UUID.randomUUID().toString(),
            code = randomCode,
            showCode = "A MANO",
            name = name.ifBlank { "Articolo a mano" },
            priceCents = priceCents,
            quantity = maxOf(1, quantity),
            kind = "manuale",
            tag = "A MANO",
            sequence = (database.cartDao().getMaxSequence() ?: 0) + 1
        )
        database.cartDao().insert(item)
        afterItemChanged(item, "Aggiunto", FeedbackType.ADD)
    }

    suspend fun saveNewProductAndAddToCart(
        code: String,
        showCode: String,
        name: String,
        priceCents: Int,
        quantity: Int,
        kind: String,
        rememberPrice: Boolean
    ) = withContext(Dispatchers.IO) {
        clearDemoCart()
        saveSnapshot()
        val finalName = name.trim().ifBlank {
            if (kind == "qr") "Articolo da QR" else "Articolo ${code.takeLast(5)}"
        }
        val storeId = prefs.settings.value.currentStoreId ?: "_"

        if (rememberPrice) {
            val catItem = CatalogItem(
                code = code,
                storeId = storeId,
                name = finalName,
                priceCents = priceCents,
                kind = kind,
                updatedAt = System.currentTimeMillis()
            )
            database.catalogDao().insert(catItem)
        }

        val newItem = CartItem(
            id = UUID.randomUUID().toString(),
            code = code,
            showCode = showCode,
            name = finalName,
            priceCents = priceCents,
            quantity = maxOf(1, quantity),
            kind = kind,
            sequence = (database.cartDao().getMaxSequence() ?: 0) + 1
        )
        database.cartDao().insert(newItem)
        afterItemChanged(newItem, "Aggiunto", FeedbackType.ADD)
    }

    suspend fun updateCartItem(
        itemId: String,
        name: String,
        priceCents: Int,
        quantity: Int,
        updateCatalog: Boolean
    ) = withContext(Dispatchers.IO) {
        saveSnapshot()
        val existing = database.cartDao().getById(itemId) ?: return@withContext
        val updated = existing.copy(
            name = name.ifBlank { existing.name },
            priceCents = priceCents,
            quantity = quantity
        )
        database.cartDao().update(updated)

        if (updateCatalog && (existing.kind == "ean" || existing.kind == "qr")) {
            val storeId = prefs.settings.value.currentStoreId ?: "_"
            val catItem = CatalogItem(
                code = existing.code,
                storeId = storeId,
                name = updated.name,
                priceCents = updated.priceCents,
                kind = existing.kind,
                updatedAt = System.currentTimeMillis()
            )
            database.catalogDao().insert(catItem)
        }

        _events.emit(UiEvent.ShowToast("Modificato · ${updated.name}", "$quantity × ${FormatUtils.formatEuroWithSymbol(priceCents)}", canUndo = true))
    }

    suspend fun deleteCartItem(itemId: String) = withContext(Dispatchers.IO) {
        saveSnapshot()
        val item = database.cartDao().getById(itemId) ?: return@withContext
        database.cartDao().deleteById(itemId)
        soundFeedback.play(FeedbackType.REMOVE, prefs.settings.value.soundEnabled)
        _events.emit(UiEvent.ShowToast("Riga eliminata · ${item.name}", isError = true, canUndo = true))
    }

    private suspend fun afterItemChanged(line: CartItem, verb: String, feedbackType: FeedbackType) {
        soundFeedback.play(feedbackType, prefs.settings.value.soundEnabled)

        var matchedListText: String? = null
        if (feedbackType == FeedbackType.ADD && !_isDemoMode.value) {
            val allListItems = database.shoppingListDao().getAll().first()
            for (listItem in allListItems) {
                if (!listItem.isManualChecked && listItem.matchedCode == null) {
                    if (FormatUtils.nameMatch(listItem.text, line.name)) {
                        database.shoppingListDao().update(
                            listItem.copy(
                                matchedCode = line.code,
                                matchedName = line.name
                            )
                        )
                        matchedListText = listItem.text
                        break
                    }
                }
            }
        }

        val sign = if (feedbackType == FeedbackType.REMOVE) "−" else ""
        val subtitle = buildString {
            append("$sign${FormatUtils.formatEuro(line.priceCents)} €")
            if (matchedListText != null) {
                append(" · dalla lista: $matchedListText")
            }
        }
        _events.emit(UiEvent.ShowToast("$verb · ${line.name}", subtitle, isError = (feedbackType == FeedbackType.REMOVE), canUndo = true))

        checkBudget()
    }

    private suspend fun checkBudget() {
        val budget = prefs.settings.value.budgetCents
        if (budget <= 0 || _isDemoMode.value) {
            lastBudgetAlertLevel = 0
            return
        }
        val currentTotals = cartTotals.first()
        val cents = currentTotals.totalCents
        val level = when {
            cents > budget -> 2
            cents >= (budget * 0.9).toInt() -> 1
            else -> 0
        }
        if (level > lastBudgetAlertLevel) {
            val remaining = budget - cents
            val pct = (cents * 100) / budget
            if (level == 2) {
                soundFeedback.play(FeedbackType.WARN, prefs.settings.value.soundEnabled)
                _events.emit(UiEvent.ShowToast("Budget superato", "di ${FormatUtils.formatEuro(cents - budget)} € su ${FormatUtils.formatEuro(budget)} €", isError = true))
            } else {
                soundFeedback.play(FeedbackType.ASK, prefs.settings.value.soundEnabled)
                _events.emit(UiEvent.ShowToast("Sei al $pct% del budget", "restano ${FormatUtils.formatEuro(remaining)} €"))
            }
            _events.emit(UiEvent.BudgetAlert(pct, remaining))
        }
        lastBudgetAlertLevel = level
    }

    private suspend fun saveSnapshot() {
        undoSnapshot = database.cartDao().getAll().first()
    }

    suspend fun undoLastAction() = withContext(Dispatchers.IO) {
        val snapshot = undoSnapshot ?: return@withContext
        database.cartDao().clear()
        database.cartDao().insertAll(snapshot)
        undoSnapshot = null
        _events.emit(UiEvent.ShowToast("Operazione annullata", isError = true))
    }

    suspend fun closeSession(saveToHistory: Boolean) = withContext(Dispatchers.IO) {
        val currentItems = database.cartDao().getAll().first()
        val totals = cartTotals.first()

        if (saveToHistory && totals.totalPieces > 0) {
            val activeStore = getActiveStore()
            val sessionItems = currentItems.filter { it.quantity > 0 }.map {
                SessionItemDto(
                    code = if (it.kind == "manuale") "" else it.showCode,
                    name = it.name,
                    quantity = it.quantity,
                    priceCents = it.priceCents
                )
            }
            val jsonArray = JSONArray()
            for (item in sessionItems) {
                val obj = JSONObject()
                obj.put("code", item.code)
                obj.put("name", item.name)
                obj.put("quantity", item.quantity)
                obj.put("priceCents", item.priceCents)
                jsonArray.put(obj)
            }

            val session = ShoppingSession(
                id = "h_" + System.currentTimeMillis().toString(36),
                timestamp = System.currentTimeMillis(),
                storeId = activeStore?.id,
                storeName = activeStore?.name ?: "Negozio non specificato",
                totalCents = totals.totalCents,
                totalPieces = totals.totalPieces,
                totalRows = totals.totalRows,
                budgetCents = prefs.settings.value.budgetCents,
                itemsJson = jsonArray.toString()
            )
            database.historyDao().insert(session)

            // Remove taken items from shopping list
            database.shoppingListDao().deleteCompleted()
        }

        database.cartDao().clear()
        _isDemoMode.value = false
        _isRemoveMode.value = false
        undoSnapshot = null
        lastBudgetAlertLevel = 0

        soundFeedback.play(FeedbackType.ADD, prefs.settings.value.soundEnabled)
        val msg = if (saveToHistory && totals.totalPieces > 0) {
            "Spesa salvata · ${FormatUtils.formatEuroWithSymbol(totals.totalCents)}"
        } else {
            "Conto svuotato"
        }
        _events.emit(UiEvent.ShowToast(msg, if (saveToHistory) "La trovi nello Storico" else "Non salvata nello storico"))
    }

    suspend fun getActiveStore(): Store? {
        val storeId = prefs.settings.value.currentStoreId ?: return null
        return database.storeDao().getById(storeId)
    }

    suspend fun selectStore(storeId: String, viaGps: Boolean = false) = withContext(Dispatchers.IO) {
        prefs.setCurrentStoreId(storeId)
        database.storeDao().updateLastUsed(storeId)

        // Migrate legacy default '_' prices if any
        val legacy = database.catalogDao().getLegacyDefaultItems()
        if (legacy.isNotEmpty()) {
            val migrated = legacy.map { it.copy(id = 0, storeId = storeId) }
            database.catalogDao().insertAll(migrated)
            database.catalogDao().deleteAllForStore("_")
        }

        val store = database.storeDao().getById(storeId)
        val storeName = store?.name ?: "Negozio"
        val subtitle = if (viaGps) "Riconosciuto con la posizione" else "Prezzi separati per questo negozio"
        _events.emit(UiEvent.ShowToast("Negozio: $storeName", subtitle))
    }

    suspend fun addStore(name: String, address: String = "", lat: Double? = null, lon: Double? = null, osmId: String? = null): Store = withContext(Dispatchers.IO) {
        val store = Store(
            id = "s_" + System.currentTimeMillis().toString(36),
            name = name.take(50),
            address = address,
            latitude = lat,
            longitude = lon,
            osmId = osmId,
            lastUsed = System.currentTimeMillis()
        )
        database.storeDao().insert(store)
        selectStore(store.id)
        store
    }

    suspend fun deleteStore(storeId: String) = withContext(Dispatchers.IO) {
        val store = database.storeDao().getById(storeId)
        database.storeDao().deleteById(storeId)
        database.catalogDao().deleteAllForStore(storeId)
        if (prefs.settings.value.currentStoreId == storeId) {
            val remaining = database.storeDao().getAll().first()
            prefs.setCurrentStoreId(remaining.firstOrNull()?.id)
        }
        _events.emit(UiEvent.ShowToast("Negozio eliminato", store?.name ?: "", isError = true))
    }

    suspend fun findCheaperElsewhere(code: String): CheaperComparison? = withContext(Dispatchers.IO) {
        val currentStoreId = prefs.settings.value.currentStoreId ?: "_"
        val items = database.catalogDao().getElsewherePrices(code, currentStoreId)
        if (items.isEmpty()) return@withContext null
        val best = items.first()
        val store = database.storeDao().getById(best.storeId)
        CheaperComparison(
            storeName = store?.name ?: "Altro",
            priceCents = best.priceCents
        )
    }

    // Shopping List management
    suspend fun addShoppingListItems(text: String) = withContext(Dispatchers.IO) {
        val parts = text.split(Regex("""[,;\n]+"""))
            .map { it.trim().replace(Regex("""\s+"""), " ") }
            .filter { it.isNotEmpty() }

        val activeCart = database.cartDao().getAll().first()
        val newItems = mutableListOf<ShoppingListItem>()

        for (part in parts) {
            val matchedInCart = activeCart.find { it.quantity > 0 && FormatUtils.nameMatch(part, it.name) }
            val item = ShoppingListItem(
                id = UUID.randomUUID().toString(),
                text = part.take(40),
                isManualChecked = false,
                matchedCode = matchedInCart?.code,
                matchedName = matchedInCart?.name,
                createdAt = System.currentTimeMillis()
            )
            newItems.add(item)
        }
        database.shoppingListDao().insertAll(newItems)
        _events.emit(UiEvent.ShowToast("${newItems.size} ${if (newItems.size == 1) "voce aggiunta" else "voci aggiunte"} alla lista"))
    }

    suspend fun toggleShoppingListItem(id: String, checked: Boolean) = withContext(Dispatchers.IO) {
        val item = database.shoppingListDao().getById(id) ?: return@withContext
        val updated = if (checked) {
            item.copy(isManualChecked = true)
        } else {
            item.copy(isManualChecked = false, matchedCode = null, matchedName = null)
        }
        database.shoppingListDao().update(updated)
    }

    suspend fun deleteShoppingListItem(id: String) = withContext(Dispatchers.IO) {
        database.shoppingListDao().deleteById(id)
    }

    suspend fun clearCompletedShoppingListItems() = withContext(Dispatchers.IO) {
        database.shoppingListDao().deleteCompleted()
        _events.emit(UiEvent.ShowToast("Voci completate rimosse dalla lista"))
    }

    // Catalog management
    fun getCatalogForStore(storeId: String): Flow<List<CatalogItem>> {
        return database.catalogDao().getAllForStore(storeId)
    }

    suspend fun updateCatalogItem(item: CatalogItem) = withContext(Dispatchers.IO) {
        database.catalogDao().insert(item)
        // Also sync price with active cart
        val cartItem = database.cartDao().getByCode(item.code)
        if (cartItem != null && item.priceCents > 0) {
            database.cartDao().update(cartItem.copy(priceCents = item.priceCents, name = item.name))
        }
        _events.emit(UiEvent.ShowToast("Listino aggiornato · ${item.name}", FormatUtils.formatEuroWithSymbol(item.priceCents)))
    }

    suspend fun deleteCatalogItem(code: String, storeId: String) = withContext(Dispatchers.IO) {
        database.catalogDao().delete(code, storeId)
        _events.emit(UiEvent.ShowToast("Articolo tolto dal listino", isError = true))
    }

    suspend fun deleteSession(sessionId: String) = withContext(Dispatchers.IO) {
        database.historyDao().deleteById(sessionId)
        _events.emit(UiEvent.ShowToast("Spesa eliminata dallo storico", isError = true))
    }

    fun generateReceiptText(items: List<CartItem>, totals: CartTotals, storeName: String?): String {
        return buildString {
            append("SPESA AL VOLO")
            if (!storeName.isNullOrBlank()) {
                append(" · $storeName")
            }
            append(" · ${FormatUtils.formatDate(System.currentTimeMillis())}\n\n")
            val active = items.filter { it.quantity > 0 }
            for (line in active) {
                append("${line.quantity} × ${line.name}  ${FormatUtils.formatEuroWithSymbol(line.priceCents)} = ${FormatUtils.formatEuroWithSymbol(line.totalPriceCents)}\n")
            }
            append("\nTOTALE: ${FormatUtils.formatEuroWithSymbol(totals.totalCents)} (${totals.totalPieces} pezzi)")
        }
    }

    suspend fun generateCsvExport(): String = withContext(Dispatchers.IO) {
        val sessions = database.historyDao().getAllList()
        val sb = StringBuilder()
        sb.append("Data;Negozio;Codice;Articolo;Quantita;Prezzo_Euro;Totale_Euro\r\n")

        for (session in sessions) {
            val dateStr = FormatUtils.formatDate(session.timestamp)
            val storeStr = session.storeName.replace(";", ",")
            try {
                val array = JSONArray(session.itemsJson)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val code = obj.optString("code", "")
                    val name = obj.optString("name", "").replace(";", ",")
                    val qty = obj.optInt("quantity", 1)
                    val price = obj.optInt("priceCents", 0) / 100.0
                    val total = (qty * price)

                    sb.append("$dateStr;$storeStr;$code;$name;$qty;${String.format(java.util.Locale.ITALIAN, "%.2f", price)};${String.format(java.util.Locale.ITALIAN, "%.2f", total)}\r\n")
                }
            } catch (e: Exception) {
                // Ignore parse errors
            }
        }
        sb.toString()
    }
}
