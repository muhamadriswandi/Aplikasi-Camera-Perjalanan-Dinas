package com.example.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrandingWatermark
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.repository.AppSettings
import com.example.data.repository.SettingsRepository
import com.example.data.repository.WatermarkPosition
import com.example.ui.theme.SurveyPrimaryAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsRepository: SettingsRepository,
    modifier: Modifier = Modifier
) {
    val settings by settingsRepository.settings.collectAsState()

    var showFontSizeMenu by remember { mutableStateOf(false) }
    var showQualityMenu by remember { mutableStateOf(false) }
    var showAccuracyMenu by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = SurveyPrimaryAmber,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Pengaturan",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Watermark Settings
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.BrandingWatermark, contentDescription = null, tint = SurveyPrimaryAmber)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Konfigurasi Watermark", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Enable/Disable Watermark
                    SettingToggleRow(
                        title = "Cetak Watermark Otomatis",
                        subtitle = "Menyertakan koordinat GPS, alamat, waktu, dan nama proyek pada foto",
                        isChecked = settings.watermarkEnabled,
                        onCheckedChange = {
                            settingsRepository.updateSettings(settings.copy(watermarkEnabled = it))
                        }
                    )

                    if (settings.watermarkEnabled) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                        // Section: Watermark Position for Portrait and Landscape
                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Text(
                                text = "Penempatan Watermark Berdasarkan Orientasi",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "Tentukan posisi watermark secara terpisah untuk foto Potret (tegak) dan Landscape (mendatar).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // 1. MODE POTRET
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("📱", fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Mode Potret (Tegak)", fontWeight = FontWeight.SemiBold)
                                }
                                Text(
                                    text = settings.watermarkPositionPortrait.label,
                                    color = SurveyPrimaryAmber,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // 3x2 Visual Selector Box for Portrait
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(androidx.compose.ui.graphics.Color(0xFF0F172A), RoundedCornerShape(12.dp))
                                    .border(1.dp, androidx.compose.ui.graphics.Color(0xFF334155), RoundedCornerShape(12.dp))
                                    .padding(8.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    // Row 1: Top positions
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf(WatermarkPosition.TOP_LEFT, WatermarkPosition.TOP_CENTER, WatermarkPosition.TOP_RIGHT).forEach { pos ->
                                            val isSelected = settings.watermarkPositionPortrait == pos
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .background(
                                                        if (isSelected) SurveyPrimaryAmber else androidx.compose.ui.graphics.Color(0xFF1E293B),
                                                        RoundedCornerShape(8.dp)
                                                    )
                                                    .border(
                                                        1.dp,
                                                        if (isSelected) SurveyPrimaryAmber else androidx.compose.ui.graphics.Color(0xFF475569),
                                                        RoundedCornerShape(8.dp)
                                                    )
                                                    .clickable {
                                                        settingsRepository.updateSettings(
                                                            settings.copy(
                                                                watermarkPositionPortrait = pos,
                                                                watermarkPosition = pos
                                                            )
                                                        )
                                                    }
                                                    .padding(vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = pos.label,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) androidx.compose.ui.graphics.Color.Black else androidx.compose.ui.graphics.Color.White
                                                )
                                            }
                                        }
                                    }

                                    // Row 2: Bottom positions
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf(WatermarkPosition.BOTTOM_LEFT, WatermarkPosition.BOTTOM_CENTER, WatermarkPosition.BOTTOM_RIGHT).forEach { pos ->
                                            val isSelected = settings.watermarkPositionPortrait == pos
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .background(
                                                        if (isSelected) SurveyPrimaryAmber else androidx.compose.ui.graphics.Color(0xFF1E293B),
                                                        RoundedCornerShape(8.dp)
                                                    )
                                                    .border(
                                                        1.dp,
                                                        if (isSelected) SurveyPrimaryAmber else androidx.compose.ui.graphics.Color(0xFF475569),
                                                        RoundedCornerShape(8.dp)
                                                    )
                                                    .clickable {
                                                        settingsRepository.updateSettings(
                                                            settings.copy(
                                                                watermarkPositionPortrait = pos,
                                                                watermarkPosition = pos
                                                            )
                                                        )
                                                    }
                                                    .padding(vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = pos.label,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) androidx.compose.ui.graphics.Color.Black else androidx.compose.ui.graphics.Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // 2. MODE LANDSCAPE
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("🔄", fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Mode Landscape (Mendatar)", fontWeight = FontWeight.SemiBold)
                                }
                                Text(
                                    text = settings.watermarkPositionLandscape.label,
                                    color = SurveyPrimaryAmber,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // 3x2 Visual Selector Box for Landscape
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(androidx.compose.ui.graphics.Color(0xFF0F172A), RoundedCornerShape(12.dp))
                                    .border(1.dp, androidx.compose.ui.graphics.Color(0xFF334155), RoundedCornerShape(12.dp))
                                    .padding(8.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    // Row 1: Top positions
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf(WatermarkPosition.TOP_LEFT, WatermarkPosition.TOP_CENTER, WatermarkPosition.TOP_RIGHT).forEach { pos ->
                                            val isSelected = settings.watermarkPositionLandscape == pos
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .background(
                                                        if (isSelected) SurveyPrimaryAmber else androidx.compose.ui.graphics.Color(0xFF1E293B),
                                                        RoundedCornerShape(8.dp)
                                                    )
                                                    .border(
                                                        1.dp,
                                                        if (isSelected) SurveyPrimaryAmber else androidx.compose.ui.graphics.Color(0xFF475569),
                                                        RoundedCornerShape(8.dp)
                                                    )
                                                    .clickable {
                                                        settingsRepository.updateSettings(
                                                            settings.copy(watermarkPositionLandscape = pos)
                                                        )
                                                    }
                                                    .padding(vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = pos.label,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) androidx.compose.ui.graphics.Color.Black else androidx.compose.ui.graphics.Color.White
                                                )
                                            }
                                        }
                                    }

                                    // Row 2: Bottom positions
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf(WatermarkPosition.BOTTOM_LEFT, WatermarkPosition.BOTTOM_CENTER, WatermarkPosition.BOTTOM_RIGHT).forEach { pos ->
                                            val isSelected = settings.watermarkPositionLandscape == pos
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .background(
                                                        if (isSelected) SurveyPrimaryAmber else androidx.compose.ui.graphics.Color(0xFF1E293B),
                                                        RoundedCornerShape(8.dp)
                                                    )
                                                    .border(
                                                        1.dp,
                                                        if (isSelected) SurveyPrimaryAmber else androidx.compose.ui.graphics.Color(0xFF475569),
                                                        RoundedCornerShape(8.dp)
                                                    )
                                                    .clickable {
                                                        settingsRepository.updateSettings(
                                                            settings.copy(watermarkPositionLandscape = pos)
                                                        )
                                                    }
                                                    .padding(vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = pos.label,
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) androidx.compose.ui.graphics.Color.Black else androidx.compose.ui.graphics.Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                        // Auto rotate watermark toggle
                        SettingToggleRow(
                            title = "Penyesuaian Orientasi Otomatis",
                            subtitle = "Informasi lokasi otomatis tegak menyesuaikan posisi kamera saat memotret",
                            isChecked = settings.autoRotateWatermark,
                            onCheckedChange = {
                                settingsRepository.updateSettings(settings.copy(autoRotateWatermark = it))
                            }
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                        // Font Size Option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showFontSizeMenu = true }
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Ukuran Teks Watermark", fontWeight = FontWeight.Medium)
                                Text("Skala keterbacaan huruf", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                text = settings.watermarkFontSize,
                                color = SurveyPrimaryAmber,
                                fontWeight = FontWeight.Bold
                            )

                            DropdownMenu(
                                expanded = showFontSizeMenu,
                                onDismissRequest = { showFontSizeMenu = false }
                            ) {
                                listOf("Kecil", "Normal", "Besar").forEach { size ->
                                    DropdownMenuItem(
                                        text = { Text(size) },
                                        onClick = {
                                            settingsRepository.updateSettings(settings.copy(watermarkFontSize = size))
                                            showFontSizeMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Section 2: GPS Settings
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.GpsFixed, contentDescription = null, tint = SurveyPrimaryAmber)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Konfigurasi GPS & Akurasi", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    SettingToggleRow(
                        title = "Wajib GPS Terkunci",
                        subtitle = "Mencegah pengambilan foto jika sinyal GPS belum valid",
                        isChecked = settings.requireGpsLock,
                        onCheckedChange = {
                            settingsRepository.updateSettings(settings.copy(requireGpsLock = it))
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    // Minimum GPS Accuracy
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAccuracyMenu = true }
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Toleransi Akurasi GPS", fontWeight = FontWeight.Medium)
                            Text("Batas akurasi maksimal yang diperbolehkan", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            text = "≤ ${settings.minGpsAccuracyMeters.toInt()} meter",
                            color = SurveyPrimaryAmber,
                            fontWeight = FontWeight.Bold
                        )

                        DropdownMenu(
                            expanded = showAccuracyMenu,
                            onDismissRequest = { showAccuracyMenu = false }
                        ) {
                            listOf(5f, 10f, 20f, 50f).forEach { acc ->
                                DropdownMenuItem(
                                    text = { Text("≤ ${acc.toInt()} meter") },
                                    onClick = {
                                        settingsRepository.updateSettings(settings.copy(minGpsAccuracyMeters = acc))
                                        showAccuracyMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Section 3: Camera & Viewfinder Settings
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = SurveyPrimaryAmber)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Kamera & Sensor", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // JPEG Quality
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showQualityMenu = true }
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Kualitas Kompresi JPEG", fontWeight = FontWeight.Medium)
                            Text("Keseimbangan resolusi dan ukuran berkas", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            text = "${settings.jpegQuality}%",
                            color = SurveyPrimaryAmber,
                            fontWeight = FontWeight.Bold
                        )

                        DropdownMenu(
                            expanded = showQualityMenu,
                            onDismissRequest = { showQualityMenu = false }
                        ) {
                            listOf(80, 90, 95, 100).forEach { q ->
                                DropdownMenuItem(
                                    text = { Text("$q%") },
                                    onClick = {
                                        settingsRepository.updateSettings(settings.copy(jpegQuality = q))
                                        showQualityMenu = false
                                    }
                                )
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    SettingToggleRow(
                        title = "Garis Grid (Rule of Thirds)",
                        subtitle = "Membantu komposisi pemotretan objek survei",
                        isChecked = settings.showGrid,
                        onCheckedChange = {
                            settingsRepository.updateSettings(settings.copy(showGrid = it))
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    SettingToggleRow(
                        title = "Indikator Kemiringan (Horizon Level)",
                        subtitle = "Menampilkan waterpass digital pitch & roll di kamera",
                        isChecked = settings.showLevelIndicator,
                        onCheckedChange = {
                            settingsRepository.updateSettings(settings.copy(showLevelIndicator = it))
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    SettingToggleRow(
                        title = "Getar Saat Memotret",
                        subtitle = "Umpan balik haptik konfirmasi pengambilan foto",
                        isChecked = settings.soundVibrateFeedback,
                        onCheckedChange = {
                            settingsRepository.updateSettings(settings.copy(soundVibrateFeedback = it))
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!isChecked) }
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = SurveyPrimaryAmber)
        )
    }
}
