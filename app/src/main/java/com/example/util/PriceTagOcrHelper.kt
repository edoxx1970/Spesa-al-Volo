package com.example.util

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

data class CandidatePrice(
    val cents: Int,
    val score: Float,
    val text: String
)

object PriceTagOcrHelper {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    suspend fun analyzeBitmap(bitmap: Bitmap): List<CandidatePrice> = suspendCancellableCoroutine { continuation ->
        val image = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val prices = extractPricesFromText(visionText)
                continuation.resume(prices)
            }
            .addOnFailureListener {
                continuation.resume(emptyList())
            }
    }

    private fun extractPricesFromText(visionText: Text): List<CandidatePrice> {
        val found = mutableListOf<CandidatePrice>()
        val regex = Regex("""(?i)(?:€|\b)(\d{1,3})[.,](\d{2})(?!\d)""")

        for (block in visionText.textBlocks) {
            for (line in block.lines) {
                val box = line.boundingBox
                val height = box?.height()?.toFloat() ?: 20f

                val text = line.text
                val matches = regex.findAll(text)
                for (match in matches) {
                    val whole = match.groupValues[1].toIntOrNull() ?: continue
                    val centsPart = match.groupValues[2].toIntOrNull() ?: continue
                    val totalCents = whole * 100 + centsPart

                    if (totalCents in 1..99999) {
                        val existing = found.find { it.cents == totalCents }
                        if (existing == null) {
                            found.add(CandidatePrice(cents = totalCents, score = height, text = match.value))
                        } else if (height > existing.score) {
                            found.remove(existing)
                            found.add(CandidatePrice(cents = totalCents, score = height, text = match.value))
                        }
                    }
                }
            }
        }

        return found.sortedByDescending { it.score }.take(6)
    }
}
