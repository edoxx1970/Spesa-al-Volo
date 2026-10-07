package com.example.ui.sheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CatalogItem
import com.example.data.model.Store
import com.example.ui.theme.BrandGoGreenLight
import com.example.ui.theme.BrandLaserRedLight
import com.example.util.BarcodeParser
import com.example.util.FormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogSheet(
    activeStore: Store?,
    catalogItems: List<CatalogItem>,
    weighedEnabled: Boolean,
    soundEnabled: Boolean,
    onEditItem: (CatalogItem) -> Unit,
    onDeleteItem: (code: String, storeId: String) -> Unit,
    onToggleWeighed: (Boolean) -> Unit,
    onToggleSound: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }

    val filteredItems = remember(catalogItems, searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) {
            catalogItems
        } else {
            catalogItems.filter {
                it.name.lowercase().contains(q) || it.code.lowercase().contains(q)
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier.testTag("catalog_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (activeStore != null) "Listino · ${activeStore.name}" else "Listino prezzi",
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "${catalogItems.size} ${if (catalogItems.size == 1) "prezzo salvato" else "prezzi salvati"}" +
                        if (activeStore != null) " per questo negozio" else " (nessun negozio scelto)",
                fontSize = 12.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Search input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Cerca per nome o codice", fontSize = 13.5.sp) },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_catalog_search")
            )

            // Catalog list
            if (filteredItems.isEmpty()) {
                Text(
                    text = if (searchQuery.isNotEmpty()) "Nessun articolo trovato per \"$searchQuery\"."
                    else "Ancora nessun prezzo salvato. Quando spari un codice nuovo e scrivi il prezzo, lo trovi qui.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(minOf(240.dp, (filteredItems.size * 56).dp)),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(filteredItems, key = { "${it.code}_${it.storeId}" }) { item ->
                        val isPlu = item.code.startsWith("plu:")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onEditItem(item) }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = if (isPlu) "Banco · art. ${item.code.removePrefix("plu:")}" else BarcodeParser.codeLabel(item.code),
                                    fontSize = 11.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isPlu) "a peso" else FormatUtils.formatEuroWithSymbol(item.priceCents),
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(end = 8.dp)
                                )

                                IconButton(
                                    onClick = { onEditItem(item) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Modifica",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                IconButton(
                                    onClick = { onDeleteItem(item.code, item.storeId) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Elimina",
                                        tint = BrandLaserRedLight,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    }
                }
            }

            // Settings Section
            Text(
                text = "IMPOSTAZIONI",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleWeighed(!weighedEnabled) }
            ) {
                Checkbox(
                    checked = weighedEnabled,
                    onCheckedChange = { onToggleWeighed(it) },
                    colors = CheckboxDefaults.colors(checkedColor = BrandGoGreenLight)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Etichette di banco e a peso (prefisso 2): leggi il prezzo dalle cifre 8–12 del codice",
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 16.sp
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleSound(!soundEnabled) }
            ) {
                Checkbox(
                    checked = soundEnabled,
                    onCheckedChange = { onToggleSound(it) },
                    colors = CheckboxDefaults.colors(checkedColor = BrandGoGreenLight)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Bip sonoro e vibrazione tattile a ogni lettura",
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Close button
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandGoGreenLight,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Chiudi", fontWeight = FontWeight.Bold)
            }
        }
    }
}
