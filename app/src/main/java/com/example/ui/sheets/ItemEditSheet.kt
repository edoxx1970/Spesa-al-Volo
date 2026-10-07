package com.example.ui.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BrandGoGreenDark
import com.example.ui.theme.BrandGoGreenLight
import com.example.ui.theme.BrandLaserRedLight
import com.example.ui.viewmodel.ActiveSheet
import com.example.util.BarcodeParser
import com.example.util.FormatUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemEditSheet(
    data: ActiveSheet.EditItem,
    onDismiss: () -> Unit,
    onSave: (name: String, priceCents: Int, quantity: Int, rememberPrice: Boolean) -> Unit,
    onDelete: () -> Unit,
    onOpenPriceTagReader: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember(data.initialName) { mutableStateOf(data.initialName) }
    var priceText by remember(data.initialPriceCents) {
        mutableStateOf(if (data.initialPriceCents > 0) FormatUtils.formatEuro(data.initialPriceCents) else "")
    }
    var quantity by remember(data.initialQty) { mutableIntStateOf(maxOf(1, data.initialQty)) }
    var rememberPrice by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier.testTag("item_edit_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Sheet Title
            Text(
                text = when {
                    data.isManual -> "Articolo senza codice"
                    data.isNew -> "Codice nuovo: quanto costa?"
                    else -> "Modifica articolo"
                },
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Subtitle with code info
            if (!data.isManual && data.code.isNotEmpty()) {
                Text(
                    text = BarcodeParser.codeLabel(data.code, data.showCode),
                    fontSize = 12.5.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (data.isManual) {
                Text(
                    text = "Frutta sfusa, pane, un prezzo scritto a mano…",
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Name input
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    errorMessage = null
                },
                label = { Text("Nome prodotto") },
                placeholder = { Text("Es. Latte intero 1 L") },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_item_name")
            )

            // Price & Quantity row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Price input
                OutlinedTextField(
                    value = priceText,
                    onValueChange = {
                        priceText = it
                        errorMessage = null
                    },
                    label = { Text("Prezzo") },
                    placeholder = { Text("0,00") },
                    prefix = { Text("€ ", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_item_price")
                )

                // Quantity Stepper
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Quantità",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        OutlinedButton(
                            onClick = { quantity = maxOf(if (data.isNew) 1 else 0, quantity - 1) },
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("btn_item_qty_minus")
                        ) {
                            Text(text = "−", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }

                        Text(
                            text = "$quantity",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        )

                        OutlinedButton(
                            onClick = { quantity++ },
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier
                                .size(38.dp)
                                .testTag("btn_item_qty_plus")
                        ) {
                            Text(text = "+", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Cheaper elsewhere info
            if (!data.elseWhereNote.isNullOrBlank()) {
                Text(
                    text = data.elseWhereNote,
                    fontSize = 12.sp,
                    color = BrandGoGreenDark,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // "Leggi il prezzo dal cartellino" button
            OutlinedButton(
                onClick = onOpenPriceTagReader,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("btn_read_price_tag")
            ) {
                Icon(imageVector = Icons.Default.Sell, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Leggi il prezzo dal cartellino (OCR)",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Remember in catalog checkbox
            if (!data.isManual) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Checkbox(
                        checked = rememberPrice,
                        onCheckedChange = { rememberPrice = it },
                        colors = CheckboxDefaults.colors(checkedColor = BrandGoGreenLight)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (data.isNew) {
                            "Ricorda il prezzo nel listino per questo negozio"
                        } else {
                            "Aggiorna anche il prezzo salvato nel listino"
                        },
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Error message if any
            if (errorMessage != null) {
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Actions row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!data.isNew && data.cartItemId != null) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = BrandLaserRedLight),
                        modifier = Modifier.testTag("btn_delete_item")
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Elimina", fontSize = 13.sp)
                    }
                }

                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                ) {
                    Text("Annulla", fontSize = 14.sp)
                }

                Button(
                    onClick = {
                        val parsedCents = FormatUtils.parsePriceToCents(priceText)
                        if (parsedCents == null || parsedCents <= 0) {
                            errorMessage = "Inserisci un prezzo valido (es. 1,29)"
                            return@Button
                        }
                        onSave(name.trim(), parsedCents, quantity, rememberPrice)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandGoGreenLight,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("btn_save_item")
                ) {
                    Text(
                        text = if (data.isNew) "Aggiungi" else "Salva",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
