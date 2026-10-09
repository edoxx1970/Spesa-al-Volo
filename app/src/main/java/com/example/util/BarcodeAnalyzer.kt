package com.example.util

import android.annotation.SuppressLint
import android.graphics.Rect
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage

/**
 * CameraX ImageAnalysis analyzer using Google ML Kit Barcode Scanning.
 * Scans retail and 2D barcodes on grocery products, extracts structured data (GTIN, PLU, price, weight, expiry),
 * and reports the bounding box for on-screen targeting reticle highlights.
 */
class BarcodeAnalyzer(
    private val weighedEnabled: Boolean = true,
    private val onGroceryBarcodeDetected: (ParsedScanResult, Rect?) -> Unit
) : ImageAnalysis.Analyzer {

    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.FORMAT_EAN_13,
            Barcode.FORMAT_EAN_8,
            Barcode.FORMAT_UPC_A,
            Barcode.FORMAT_UPC_E,
            Barcode.FORMAT_CODE_128,
            Barcode.FORMAT_CODE_39,
            Barcode.FORMAT_ITF,
            Barcode.FORMAT_QR_CODE,
            Barcode.FORMAT_DATA_MATRIX
        )
        .build()

    private val scanner = BarcodeScanning.getClient(options)

    private var lastScannedCode: String? = null
    private var lastScannedTimestamp: Long = 0L

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    val now = System.currentTimeMillis()
                    for (barcode in barcodes) {
                        val raw = barcode.rawValue
                        if (!raw.isNullOrBlank()) {
                            // Debounce logic: minimum 800ms between scans, and 1800ms if exact same code
                            val isSame = raw == lastScannedCode
                            val timeDiff = now - lastScannedTimestamp
                            if (timeDiff > 800L && (!isSame || timeDiff > 1800L)) {
                                val formatName = BarcodeParser.getFormatName(barcode.format)
                                val parsed = BarcodeParser.readScan(raw, weighedEnabled, formatName)
                                if (parsed != null) {
                                    lastScannedCode = raw
                                    lastScannedTimestamp = now
                                    onGroceryBarcodeDetected(parsed, barcode.boundingBox)
                                    break
                                }
                            }
                        }
                    }
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
}
