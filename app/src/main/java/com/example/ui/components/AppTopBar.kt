package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BrandGoGreenLight
import com.example.ui.theme.BrandLaserRedLight
import com.example.ui.theme.BrandLaserRedLightSoft

@Composable
fun AppTopBar(
    isCameraActive: Boolean,
    isRemoveMode: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Brand logo & title
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.testTag("app_brand")
        ) {
            // Barcode glyph with laser line
            Canvas(modifier = Modifier.size(width = 24.dp, height = 20.dp)) {
                val barW = 2.dp.toPx()
                var x = 0f
                val barColor = Color(0xFF14202A)
                while (x < size.width) {
                    drawLine(
                        color = barColor,
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = barW
                    )
                    x += 3.5.dp.toPx()
                }
                // Laser line across glyph
                drawLine(
                    color = BrandLaserRedLight,
                    start = Offset(-2f, size.height / 2),
                    end = Offset(size.width + 2f, size.height / 2),
                    strokeWidth = 2.dp.toPx()
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = "Spesa al Volo",
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp,
                letterSpacing = (-0.2).sp,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Status badge chip
        val (chipText, chipColor, chipBg) = when {
            isRemoveMode -> Triple("Togli attivo", BrandLaserRedLight, BrandLaserRedLightSoft)
            isCameraActive -> Triple("In ascolto", BrandGoGreenLight, MaterialTheme.colorScheme.surface)
            else -> Triple("Scanner spento", MaterialTheme.colorScheme.onSurfaceVariant, MaterialTheme.colorScheme.surface)
        }

        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val blinkAlpha by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 0.25f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "blink"
        )

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(99.dp))
                .background(chipBg)
                .border(1.dp, if (isRemoveMode) BrandLaserRedLight else MaterialTheme.colorScheme.outline, RoundedCornerShape(99.dp))
                .padding(horizontal = 10.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(
                            if (isCameraActive && !isRemoveMode) chipColor.copy(alpha = blinkAlpha)
                            else chipColor
                        )
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = chipText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = chipColor
                )
            }
        }
    }
}
