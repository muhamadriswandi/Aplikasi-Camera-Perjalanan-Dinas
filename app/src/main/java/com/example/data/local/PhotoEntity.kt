package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "photos")
data class PhotoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fileName: String,
    val filePath: String,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 0.0,
    val accuracy: Float = 0f,
    val bearing: Float = 0f,
    val speed: Float = 0f,
    val regency: String = "",       // Kabupaten
    val district: String = "",      // Kecamatan
    val village: String = "",       // Desa / Kelurahan
    val address: String = "",       // Formatted address or landmark
    val projectName: String = "Umum", // Nama Proyek / Kegiatan
    val note: String = "",          // Catatan lapangan / No Aset / Keterangan
    val dateFormatted: String,      // e.g. "03 Oktober 2026"
    val timeFormatted: String,      // e.g. "08:15:35"
    val year: Int,
    val month: Int,                 // 1-12
    val day: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val resolution: String = "",    // e.g. "1920x1080"
    val fileSizeBytes: Long = 0,
    val deviceModel: String = "",
    val manufacturer: String = "",
    val isWatermarked: Boolean = true
)
