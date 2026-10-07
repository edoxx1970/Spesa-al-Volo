package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.PreferencesManager
import com.example.data.local.SpesaDatabase
import com.example.data.local.UserSettings
import com.example.data.model.CartItem
import com.example.data.model.CartTotals
import com.example.data.model.CatalogItem
import com.example.data.model.CheaperComparison
import com.example.data.model.ShoppingListItem
import com.example.data.model.ShoppingSession
import com.example.data.model.Store
import com.example.data.repository.ShoppingRepository
import com.example.data.repository.UiEvent
import com.example.util.BarcodeParser
import com.example.util.CandidatePrice
import com.example.util.FormatUtils
import com.example.util.NearbyShop
import com.example.util.NearbyShopsService
import com.example.util.OpenFoodFactsService
import com.example.util.ParsedScanResult
import com.example.util.PriceTagOcrHelper
import com.example.util.SoundFeedback
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class ActiveSheet {
    data object None : ActiveSheet()
    data class EditItem(
        val isNew: Boolean,
        val cartItemId: String? = null,
        val code: String = "",
        val showCode: String = "",
        val initialName: String = "",
        val initialPriceCents: Int = 0,
        val initialQty: Int = 1,
        val kind: String = "ean",
        val isManual: Boolean = false,
        val elseWhereNote: String? = null
    ) : ActiveSheet()
    data object PriceTagReader : ActiveSheet()
    data object StoreSelector : ActiveSheet()
    data object Budget : ActiveSheet()
    data object ShoppingList : ActiveSheet()
    data object History : ActiveSheet()
    data object Catalog : ActiveSheet()
    data object CopyReceipt : ActiveSheet()
    data object ConfirmCloseSession : ActiveSheet()
}

data class ToastState(
    val title: String,
    val subtitle: String = "",
    val isError: Boolean = false,
    val canUndo: Boolean = false
)

class ShoppingViewModel(application: Application) : AndroidViewModel(application) {

    private val database = SpesaDatabase.getDatabase(application)
    private val prefs = PreferencesManager(application)
    private val soundFeedback = SoundFeedback(application)

    val repository = ShoppingRepository(application, database, prefs, soundFeedback)

