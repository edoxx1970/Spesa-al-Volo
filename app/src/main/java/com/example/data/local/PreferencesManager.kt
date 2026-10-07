package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserSettings(
    val currentStoreId: String? = null,
    val budgetCents: Int = 0,
    val weighedBarcodeEnabled: Boolean = true,
    val soundEnabled: Boolean = true,
    val hasSeenDemo: Boolean = false
)

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("spesa_al_volo_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<UserSettings> = _settings.asStateFlow()

    private fun loadSettings(): UserSettings {
        return UserSettings(
            currentStoreId = prefs.getString("current_store_id", null),
            budgetCents = prefs.getInt("budget_cents", 0),
            weighedBarcodeEnabled = prefs.getBoolean("weighed_barcode_enabled", true),
            soundEnabled = prefs.getBoolean("sound_enabled", true),
            hasSeenDemo = prefs.getBoolean("has_seen_demo", false)
        )
    }

    fun setCurrentStoreId(storeId: String?) {
        prefs.edit().putString("current_store_id", storeId).apply()
        _settings.value = _settings.value.copy(currentStoreId = storeId)
    }

    fun setBudgetCents(cents: Int) {
        prefs.edit().putInt("budget_cents", cents).apply()
        _settings.value = _settings.value.copy(budgetCents = cents)
    }

    fun setWeighedBarcodeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("weighed_barcode_enabled", enabled).apply()
        _settings.value = _settings.value.copy(weighedBarcodeEnabled = enabled)
    }

    fun setSoundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("sound_enabled", enabled).apply()
        _settings.value = _settings.value.copy(soundEnabled = enabled)
    }

    fun setHasSeenDemo(seen: Boolean) {
        prefs.edit().putBoolean("has_seen_demo", seen).apply()
        _settings.value = _settings.value.copy(hasSeenDemo = seen)
    }
}
