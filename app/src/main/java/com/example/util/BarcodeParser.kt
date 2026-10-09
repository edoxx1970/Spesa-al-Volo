package com.example.util

import com.google.mlkit.vision.barcode.common.Barcode
import java.net.URI
import java.net.URLDecoder
import kotlin.math.pow
import kotlin.math.roundToInt

data class ParsedScanResult(
    val key: String,
    val show: String,
    val kind: String, // "ean", "banco", "gs1", "qr", "manuale"
    val base: String? = null,
    val priceCents: Int? = null,
    val weightKg: String? = null,
    val plu: String? = null,
    val lotNumber: String? = null,
    val expirationDate: String? = null,
    val formatName: String = "EAN",
    val rawValue: String = key
)

object BarcodeParser {

    private const val GS = '\u001D'

    fun getFormatName(formatId: Int): String {
        return when (formatId) {
            Barcode.FORMAT_EAN_13 -> "EAN-13"
            Barcode.FORMAT_EAN_8 -> "EAN-8"
            Barcode.FORMAT_UPC_A -> "UPC-A"
            Barcode.FORMAT_UPC_E -> "UPC-E"
            Barcode.FORMAT_CODE_128 -> "Code 128"
            Barcode.FORMAT_CODE_39 -> "Code 39"
            Barcode.FORMAT_ITF -> "ITF-14"
            Barcode.FORMAT_QR_CODE -> "QR Code"
            Barcode.FORMAT_DATA_MATRIX -> "Data Matrix"
            else -> "Codice a barre"
        }
    }

    fun eanCheck(body: String): String {
        var s = 0
        val len = body.length
        for (i in 0 until len) {
            val digit = body[len - 1 - i].digitToIntOrNull() ?: 0
            s += digit * (if (i % 2 == 0) 3 else 1)
        }
        return (((10 - (s % 10)) % 10)).toString()
    }

    fun eanValid(code: String): Boolean {
        if (!code.matches(Regex("""^(\d{8}|\d{13})$"""))) return false
        val body = code.substring(0, code.length - 1)
        return eanCheck(body) == code.last().toString()
    }

    fun gtinToKey(g: String): String {
        val padded = g.padStart(14, '0')
        return when {
            padded.startsWith("000000") -> padded.substring(6) // GTIN-8
            padded[0] == '0' -> padded.substring(1) // EAN-13
            else -> padded
        }
    }

    fun parseWeighed(code: String, enabled: Boolean): ParsedScanResult? {
        if (!enabled || !code.matches(Regex("""^2\d{12}$""")) || !eanValid(code)) return null
        val priceStr = code.substring(7, 12)
        val price = priceStr.toIntOrNull() ?: return null
        if (price <= 0) return null
        val plu = code.substring(1, 7)
        return ParsedScanResult(
            key = code,
            show = code,
            kind = "banco",
            plu = plu,
            priceCents = price,
            formatName = "EAN-13 Banco",
            rawValue = code
        )
    }

    private fun formatGs1Date(yymmdd: String?): String? {
        if (yymmdd == null || yymmdd.length != 6 || !yymmdd.all { it.isDigit() }) return null
        val yy = yymmdd.substring(0, 2)
        val mm = yymmdd.substring(2, 4)
        val dd = yymmdd.substring(4, 6)
        return "$dd/$mm/20$yy"
    }

    private fun gs1Result(ai: Map<String, String>, defaultFormat: String = "GS1"): ParsedScanResult? {
        val gtin = ai["01"] ?: return null
        if (!gtin.matches(Regex("""^\d{14}$"""))) return null
        val base = gtinToKey(gtin)
        var price: Int? = null
        var weightStr: String? = null
        var wRaw = ""

        for ((k, v) in ai) {
            if (k.matches(Regex("""^392\d$""")) && v.matches(Regex("""^\d{1,15}$"""))) {
                val decimals = k[3].digitToInt()
                val parsed = v.toDoubleOrNull() ?: 0.0
                price = (parsed * 100.0 / 10.0.pow(decimals)).roundToInt()
            }
            if (k.matches(Regex("""^310\d$""")) && v.matches(Regex("""^\d{6}$"""))) {
                wRaw = v
                val decimals = k[3].digitToInt()
                val parsed = v.toDoubleOrNull() ?: 0.0
                val kg = parsed / 10.0.pow(decimals)
                weightStr = String.format(java.util.Locale.ITALIAN, "%.3f kg", kg)
            }
        }

        val lot = ai["10"]
        val expiry = formatGs1Date(ai["17"] ?: ai["15"])

        return if (price != null && price > 0) {
            ParsedScanResult(
                key = "$base#$price#$wRaw",
                show = base,
                kind = "gs1",
                base = base,
                priceCents = price,
                weightKg = weightStr,
                lotNumber = lot,
                expirationDate = expiry,
                formatName = defaultFormat,
                rawValue = "$base#$price#$wRaw"
            )
        } else {
            ParsedScanResult(
                key = base,
                show = base,
                kind = "ean",
                base = base,
                lotNumber = lot,
                expirationDate = expiry,
                formatName = defaultFormat,
                rawValue = base
            )
        }
    }

