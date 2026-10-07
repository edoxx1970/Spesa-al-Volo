package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CartTotals
import com.example.ui.theme.BrandGoGreenDark
import com.example.ui.theme.BrandLaserRedDark
import com.example.ui.theme.BrandWarnDark
import com.example.ui.theme.TillInk
import com.example.ui.theme.TillMuted
import com.example.ui.theme.TillViewBgLight
import com.example.util.FormatUtils

@Composable
fun TillHeader(
    totals: CartTotals,
    budgetCents: Int,
    onBudgetClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Pulse animation when total changes
    val scale = remember { Animatable(1f) }
    LaunchedEffect(totals.totalCents) {
        if (totals.totalCents > 0) {
            scale.animateTo(1.05f, animationSpec = tween(120, easing = LinearEasing))
            scale.animateTo(1f, animationSpec = tween(150, easing = LinearEasing))
        }
    }

    val budgetProgress by animateFloatAsState(
        targetValue = if (budgetCents > 0) {
            (totals.totalCents.toFloat() / budgetCents.toFloat()).coerceIn(0f, 1f)
        } else 0f,
        label = "budgetProgress"
    )

    val budgetRatio = if (budgetCents > 0) totals.totalCents.toFloat() / budgetCents.toFloat() else 0f
    val budgetStatusColor by animateColorAsState(
        targetValue = when {
            budgetCents <= 0 -> Color.Transparent
            budgetRatio > 1f -> BrandLaserRedDark
            budgetRatio >= 0.9f -> BrandWarnDark
            else -> BrandGoGreenDark
        },
        label = "budgetStatusColor"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(TillViewBgLight)
            .padding(horizontal = 18.dp, vertical = 14.dp)
            .testTag("till_header")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "TOTALE EURO",
                color = TillMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.8.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // Giant amount display
                Row(
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.scale(scale.value)
                ) {
                    Text(
                        text = "€ ",
                        color = TillMuted,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Text(
                        text = FormatUtils.formatEuro(totals.totalCents),
                        color = TillInk,
                        fontSize = 42.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = (-0.5).sp,
                        modifier = Modifier.testTag("till_total_amount")
                    )
                }

                // Metadata: pieces and rows count
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.padding(bottom = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${totals.totalPieces}",
                            color = TillInk,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp
                        )
                        Text(
                            text = " pezzi",
                            color = TillMuted,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${totals.totalRows}",
                            color = TillInk,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp
                        )
                        Text(
                            text = " articoli",
                            color = TillMuted,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Budget bar & button
            Spacer(modifier = Modifier.height(8.dp))
            if (budgetCents > 0) {
                LinearProgressIndicator(
                    progress = { budgetProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(99.dp)),
                    color = budgetStatusColor,
                    trackColor = Color.White.copy(alpha = 0.12f)
                )

                Spacer(modifier = Modifier.height(6.dp))

                val remaining = budgetCents - totals.totalCents
                val budgetNote = if (remaining >= 0) {
                    "restano ${FormatUtils.formatEuroWithSymbol(remaining)}"
                } else {
                    "superato di ${FormatUtils.formatEuroWithSymbol(-remaining)}"
                }

                Text(
                    text = "Budget ${FormatUtils.formatEuroWithSymbol(budgetCents)} · $budgetNote",
                    color = budgetStatusColor,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable(onClick = onBudgetClick)
                        .testTag("till_budget_button")
                )
            } else {
                Text(
                    text = "+ Imposta un budget",
                    color = TillMuted,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clickable(onClick = onBudgetClick)
                        .testTag("till_budget_button")
                )
            }
        }
    }
}
