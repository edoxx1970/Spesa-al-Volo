package com.example

import com.example.util.BarcodeParser
import com.example.util.FormatUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BarcodeParserTest {

    @Test
    fun testEanCheckAndValid() {
        // EAN-13: 800111100001 -> check digit is 1
        val body = "800111100001"
        val check = BarcodeParser.eanCheck(body)
        assertEquals("1", check)
        assertTrue(BarcodeParser.eanValid("8001111000011"))
        assertFalse(BarcodeParser.eanValid("8001111000019"))

        // EAN-8: 8001111 -> check digit is 7
        val ean8Body = "8001111"
        val check8 = BarcodeParser.eanCheck(ean8Body)
        assertTrue(BarcodeParser.eanValid(ean8Body + check8))
    }

    @Test
    fun testWeighedBarcodeParsing() {
        // 210041200348 -> check digit 9, plu 100412, price 00348 = € 3,48
        val check = BarcodeParser.eanCheck("210041200348")
        val code = "210041200348$check"
        val parsed = BarcodeParser.parseWeighed(code, enabled = true)

        assertNotNull(parsed)
        assertEquals("banco", parsed?.kind)
        assertEquals("100412", parsed?.plu)
        assertEquals(348, parsed?.priceCents)

        // Disabled
        assertNull(BarcodeParser.parseWeighed(code, enabled = false))
    }

    @Test
    fun testGs1DigitalLinkParsing() {
        val url = "https://id.example.com/01/08001234567890/10/ABC?3922=0348&3103=000215"
        val parsed = BarcodeParser.readScan(url)

        assertNotNull(parsed)
        assertEquals("gs1", parsed?.kind)
        assertEquals(348, parsed?.priceCents)
        assertEquals("0,215 kg", parsed?.weightKg)
        assertEquals("8001234567890", parsed?.base)
    }

    @Test
    fun testFormatEuro() {
        assertEquals("1,29", FormatUtils.formatEuro(129))
        assertEquals("€ 1,29", FormatUtils.formatEuroWithSymbol(129))
        assertEquals("0,00", FormatUtils.formatEuro(0))
        assertEquals(129, FormatUtils.parsePriceToCents("1,29"))
        assertEquals(250, FormatUtils.parsePriceToCents("€ 2.50"))
    }

    @Test
    fun testShoppingListNameMatching() {
        assertTrue(FormatUtils.nameMatch("latte", "Latte intero 1 L"))
        assertTrue(FormatUtils.nameMatch("uova", "Uova biologiche medie conf da 6"))
        assertTrue(FormatUtils.nameMatch("caffe", "Caffè macinato 250 g"))
        assertFalse(FormatUtils.nameMatch("pane", "Pomodori pelati"))
    }
}