    private fun parseDigitalLink(s: String, formatName: String): ParsedScanResult? {
        if (!s.startsWith("http://", ignoreCase = true) && !s.startsWith("https://", ignoreCase = true)) {
            return null
        }
        return try {
            val uri = URI(s)
            val pathParts = uri.path?.split("/")?.filter { it.isNotEmpty() } ?: return null
            val i0 = pathParts.indexOfFirst { it == "01" || it == "gtin" }
            if (i0 < 0 || i0 + 1 >= pathParts.size) return null
            val gtinCandidate = pathParts[i0 + 1]
            if (!gtinCandidate.matches(Regex("""^\d{8,14}$"""))) return null

            val ai = mutableMapOf<String, String>()
            var i = i0
            while (i + 1 < pathParts.size) {
                val key = if (pathParts[i] == "gtin") "01" else pathParts[i]
                val value = try {
                    URLDecoder.decode(pathParts[i + 1], "UTF-8")
                } catch (e: Exception) {
                    pathParts[i + 1]
                }
                ai[key] = value
                i += 2
            }
            ai["01"] = gtinCandidate.padStart(14, '0')

            uri.query?.split("&")?.forEach { param ->
                val eq = param.indexOf('=')
                if (eq > 0) {
                    val k = param.substring(0, eq)
                    val v = param.substring(eq + 1)
                    if (k.matches(Regex("""^\d{2,4}$"""))) {
                        ai[k] = v
                    }
                }
            }
            gs1Result(ai, formatName)
        } catch (e: Exception) {
            null
        }
    }

    private fun aiInfo(str: String): Pair<String?, Int?> {
        if (str.length < 2) return Pair(null, null)
        val two = str.substring(0, 2)
        if (two.matches(Regex("""^3[1-6]$""")) && str.length >= 4) return Pair(str.substring(0, 4), 6)
        if (two == "41" && str.length >= 3) return Pair(str.substring(0, 3), 13)
        val fixed = mapOf(
            "00" to 18, "01" to 14, "02" to 14, "03" to 14,
            "11" to 6, "12" to 6, "13" to 6, "15" to 6, "16" to 6, "17" to 6, "20" to 2
        )
        if (fixed.containsKey(two)) return Pair(two, fixed[two])
        if (two.matches(Regex("""^(10|21|22|30|37|9\d)$"""))) return Pair(two, null)
        if (str.length >= 3 && str.substring(0, 3).matches(Regex("""^(23|24|25|40|42|71)$"""))) return Pair(str.substring(0, 3), null)
        if (str.length >= 4 && str.substring(0, 4).matches(Regex("""^(39|43|70|72|80|81|82)$"""))) return Pair(str.substring(0, 4), null)
        return Pair(null, null)
    }

    private fun parseElementString(s: String, formatName: String): ParsedScanResult? {
        val t = s.replace(Regex("""^\](C1|e0|d2|Q3|J1)"""), "")
        val parenMatch = Regex("""\((\d{2,4})\)([^(]*)""").findAll(t)
        if (parenMatch.any()) {
            val ai = mutableMapOf<String, String>()
            for (m in parenMatch) {
                ai[m.groupValues[1]] = m.groupValues[2].trim()
            }
            return gs1Result(ai, formatName)
        }
        if (!t.matches(Regex("""^01\d{14}.*"""))) return null
        if (t == s && !t.contains(GS) && t.length < 16) return null

        var i = 0
        var guard = 0
        val ai = mutableMapOf<String, String>()
        while (i < t.length && guard++ < 40) {
            if (t[i] == GS) {
                i++
                continue
            }
            val (code, len) = aiInfo(t.substring(i))
            if (code == null) break
            i += code.length
            if (len != null && i + len <= t.length) {
                ai[code] = t.substring(i, i + len)
                i += len
            } else {
                var j = t.indexOf(GS, i)
                if (j < 0) j = t.length
                ai[code] = t.substring(i, j)
                i = j
            }
        }
        return gs1Result(ai, formatName)
    }

    fun readScan(
        raw: String?,
        weighedEnabled: Boolean = true,
        formatName: String = "EAN"
    ): ParsedScanResult? {
        val s = (raw ?: "").replace(Regex("""[\r\n]+"""), " ").trim()
        if (s.isEmpty()) return null

        val gs1 = parseDigitalLink(s, formatName) ?: parseElementString(s, formatName)
        if (gs1 != null) return gs1

        val compact = s.replace(Regex("""\s+"""), "")
        if (compact.matches(Regex("""^\d{6,14}$"""))) {
            var k = compact
            if (k.length == 12) k = "0$k" // UPC-A -> EAN-13
            if (k.length == 14 && k[0] == '0') k = k.substring(1)

            // Check if it is a weighed supermarket barcode (prefix 2)
            val weighed = parseWeighed(k, weighedEnabled)
            if (weighed != null) return weighed

            val format = if (k.length == 8) "EAN-8" else if (k.length == 13) "EAN-13" else formatName
            return ParsedScanResult(key = k, show = k, kind = "ean", formatName = format, rawValue = s)
        }

        if (compact.matches(Regex("""^[A-Za-z0-9\-./+%$]{3,40}$""")) && !s.startsWith("http", ignoreCase = true)) {
            val up = compact.uppercase()
            return ParsedScanResult(key = up, show = up, kind = "ean", formatName = formatName, rawValue = s)
        }

        val qrText = if (s.length > 160) s.substring(0, 160) else s
        return ParsedScanResult(key = "QR:$qrText", show = qrText, kind = "qr", formatName = "QR Code", rawValue = s)
    }

    fun shortCode(s: String?): String {
        val str = s ?: ""
        return if (str.length > 30) str.take(28) + "…" else str
    }

    fun codeLabel(key: String, show: String? = null): String {
        return if (key.startsWith("QR:")) {
            "QR · " + shortCode(show ?: key.substring(3))
        } else {
            "Codice " + shortCode(show ?: key)
        }
    }
}
