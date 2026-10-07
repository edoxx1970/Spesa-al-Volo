package com.example.ui.screens

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.CartTotals
import com.example.ui.components.AppToastBanner
import com.example.ui.components.AppTopBar
import com.example.ui.components.ReceiptSection
import com.example.ui.components.ScannerSection
import com.example.ui.components.ShoppingListCard
import com.example.ui.components.StoreBar
import com.example.ui.components.TillHeader
import com.example.ui.sheets.BudgetSheet
import com.example.ui.sheets.CatalogSheet
import com.example.ui.sheets.ConfirmCloseDialog
import com.example.ui.sheets.CopyReceiptSheet
import com.example.ui.sheets.HistorySheet
import com.example.ui.sheets.ItemEditSheet
import com.example.ui.sheets.PriceTagReaderSheet
import com.example.ui.sheets.ShoppingListSheet
import com.example.ui.sheets.StoreSelectorSheet
import com.example.ui.viewmodel.ActiveSheet
import com.example.ui.viewmodel.ShoppingViewModel

@Composable
fun SpesaAlVoloScreen(
    viewModel: ShoppingViewModel,
    modifier: Modifier = Modifier
) {
    val cartItems by viewModel.cartItems.collectAsState()
    val totals by viewModel.cartTotals.collectAsState()
    val shoppingList by viewModel.shoppingList.collectAsState()
    val historySessions by viewModel.historySessions.collectAsState()
    val stores by viewModel.stores.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val isRemoveMode by viewModel.isRemoveMode.collectAsState()
    val isDemoMode by viewModel.isDemoMode.collectAsState()
    val isCameraActive by viewModel.isCameraActive.collectAsState()
    val isTorchOn by viewModel.isTorchOn.collectAsState()
    val zoomRatio by viewModel.zoomRatio.collectAsState()
    val manualCodeInput by viewModel.manualCodeInput.collectAsState()
    val activeSheet by viewModel.activeSheet.collectAsState()
    val toastState by viewModel.toastState.collectAsState()
    val cheaperComparisons by viewModel.cheaperComparisons.collectAsState()
    val candidatePrices by viewModel.candidatePrices.collectAsState()
    val nearbyStores by viewModel.nearbyStores.collectAsState()
    val isLocatingStores by viewModel.isLocatingStores.collectAsState()
    val isOcrReading by viewModel.isOcrReading.collectAsState()

    val activeStore = stores.find { it.id == settings.currentStoreId }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            AppToastBanner(
                toastState = toastState,
                onUndo = { viewModel.undo() },
                onDismiss = { viewModel.clearToast() }
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
        ) {
            val isExpanded = maxWidth >= 840.dp
            val scrollState = rememberScrollState()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // Brand Header with animated status chip
                AppTopBar(
                    isCameraActive = isCameraActive,
                    isRemoveMode = isRemoveMode
                )

                // Store Selection Bar
                StoreBar(
                    activeStore = activeStore,
                    onClick = { viewModel.openSheet(ActiveSheet.StoreSelector) }
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (isExpanded) {
                    // Two-Pane Tablet Layout: Left side Scanner & Till, Right side Receipt
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            TillHeader(
                                totals = totals,
                                budgetCents = settings.budgetCents,
                                onBudgetClick = { viewModel.openSheet(ActiveSheet.Budget) }
                            )

                            ScannerSection(
                                isCameraActive = isCameraActive,
                                isTorchOn = isTorchOn,
                                zoomRatio = zoomRatio,
                                isRemoveMode = isRemoveMode,
                                manualCodeInput = manualCodeInput,
                                onManualCodeChange = { viewModel.manualCodeInput.value = it },
                                onSubmitManualCode = { viewModel.submitManualCode() },
                                onToggleCamera = { viewModel.toggleCamera() },
                                onToggleTorch = { viewModel.toggleTorch() },
                                onToggleZoom = { viewModel.toggleZoom() },
                                onToggleRemoveMode = { viewModel.toggleRemoveMode() },
                                onBarcodeDetected = { viewModel.onBarcodeDetected(it) },
                                onImageSelected = { viewModel.scanImageUri(it) }
                            )

                            ShoppingListCard(
                                items = shoppingList,
                                onOpenList = { viewModel.openSheet(ActiveSheet.ShoppingList) }
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(1.1f)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            ReceiptSection(
                                cartItems = cartItems,
                                totals = totals,
                                isDemoMode = isDemoMode,
                                cheaperComparisons = cheaperComparisons,
                                onClearDemo = { viewModel.clearDemo() },
                                onItemClick = { viewModel.openEditCartItem(it) },
                                onIncrement = { viewModel.incrementItem(it) },
                                onDecrement = { viewModel.decrementItem(it) },
                                onAddManualItem = { viewModel.openManualItemSheet() },
                                onCloseSessionClick = { viewModel.openSheet(ActiveSheet.ConfirmCloseSession) },
                                onHistoryClick = { viewModel.openSheet(ActiveSheet.History) },
                                onCatalogClick = {
                                    val storeId = settings.currentStoreId ?: "_"
                                    viewModel.openSheet(ActiveSheet.Catalog)
                                },
                                onCopyListClick = { viewModel.openSheet(ActiveSheet.CopyReceipt) }
                            )
                        }
                    }
                } else {
                    // Single Column Phone Layout
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        TillHeader(
                            totals = totals,
                            budgetCents = settings.budgetCents,
                            onBudgetClick = { viewModel.openSheet(ActiveSheet.Budget) }
                        )

                        ScannerSection(
                            isCameraActive = isCameraActive,
                            isTorchOn = isTorchOn,
                            zoomRatio = zoomRatio,
                            isRemoveMode = isRemoveMode,
                            manualCodeInput = manualCodeInput,
                            onManualCodeChange = { viewModel.manualCodeInput.value = it },
                            onSubmitManualCode = { viewModel.submitManualCode() },
                            onToggleCamera = { viewModel.toggleCamera() },
                            onToggleTorch = { viewModel.toggleTorch() },
                            onToggleZoom = { viewModel.toggleZoom() },
                            onToggleRemoveMode = { viewModel.toggleRemoveMode() },
                            onBarcodeDetected = { viewModel.onBarcodeDetected(it) },
                            onImageSelected = { viewModel.scanImageUri(it) }
                        )

                        ShoppingListCard(
                            items = shoppingList,
                            onOpenList = { viewModel.openSheet(ActiveSheet.ShoppingList) }
                        )

                        ReceiptSection(
                            cartItems = cartItems,
                            totals = totals,
                            isDemoMode = isDemoMode,
                            cheaperComparisons = cheaperComparisons,
                            onClearDemo = { viewModel.clearDemo() },
                            onItemClick = { viewModel.openEditCartItem(it) },
                            onIncrement = { viewModel.incrementItem(it) },
                            onDecrement = { viewModel.decrementItem(it) },
                            onAddManualItem = { viewModel.openManualItemSheet() },
                            onCloseSessionClick = { viewModel.openSheet(ActiveSheet.ConfirmCloseSession) },
                            onHistoryClick = { viewModel.openSheet(ActiveSheet.History) },
                            onCatalogClick = { viewModel.openSheet(ActiveSheet.Catalog) },
                            onCopyListClick = { viewModel.openSheet(ActiveSheet.CopyReceipt) }
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }

    // Active bottom sheets
    when (val sheet = activeSheet) {
        is ActiveSheet.EditItem -> {
            ItemEditSheet(
                data = sheet,
                onDismiss = { viewModel.closeSheet() },
                onSave = { name, priceCents, quantity, rememberPrice ->
                    viewModel.saveItemFromSheet(
                        isNew = sheet.isNew,
                        isManual = sheet.isManual,
                        cartItemId = sheet.cartItemId,
                        code = sheet.code,
                        showCode = sheet.showCode,
                        name = name,
                        priceCents = priceCents,
                        quantity = quantity,
                        kind = sheet.kind,
                        rememberPrice = rememberPrice
                    )
                },
                onDelete = { viewModel.deleteItemFromSheet(sheet.cartItemId) },
                onOpenPriceTagReader = { viewModel.openSheet(ActiveSheet.PriceTagReader) }
            )
        }

        is ActiveSheet.PriceTagReader -> {
            PriceTagReaderSheet(
                candidatePrices = candidatePrices,
                isReading = isOcrReading,
                onPriceSelected = { selectedCents ->
                    val cur = activeSheet
                    viewModel.closeSheet()
                    // Reopen edit sheet with selected price
                    viewModel.openSheet(
                        ActiveSheet.EditItem(
                            isNew = true,
                            code = "",
                            showCode = "",
                            initialName = "",
                            initialPriceCents = selectedCents,
                            initialQty = 1,
                            kind = "manuale",
                            isManual = true
                        )
                    )
                },
                onAnalyzeBitmap = { viewModel.analyzePriceTagBitmap(it) },
                onDismiss = { viewModel.closeSheet() }
            )
        }

        is ActiveSheet.StoreSelector -> {
            StoreSelectorSheet(
                activeStoreId = settings.currentStoreId,
                savedStores = stores,
                nearbyStores = nearbyStores,
                isLocating = isLocatingStores,
                onSelectStore = { viewModel.selectStore(it) },
                onAddStore = { name, address, lat, lon -> viewModel.addStore(name, address, lat, lon) },
                onDeleteStore = { viewModel.deleteStore(it) },
                onRefreshNearby = { lat, lon, acc -> viewModel.refreshNearbyStores(lat, lon, acc) },
                onDismiss = { viewModel.closeSheet() }
            )
        }

        is ActiveSheet.Budget -> {
            BudgetSheet(
                initialBudgetCents = settings.budgetCents,
                onSaveBudget = { viewModel.setBudget(it) },
                onDismiss = { viewModel.closeSheet() }
            )
        }

        is ActiveSheet.ShoppingList -> {
            ShoppingListSheet(
                items = shoppingList,
                onAddItemText = { viewModel.addShoppingListItems(it) },
                onToggleItem = { id, checked -> viewModel.toggleShoppingListItem(id, checked) },
                onDeleteItem = { viewModel.deleteShoppingListItem(it) },
                onClearCompleted = { viewModel.clearCompletedShoppingListItems() },
                onDismiss = { viewModel.closeSheet() }
            )
        }

        is ActiveSheet.History -> {
            HistorySheet(
                sessions = historySessions,
                onDeleteSession = { viewModel.deleteHistorySession(it) },
                onGenerateCsv = { viewModel.repository.generateCsvExport() },
                onDismiss = { viewModel.closeSheet() }
            )
        }

        is ActiveSheet.Catalog -> {
            val storeId = settings.currentStoreId ?: "_"
            val catalogItems by viewModel.repository.getCatalogForStore(storeId).collectAsState(initial = emptyList())
            CatalogSheet(
                activeStore = activeStore,
                catalogItems = catalogItems,
                weighedEnabled = settings.weighedBarcodeEnabled,
                soundEnabled = settings.soundEnabled,
                onEditItem = { item ->
                    viewModel.openSheet(
                        ActiveSheet.EditItem(
                            isNew = false,
                            cartItemId = null,
                            code = item.code,
                            showCode = item.code,
                            initialName = item.name,
                            initialPriceCents = item.priceCents,
                            initialQty = 1,
                            kind = item.kind
                        )
                    )
                },
                onDeleteItem = { code, sId -> viewModel.deleteCatalogItem(code, sId) },
                onToggleWeighed = { viewModel.setWeighedBarcodeEnabled(it) },
                onToggleSound = { viewModel.setSoundEnabled(it) },
                onDismiss = { viewModel.closeSheet() }
            )
        }

        is ActiveSheet.CopyReceipt -> {
            val receiptText = viewModel.repository.generateReceiptText(cartItems, totals, activeStore?.name)
            CopyReceiptSheet(
                receiptText = receiptText,
                onDismiss = { viewModel.closeSheet() }
            )
        }

        is ActiveSheet.ConfirmCloseSession -> {
            ConfirmCloseDialog(
                totals = totals,
                onDismiss = { viewModel.closeSheet() },
                onClearWithoutSaving = { viewModel.closeSession(saveToHistory = false) },
                onSaveAndClose = { viewModel.closeSession(saveToHistory = true) }
            )
        }

        ActiveSheet.None -> {}
    }
}
