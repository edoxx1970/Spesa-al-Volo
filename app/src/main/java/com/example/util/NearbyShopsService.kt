package com.example.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class NearbyShop(
    val name: String,
    val address: String,
    val kind: String,
    val latitude: Double,
    val longitude: Double,
    val distanceMeters: Float
)

object NearbyShopsService {

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    private val SHOP_LABELS = mapOf(
        "supermarket" to "Supermercato",
        "convenience" to "Alimentari",
        "discount" to "Discount",
        "greengrocer" to "Frutta e verdura",
        "butcher" to "Macelleria",
        "bakery" to "Panificio",
        "deli" to "Gastronomia",
        "grocery" to "Alimentari"
    )

    fun distanceInMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val r = 6371000.0 // Earth radius in meters
        val toRad = Math.PI / 180.0
        val dLat = (lat2 - lat1) * toRad
        val dLon = (lon2 - lon1) * toRad
        val a = sin(dLat / 2).let { it * it } +
                cos(lat1 * toRad) * cos(lat2 * toRad) * sin(dLon / 2).let { it * it }
        val c = 2 * asin(sqrt(a))
        return (r * c).toFloat()
    }

    suspend fun findNearbyShops(lat: Double, lon: Double, accuracyMeters: Float = 50f): List<NearbyShop> = withContext(Dispatchers.IO) {
        val radius = maxOf(400, minOf(1200, (accuracyMeters * 2).toInt()))
        val tags = SHOP_LABELS.keys.joinToString("|")
        val query = String.format(
            Locale.US,
            "[out:json][timeout:10];nwr(around:%d,%.6f,%.6f)[\"shop\"~\"^(%s)$\"];out center tags 20;",
            radius, lat, lon, tags
        )

        val endpoints = listOf(
            "https://overpass-api.de/api/interpreter",
            "https://overpass.kumi.systems/api/interpreter"
        )

        for (endpoint in endpoints) {
            try {
                val formBody = FormBody.Builder()
                    .add("data", query)
                    .build()

                val request = Request.Builder()
                    .url(endpoint)
                    .post(formBody)
                    .header("User-Agent", "SpesaAlVolo-Android/1.0")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use
                    val body = response.body?.string() ?: return@use
                    val json = JSONObject(body)
                    val elements = json.optJSONArray("elements") ?: return@use

                    val seen = mutableSetOf<String>()
                    val list = mutableListOf<NearbyShop>()

                    for (i in 0 until elements.length()) {
                        val elem = elements.getJSONObject(i)
                        val shopLat = if (elem.has("lat")) elem.optDouble("lat") else elem.optJSONObject("center")?.optDouble("lat") ?: continue
                        val shopLon = if (elem.has("lon")) elem.optDouble("lon") else elem.optJSONObject("center")?.optDouble("lon") ?: continue

                        val t = elem.optJSONObject("tags") ?: JSONObject()
                        val rawKind = t.optString("shop")
                        val kindLabel = SHOP_LABELS[rawKind] ?: "Negozio"
                        val nameRaw = t.optString("name", t.optString("brand", kindLabel)).trim()
                        val street = t.optString("addr:street")
                        val houseNum = t.optString("addr:housenumber")
                        val city = t.optString("addr:city")

                        val addressParts = listOf(street, houseNum).filter { it.isNotEmpty() }.joinToString(" ")
                        val fullAddress = if (addressParts.isNotEmpty()) addressParts else city

                        val displayName = if (fullAddress.isNotEmpty() && !nameRaw.contains(fullAddress, ignoreCase = true)) {
                            "$nameRaw – $fullAddress"
                        } else {
                            nameRaw
                        }

                        val key = "${displayName.lowercase()}|$shopLat|$shopLon"
                        if (seen.add(key)) {
                            val dist = distanceInMeters(lat, lon, shopLat, shopLon)
                            list.add(
                                NearbyShop(
                                    name = displayName,
                                    address = fullAddress,
                                    kind = kindLabel,
                                    latitude = shopLat,
                                    longitude = shopLon,
                                    distanceMeters = dist
                                )
                            )
                        }
                    }

                    if (list.isNotEmpty()) {
                        return@withContext list.sortedBy { it.distanceMeters }.take(10)
                    }
                }
            } catch (e: Exception) {
                // Try next endpoint
            }
        }
        emptyList()
    }
}
