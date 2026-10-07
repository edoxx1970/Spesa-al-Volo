package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCartCheckout
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CartItem
import com.example.data.model.CartTotals
import com.example.data.model.CheaperComparison
import com.example.ui.theme.BrandGoGreenDark
import com.example.ui.theme.BrandGoGreenLight
import com.example.ui.theme.BrandGoGreenLightSoft
import com.example.ui.theme.BrandLaserRedDark
import com.example.ui.theme.BrandLaserRedLight
import com.example.ui.theme.BrandLaserRedLightSoft
import com.example.ui.theme.BrandWarnLight
import com.example.ui.theme.BrandWarnLightSoft
import com.example.util.FormatUtils

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReceiptSection(
    cartItems: List<CartItem>,
    totals: CartTotals,
    isDemoMode: Boolean,
    cheaperComparisons: Map<String, CheaperComparison>,
    onClearDemo: () -> Unit,
    onItemClick: (CartItem) -> Unit,
    onIncrement: (String) -> Unit,
    onDecrement: (String) -> Unit,
    onAddManualItem: () -> Unit,
    onCloseSessionClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onCatalogClick: () -> Unit,
    onCopyListClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeItems = remember(cartItems) { cartItems.filter { it.quantity > 0 } }
    val crossedOutItems = remember(cartItems) { cartItems.filter { it.quantity == 0 } }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
            .testTag("receipt_section"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Receipt Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CONTO DELLA SPESA",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.2.sp
            )
            Text(
                text = FormatUtils.formatDate(System.currentTimeMillis()),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        // Demo Notice Banner
        AnimatedVisibility(visible = isDemoMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(BrandWarnLightSoft)
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Conto d'esempio",
                        fontWeight = FontWeight.Bold,
                        color = BrandWarnLight,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Serve a mostrarti come funziona: alla prima lettura o tocco si svuota da solo.",
                        color = BrandWarnLight,
                        fontSize = 12.sp,
                        lineHeight = 15.sp
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = onClearDemo,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandWarnLight),
                    modifier = Modifier.testTag("btn_clear_demo")
                ) {
                    Text(text = "Svuota", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Active Cart Lines
        if (activeItems.isEmpty() && crossedOutItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Il carrello è vuoto",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Avvia lo scanner e spara il primo codice a barre o QR.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                activeItems.forEach { item ->
                    ReceiptItemRow(
                        item = item,
                        cheaper = cheaperComparisons[item.code],
                        onItemClick = { onItemClick(item) },
                        onIncrement = { onIncrement(item.id) },
                        onDecrement = { onDecrement(item.id) }
                    )
                }
            }
        }

        // Crossed-out / Depennati items section
        if (crossedOutItems.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "DEPENNATI · NON CONTANO NEL TOTALE",
                fontSize = 10.5.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
                letterSpacing = 1.sp
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                crossedOutItems.forEach { item ->
                    ReceiptItemRow(
                        item = item,
                        cheaper = null,
                        onItemClick = { onItemClick(item) },
                        onIncrement = { onIncrement(item.id) },
                        onDecrement = { onDecrement(item.id) }
                    )
                }
            }
        }

        // Add manual item without barcode
        OutlinedButton(
            onClick = onAddManualItem,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .testTag("btn_add_free_item")
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "+ Articolo senza codice", fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        // Totals Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "TOTALE",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "PEZZI: ${totals.totalPieces}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = FormatUtils.formatEuroWithSymbol(totals.totalCents),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Action Buttons Grid
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onCloseSessionClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandGoGreenLight,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_close_session")
            ) {
                Icon(imageVector = Icons.Default.ShoppingCartCheckout, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Chiudi la spesa", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onHistoryClick,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("btn_open_history")
                ) {
                    Icon(imageVector = Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Storico", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = onCatalogClick,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("btn_open_catalog")
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ListAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Listino", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = onCopyListClick,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("btn_copy_list")
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Copia", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun ReceiptItemRow(
    item: CartItem,
    cheaper: CheaperComparison?,
    onItemClick: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isOff = item.quantity == 0

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isOff) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f) else Color.Transparent)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Column: Item Name, Code & Meta tags
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onItemClick)
                .padding(end = 8.dp)
        ) {
            Text(
                text = item.name,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.5.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = if (isOff) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (isOff) TextDecoration.LineThrough else TextDecoration.None
            )

            // Meta tags row: code, weight, tag, and cheaper alert
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 2.dp)
            ) {
                if (item.tag != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = item.tag,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = item.weight ?: item.showCode,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "${maxOf(item.quantity, 1)} × ${FormatUtils.formatEuro(item.priceCents)} €",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (cheaper != null && !isOff && cheaper.priceCents < item.priceCents) {
                    Text(
                        text = "${cheaper.storeName} ${FormatUtils.formatEuro(cheaper.priceCents)} €",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandGoGreenDark
                    )
                }
            }
        }

        // Right Column: Sum & Stepper
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = FormatUtils.formatEuroWithSymbol(item.totalPriceCents),
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.5.sp,
                color = if (isOff) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                textDecoration = if (isOff) TextDecoration.LineThrough else TextDecoration.None
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Stepper controls (+ / Togli / Rimetti)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (isOff) {
                    OutlinedButton(
                        onClick = onIncrement,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(text = "Rimetti", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    // Minus / Togli button
                    OutlinedButton(
                        onClick = onDecrement,
                        shape = RoundedCornerShape(6.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = BrandLaserRedLight
                        ),
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("btn_minus_${item.id}")
                    ) {
                        Text(text = "Togli", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Text(
                        text = "${item.quantity}",
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    // Plus button
                    OutlinedButton(
                        onClick = onIncrement,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier
                            .height(32.dp)
                            .width(36.dp)
                            .testTag("btn_plus_${item.id}")
                    ) {
                        Text(text = "+", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
