package com.example.ui.sheets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CartTotals
import com.example.ui.theme.BrandGoGreenLight
import com.example.ui.theme.BrandLaserRedLight
import com.example.util.FormatUtils

@Composable
fun ConfirmCloseDialog(
    totals: CartTotals,
    onDismiss: () -> Unit,
    onClearWithoutSaving: () -> Unit,
    onSaveAndClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Chiudi la spesa",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Hai finito? Salvo nello storico la spesa da ${FormatUtils.formatEuroWithSymbol(totals.totalCents)} (${totals.totalPieces} pezzi) e svuoto il conto. I prezzi del listino restano.",
                    fontSize = 13.5.sp,
                    lineHeight = 18.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSaveAndClose,
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandGoGreenLight,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("btn_confirm_save_close")
            ) {
                Text("Salva e chiudi", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = onClearWithoutSaving,
                    colors = ButtonDefaults.textButtonColors(contentColor = BrandLaserRedLight),
                    modifier = Modifier.testTag("btn_clear_without_saving")
                ) {
                    Text("Svuota")
                }
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Annulla")
                }
            }
        },
        shape = RoundedCornerShape(16.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier.testTag("confirm_close_dialog")
    )
}