    val cartItems: StateFlow<List<CartItem>> = repository.cartItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cartTotals: StateFlow<CartTotals> = repository.cartTotals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CartTotals())

    val shoppingList: StateFlow<List<ShoppingListItem>> = repository.shoppingList
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val historySessions: StateFlow<List<ShoppingSession>> = repository.historySessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stores: StateFlow<List<Store>> = repository.stores
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<UserSettings> = repository.settings
    val isRemoveMode: StateFlow<Boolean> = repository.isRemoveMode
    val isDemoMode: StateFlow<Boolean> = repository.isDemoMode

    private val _isCameraActive = MutableStateFlow(false)
    val isCameraActive: StateFlow<Boolean> = _isCameraActive.asStateFlow()

    private val _isTorchOn = MutableStateFlow(false)
    val isTorchOn: StateFlow<Boolean> = _isTorchOn.asStateFlow()

    private val _zoomRatio = MutableStateFlow(1.0f)
    val zoomRatio: StateFlow<Float> = _zoomRatio.asStateFlow()

    private val _activeSheet = MutableStateFlow<ActiveSheet>(ActiveSheet.None)
    val activeSheet: StateFlow<ActiveSheet> = _activeSheet.asStateFlow()

    private val _toastState = MutableStateFlow<ToastState?>(null)
    val toastState: StateFlow<ToastState?> = _toastState.asStateFlow()

    private val _cheaperComparisons = MutableStateFlow<Map<String, CheaperComparison>>(emptyMap())
    val cheaperComparisons: StateFlow<Map<String, CheaperComparison>> = _cheaperComparisons.asStateFlow()

    private val _candidatePrices = MutableStateFlow<List<CandidatePrice>>(emptyMap<String, Any>().let { emptyList() })
    val candidatePrices: StateFlow<List<CandidatePrice>> = _candidatePrices.asStateFlow()

    private val _nearbyStores = MutableStateFlow<List<NearbyShop>>(emptyList())
    val nearbyStores: StateFlow<List<NearbyShop>> = _nearbyStores.asStateFlow()

    private val _isLocatingStores = MutableStateFlow(false)
    val isLocatingStores: StateFlow<Boolean> = _isLocatingStores.asStateFlow()

    private val _isOcrReading = MutableStateFlow(false)
    val isOcrReading: StateFlow<Boolean> = _isOcrReading.asStateFlow()

    val manualCodeInput = MutableStateFlow("")

    init {
        viewModelScope.launch {
            repository.events.collect { event ->
                when (event) {
                    is UiEvent.ShowToast -> {
                        _toastState.value = ToastState(
                            title = event.title,
                            subtitle = event.subtitle,
                            isError = event.isError,
                            canUndo = event.canUndo
                        )
                    }
                    is UiEvent.PromptNewItem -> {
                        openNewItemSheet(event.scanResult)
                    }
                    is UiEvent.BudgetAlert -> {
                        // Handled by toast
                    }
                }
            }
        }

        viewModelScope.launch {
            cartItems.collect { items ->
                refreshCheaperAlerts(items)
            }
        }
    }

    private fun refreshCheaperAlerts(items: List<CartItem>) {
        viewModelScope.launch {
            val map = mutableMapOf<String, CheaperComparison>()
            for (item in items) {
                if (item.quantity > 0 && (item.kind == "ean" || item.kind == "qr")) {
                    val cheaper = repository.findCheaperElsewhere(item.code)
                    if (cheaper != null && cheaper.priceCents < item.priceCents) {
                        map[item.code] = cheaper
                    }
                }
            }
            _cheaperComparisons.value = map
        }
    }

    fun toggleCamera() {
        _isCameraActive.value = !_isCameraActive.value
        if (!_isCameraActive.value) {
            _isTorchOn.value = false
            _zoomRatio.value = 1.0f
        }
    }

    fun toggleTorch() {
        _isTorchOn.value = !_isTorchOn.value
    }

    fun toggleZoom() {
        _zoomRatio.value = if (_zoomRatio.value == 1.0f) 2.0f else 1.0f
    }

    fun toggleRemoveMode() {
        repository.toggleRemoveMode()
    }

    fun clearDemo() {
        viewModelScope.launch {
            repository.clearDemoCart()
            _toastState.value = ToastState("Conto pronto", "Spara il primo codice")
        }
    }

    fun onBarcodeDetected(code: String) {
        viewModelScope.launch {
            repository.handleScannedCode(code)
        }
    }

    fun submitManualCode() {
        val code = manualCodeInput.value.trim()
        if (code.isNotEmpty()) {
            manualCodeInput.value = ""
            onBarcodeDetected(code)
        }
    }

    fun scanImageUri(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(getApplication<Application>().contentResolver, uri))
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(getApplication<Application>().contentResolver, uri)
                }
                val image = InputImage.fromBitmap(bitmap, 0)
                val scanner = BarcodeScanning.getClient()
                scanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        val code = barcodes.firstOrNull()?.rawValue
                        if (!code.isNullOrBlank()) {
                            onBarcodeDetected(code)
                        } else {
                            _toastState.value = ToastState("Non leggo il codice nella foto", "Inquadra bene l'etichetta", isError = true)
                        }
                    }
                    .addOnFailureListener {
                        _toastState.value = ToastState("Errore lettura immagine", isError = true)
                    }
            } catch (e: Exception) {
                _toastState.value = ToastState("Impossibile aprire l'immagine", isError = true)
            }
        }
    }

    fun incrementItem(id: String) {
        viewModelScope.launch {
            repository.incrementItem(id)
        }
    }

    fun decrementItem(id: String) {
        viewModelScope.launch {
            repository.decrementItem(id)
        }
    }

    fun openSheet(sheet: ActiveSheet) {
        _activeSheet.value = sheet
    }

    fun closeSheet() {
        _activeSheet.value = ActiveSheet.None
        _candidatePrices.value = emptyList()
        _isOcrReading.value = false
    }

    fun clearToast() {
        _toastState.value = null
    }

    fun undo() {
        viewModelScope.launch {
            repository.undoLastAction()
            _toastState.value = null
        }
    }

    private fun openNewItemSheet(scanResult: ParsedScanResult) {
        viewModelScope.launch {
            val elsewhere = repository.findCheaperElsewhere(scanResult.key)
            val elsewhereNote = elsewhere?.let {
                "Altrove (${it.storeName}): ${FormatUtils.formatEuroWithSymbol(it.priceCents)}"
            }

            _activeSheet.value = ActiveSheet.EditItem(
                isNew = true,
                code = scanResult.key,
                showCode = scanResult.show,
                initialName = "",
                initialPriceCents = 0,
                initialQty = 1,
                kind = scanResult.kind,
                elseWhereNote = elsewhereNote
            )

            // Async OpenFoodFacts lookup for product name
            val lookedUpName = OpenFoodFactsService.lookupProductName(scanResult.key)
            if (!lookedUpName.isNullOrBlank()) {
                val cur = _activeSheet.value
                if (cur is ActiveSheet.EditItem && cur.isNew && cur.code == scanResult.key && cur.initialName.isBlank()) {
                    _activeSheet.value = cur.copy(initialName = lookedUpName)
                }
            }
        }
    }

    fun openEditCartItem(item: CartItem) {
        viewModelScope.launch {
            val elsewhere = repository.findCheaperElsewhere(item.code)
            val elsewhereNote = elsewhere?.let {
                "Altrove (${it.storeName}): ${FormatUtils.formatEuroWithSymbol(it.priceCents)}"
            }
            _activeSheet.value = ActiveSheet.EditItem(
                isNew = false,
                cartItemId = item.id,
                code = item.code,
                showCode = item.showCode,
                initialName = item.name,
                initialPriceCents = item.priceCents,
                initialQty = item.quantity,
                kind = item.kind,
                isManual = item.kind == "manuale",
                elseWhereNote = elsewhereNote
            )
        }
    }

    fun openManualItemSheet() {
        _activeSheet.value = ActiveSheet.EditItem(
            isNew = true,
            code = "",
            showCode = "A MANO",
            initialName = "",
            initialPriceCents = 0,
            initialQty = 1,
            kind = "manuale",
            isManual = true
        )
    }

    fun saveItemFromSheet(
        isNew: Boolean,
        isManual: Boolean,
        cartItemId: String?,
        code: String,
        showCode: String,
        name: String,
        priceCents: Int,
        quantity: Int,
        kind: String,
        rememberPrice: Boolean
    ) {
        viewModelScope.launch {
            if (isManual) {
                repository.addManualItem(name, priceCents, quantity)
            } else if (isNew) {
                repository.saveNewProductAndAddToCart(
                    code = code,
                    showCode = showCode,
                    name = name,
                    priceCents = priceCents,
                    quantity = quantity,
                    kind = kind,
                    rememberPrice = rememberPrice
                )
            } else if (cartItemId != null) {
                repository.updateCartItem(
                    itemId = cartItemId,
                    name = name,
                    priceCents = priceCents,
                    quantity = quantity,
                    updateCatalog = rememberPrice
                )
            }
            closeSheet()
        }
    }

    fun deleteItemFromSheet(cartItemId: String?) {
        if (cartItemId == null) return
        viewModelScope.launch {
            repository.deleteCartItem(cartItemId)
            closeSheet()
        }
    }

    // OCR Price Tag Reader
    fun analyzePriceTagBitmap(bitmap: Bitmap) {
        viewModelScope.launch {
            _isOcrReading.value = true
            val candidates = PriceTagOcrHelper.analyzeBitmap(bitmap)
            _candidatePrices.value = candidates
            _isOcrReading.value = false
            if (candidates.isEmpty()) {
                _toastState.value = ToastState("Nessun prezzo trovato", "Tieni il cartellino dritto e senza riflessi", isError = true)
            }
        }
    }

    // Stores
    fun selectStore(storeId: String, viaGps: Boolean = false) {
        viewModelScope.launch {
            repository.selectStore(storeId, viaGps)
        }
    }

    fun addStore(name: String, address: String = "", lat: Double? = null, lon: Double? = null) {
        viewModelScope.launch {
            repository.addStore(name, address, lat, lon)
        }
    }

    fun deleteStore(storeId: String) {
        viewModelScope.launch {
            repository.deleteStore(storeId)
        }
    }

    fun refreshNearbyStores(lat: Double, lon: Double, accuracy: Float) {
        viewModelScope.launch {
            _isLocatingStores.value = true
            val list = NearbyShopsService.findNearbyShops(lat, lon, accuracy)
            _nearbyStores.value = list
            _isLocatingStores.value = false
        }
    }

    // Budget
    fun setBudget(cents: Int) {
        prefs.setBudgetCents(cents)
        val text = if (cents > 0) "Budget: ${FormatUtils.formatEuroWithSymbol(cents)}" else "Budget rimosso"
        _toastState.value = ToastState(text)
    }

    // Shopping list
    fun addShoppingListItems(text: String) {
        viewModelScope.launch {
            repository.addShoppingListItems(text)
        }
    }

    fun toggleShoppingListItem(id: String, checked: Boolean) {
        viewModelScope.launch {
            repository.toggleShoppingListItem(id, checked)
        }
    }

    fun deleteShoppingListItem(id: String) {
        viewModelScope.launch {
            repository.deleteShoppingListItem(id)
        }
    }

    fun clearCompletedShoppingListItems() {
        viewModelScope.launch {
            repository.clearCompletedShoppingListItems()
        }
    }

    // Close session
    fun closeSession(saveToHistory: Boolean) {
        viewModelScope.launch {
            repository.closeSession(saveToHistory)
            closeSheet()
        }
    }

    // Catalog
    fun updateCatalogItem(item: CatalogItem) {
        viewModelScope.launch {
            repository.updateCatalogItem(item)
        }
    }

    fun deleteCatalogItem(code: String, storeId: String) {
        viewModelScope.launch {
            repository.deleteCatalogItem(code, storeId)
        }
    }

    fun setWeighedBarcodeEnabled(enabled: Boolean) {
        prefs.setWeighedBarcodeEnabled(enabled)
    }

    fun setSoundEnabled(enabled: Boolean) {
        prefs.setSoundEnabled(enabled)
    }

    fun deleteHistorySession(id: String) {
        viewModelScope.launch {
            repository.deleteSession(id)
        }
    }

    override fun onCleared() {
        super.onCleared()
        soundFeedback.release()
    }
}
