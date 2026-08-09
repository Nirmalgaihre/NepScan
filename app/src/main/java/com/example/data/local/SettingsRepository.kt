package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class UserPreferences(
    val autoCapture: Boolean = false,
    val defaultFilter: String = "AUTO",
    val scanQuality: String = "HIGH",
    val flashMode: String = "OFF",
    val pdfQuality: String = "HIGH",
    val themeMode: String = "SYSTEM"
)

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("nepscan_settings", Context.MODE_PRIVATE)

    private val _preferences = MutableStateFlow(loadPreferences())
    val preferences: StateFlow<UserPreferences> = _preferences

    private fun loadPreferences(): UserPreferences {
        return UserPreferences(
            autoCapture = prefs.getBoolean("auto_capture", false),
            defaultFilter = prefs.getString("default_filter", "AUTO") ?: "AUTO",
            scanQuality = prefs.getString("scan_quality", "HIGH") ?: "HIGH",
            flashMode = prefs.getString("flash_mode", "OFF") ?: "OFF",
            pdfQuality = prefs.getString("pdf_quality", "HIGH") ?: "HIGH",
            themeMode = prefs.getString("theme_mode", "SYSTEM") ?: "SYSTEM"
        )
    }

    fun updateAutoCapture(value: Boolean) {
        prefs.edit().putBoolean("auto_capture", value).apply()
        _preferences.value = _preferences.value.copy(autoCapture = value)
    }

    fun updateDefaultFilter(filter: String) {
        prefs.edit().putString("default_filter", filter).apply()
        _preferences.value = _preferences.value.copy(defaultFilter = filter)
    }

    fun updateScanQuality(quality: String) {
        prefs.edit().putString("scan_quality", quality).apply()
        _preferences.value = _preferences.value.copy(scanQuality = quality)
    }

    fun updateFlashMode(mode: String) {
        prefs.edit().putString("flash_mode", mode).apply()
        _preferences.value = _preferences.value.copy(flashMode = mode)
    }

    fun updatePdfQuality(quality: String) {
        prefs.edit().putString("pdf_quality", quality).apply()
        _preferences.value = _preferences.value.copy(pdfQuality = quality)
    }

    fun updateThemeMode(theme: String) {
        prefs.edit().putString("theme_mode", theme).apply()
        _preferences.value = _preferences.value.copy(themeMode = theme)
    }
}
