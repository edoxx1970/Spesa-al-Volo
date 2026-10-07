package com.example.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object OpenFoodFactsService {

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(4, TimeUnit.SECONDS)
            .build()
    }

    suspend fun lookupProductName(barcode: String): String? = withContext(Dispatchers.IO) {
        if (!barcode.matches(Regex("""^(\d{8}|\d{13})$"""))) return@withContext null

        try {
            val url = "https://world.openfoodfacts.org/api/v2/product/$barcode.json?fields=product_name_it,product_name,brands,quantity"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "SpesaAlVolo-Android/1.0")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val json = JSONObject(body)
                if (json.optInt("status", 0) != 1) return@withContext null

                val product = json.optJSONObject("product") ?: return@withContext null
                val nameIt = product.optString("product_name_it").trim()
                val name = if (nameIt.isNotEmpty()) nameIt else product.optString("product_name").trim()
                val brand = product.optString("brands").split(",").firstOrNull()?.trim() ?: ""
                val quantity = product.optString("quantity").trim()

                val parts = mutableListOf<String>()
                if (brand.isNotEmpty() && !name.contains(brand, ignoreCase = true)) {
                    parts.add(brand)
                }
                if (name.isNotEmpty()) {
                    parts.add(name)
                }
                if (quantity.isNotEmpty()) {
                    parts.add(quantity)
                }

                val finalStr = parts.joinToString(" ").trim()
                if (finalStr.isNotEmpty()) finalStr.take(60) else null
            }
        } catch (e: Exception) {
            null
        }
    }
}
