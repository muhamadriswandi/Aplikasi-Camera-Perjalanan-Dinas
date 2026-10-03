package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class WatermarkPosition(val label: String) {
    TOP_LEFT("Atas Kiri"),
    TOP_CENTER("Atas Tengah"),
    TOP_RIGHT("Atas Kanan"),
    BOTTOM_LEFT("Bawah Kiri"),
    BOTTOM_CENTER("Bawah Tengah"),
    BOTTOM_RIGHT("Bawah Kanan")
}

data class AppSettings(
    val watermarkEnabled: Boolean = true,
    val watermarkPosition: WatermarkPosition = WatermarkPosition.BOTTOM_LEFT, // fallback
    val watermarkPositionPortrait: WatermarkPosition = WatermarkPosition.BOTTOM_LEFT,
    val watermarkPositionLandscape: WatermarkPosition = WatermarkPosition.BOTTOM_LEFT,
    val watermarkFontSize: String = "Normal", // Kecil, Normal, Besar
    val requireGpsLock: Boolean = false,     // Bila true, harus lock GPS dulu
    val minGpsAccuracyMeters: Float = 20f,   // Toleransi akurasi GPS
    val jpegQuality: Int = 95,               // 80, 90, 95, 100
    val showGrid: Boolean = true,
    val showLevelIndicator: Boolean = true,
    val dateFormatPattern: String = "dd MMMM yyyy HH:mm:ss",
    val showLiveWatermarkOnCamera: Boolean = false, // HUD live watermark removed from camera preview
    val autoRotateWatermark: Boolean = true,
    val darkTheme: Boolean = true,
    val soundVibrateFeedback: Boolean = true
)

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("geocamera_survey_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun loadSettings(): AppSettings {
        val legacyPosStr = prefs.getString("watermark_pos", WatermarkPosition.BOTTOM_LEFT.name)
        val legacyPos = try {
            WatermarkPosition.valueOf(legacyPosStr ?: WatermarkPosition.BOTTOM_LEFT.name)
        } catch (_: Exception) {
            WatermarkPosition.BOTTOM_LEFT
        }

        val posPortraitStr = prefs.getString("watermark_pos_portrait", legacyPos.name)
        val posPortrait = try {
            WatermarkPosition.valueOf(posPortraitStr ?: legacyPos.name)
        } catch (_: Exception) {
            legacyPos
        }

        val posLandscapeStr = prefs.getString("watermark_pos_landscape", legacyPos.name)
        val posLandscape = try {
            WatermarkPosition.valueOf(posLandscapeStr ?: legacyPos.name)
        } catch (_: Exception) {
            legacyPos
        }

        return AppSettings(
            watermarkEnabled = prefs.getBoolean("watermark_enabled", true),
            watermarkPosition = posPortrait,
            watermarkPositionPortrait = posPortrait,
            watermarkPositionLandscape = posLandscape,
            watermarkFontSize = prefs.getString("watermark_font_size", "Normal") ?: "Normal",
            requireGpsLock = prefs.getBoolean("require_gps_lock", false),
            minGpsAccuracyMeters = prefs.getFloat("min_gps_accuracy", 20f),
            jpegQuality = prefs.getInt("jpeg_quality", 95),
            showGrid = prefs.getBoolean("show_grid", true),
            showLevelIndicator = prefs.getBoolean("show_level_indicator", true),
            dateFormatPattern = prefs.getString("date_format_pattern", "dd MMMM yyyy HH:mm:ss") ?: "dd MMMM yyyy HH:mm:ss",
            showLiveWatermarkOnCamera = false,
            autoRotateWatermark = prefs.getBoolean("auto_rotate_watermark", true),
            darkTheme = prefs.getBoolean("dark_theme", true),
            soundVibrateFeedback = prefs.getBoolean("sound_vibrate_feedback", true)
        )
    }

    fun updateSettings(newSettings: AppSettings) {
        prefs.edit()
            .putBoolean("watermark_enabled", newSettings.watermarkEnabled)
            .putString("watermark_pos", newSettings.watermarkPosition.name)
            .putString("watermark_pos_portrait", newSettings.watermarkPositionPortrait.name)
            .putString("watermark_pos_landscape", newSettings.watermarkPositionLandscape.name)
            .putString("watermark_font_size", newSettings.watermarkFontSize)
            .putBoolean("require_gps_lock", newSettings.requireGpsLock)
            .putFloat("min_gps_accuracy", newSettings.minGpsAccuracyMeters)
            .putInt("jpeg_quality", newSettings.jpegQuality)
            .putBoolean("show_grid", newSettings.showGrid)
            .putBoolean("show_level_indicator", newSettings.showLevelIndicator)
            .putString("date_format_pattern", newSettings.dateFormatPattern)
            .putBoolean("show_live_watermark", false)
            .putBoolean("auto_rotate_watermark", newSettings.autoRotateWatermark)
            .putBoolean("dark_theme", newSettings.darkTheme)
            .putBoolean("sound_vibrate_feedback", newSettings.soundVibrateFeedback)
            .apply()

        _settings.value = newSettings
    }
}
