package com.example.util

import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

object FormatUtils {

    private val STOP_WORDS = setOf(
        "di", "del", "della", "dei", "delle", "degli", "al", "alla", "allo", "ai", "agli", "alle",
        "con", "per", "da", "dal", "il", "lo", "la", "gli", "le", "un", "una", "uno",
        "gr", "kg", "ml", "cl", "lt", "l", "pz", "conf", "g"
    )

    fun formatEuro(cents: Int): String {
        val euros = cents / 100.0
        return String.format(Locale.ITALIAN, "%.2f", euros)
    }

    fun formatEuroWithSymbol(cents: Int): String {
        return "€ ${formatEuro(cents)}"
    }

    fun parsePriceToCents(str: String?): Int? {
        if (str.isNullOrBlank()) return null
        val cleaned = str.replace("€", "").replace(" ", "").replace(",", ".").trim()
        val num = cleaned.toDoubleOrNull() ?: return null
        if (num <= 0.0 || num > 99999.0) return null
        return (num * 100.0).roundToInt()
    }

    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALIAN)
        return sdf.format(Date(timestamp))
    }

    fun formatHistorySummaryDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("EEE d MMM, HH:mm", Locale.ITALIAN)
        return sdf.format(Date(timestamp))
    }

    fun formatMonthName(monthIndex: Int): String {
        val months = arrayOf(
            "GENNAIO", "FEBBRAIO", "MARZO", "APRILE", "MAGGIO", "GIUGNO",
            "LUGLIO", "AGOSTO", "SETTEMBRE", "OTTOBRE", "NOVEMBRE", "DICEMBRE"
        )
        return months.getOrElse(monthIndex) { "" }
    }

    fun formatDistance(meters: Float): String {
        return if (meters < 1000f) {
            val rounded = (maxOf(10, (meters / 10f).roundToInt() * 10))
            "$rounded m"
        } else {
            val km = meters / 1000f
            String.format(Locale.ITALIAN, "%.1f km", km)
        }
    }

    private fun normTxt(s: String?): String {
        if (s == null) return ""
        val normalized = Normalizer.normalize(s.lowercase(Locale.ITALIAN), Normalizer.Form.NFD)
        return normalized.replace(Regex("""\p{M}"""), "")
            .replace(Regex("""[^a-z0-9 ]+"""), " ")
    }

    private fun stem(w: String): String {
        return if (w.length > 3) w.replace(Regex("""[aeiou]$"""), "") else w
    }

    private fun wordsOf(s: String): List<String> {
        return normTxt(s).split(Regex("""\s+"""))
            .filter { it.length >= 3 && !STOP_WORDS.contains(it) && !it.first().isDigit() }
            .map { stem(it) }
    }

    /**
     * Checks if a shopping list item like "latte" matches a product name like "Latte intero 1 L".
     */
    fun nameMatch(listText: String, productName: String): Boolean {
        val a = wordsOf(listText)
        val b = wordsOf(productName)
        if (a.isEmpty() || b.isEmpty()) return false
        return a.all { wordA ->
            b.any { wordB ->
                wordA == wordB || (wordA.length >= 4 && wordB.startsWith(wordA)) || (wordB.length >= 4 && wordA.startsWith(wordB))
            }
        }
    }
}
