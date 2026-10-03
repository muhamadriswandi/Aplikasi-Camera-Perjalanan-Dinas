package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.example.data.local.PhotoEntity
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object FileUtils {

    fun createPhotoFile(
        context: Context,
        projectName: String,
        timestamp: Long = System.currentTimeMillis()
    ): File {
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        val yearFormat = SimpleDateFormat("yyyy", Locale.US)
        val monthFormat = SimpleDateFormat("MM", Locale.US)
        val dayFormat = SimpleDateFormat("dd", Locale.US)

        val date = Date(timestamp)
        val yearStr = yearFormat.format(date)
        val monthStr = monthFormat.format(date)
        val dayStr = dayFormat.format(date)
        val sanitizedProject = sanitizeFolderName(projectName.ifBlank { "Umum" })

        // Target: Pictures/GeoCamera/YYYY/MM/DD/Project/
        val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES) ?: context.filesDir
        val targetDir = File(baseDir, "GeoCamera/$yearStr/$monthStr/$dayStr/$sanitizedProject")
        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        val fileName = "IMG_${dateFormat.format(date)}.jpg"
        return File(targetDir, fileName)
    }

    private fun sanitizeFolderName(name: String): String {
        return name.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim()
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format(Locale.US, "%.2f GB", gb)
            mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
            kb >= 1.0 -> String.format(Locale.US, "%.0f KB", kb)
            else -> "$bytes B"
        }
    }

    fun sharePhoto(context: Context, photo: PhotoEntity) {
        val file = File(photo.filePath)
        if (!file.exists()) return

        val uri = FileProvider.getUriForFile(
            context,
            "com.aistudio.geocamera.qvyztp.fileprovider",
            file
        )

        val textExtra = buildString {
            append("🌐 GeoCamera Survey: ${photo.projectName}\n")
            append("📍 Lat: ${photo.latitude}, Lng: ${photo.longitude}\n")
            if (photo.regency.isNotBlank() || photo.district.isNotBlank()) {
                append("🏛️ Lokasi: ${listOf(photo.village, photo.district, photo.regency).filter { it.isNotBlank() }.joinToString(", ")}\n")
            }
            if (photo.note.isNotBlank()) {
                append("📝 Keterangan: ${photo.note}\n")
            }
            append("🕒 Waktu: ${photo.dateFormatted} ${photo.timeFormatted}")
        }

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "GeoCamera Survey - ${photo.projectName}")
            putExtra(Intent.EXTRA_TEXT, textExtra)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Bagikan Foto Survei"))
    }

    fun shareMultiplePhotos(context: Context, photos: List<PhotoEntity>) {
        if (photos.isEmpty()) return
        val uris = ArrayList<Uri>()
        photos.forEach { photo ->
            val file = File(photo.filePath)
            if (file.exists()) {
                val uri = FileProvider.getUriForFile(
                    context,
                    "com.aistudio.geocamera.qvyztp.fileprovider",
                    file
                )
                uris.add(uri)
            }
        }

        if (uris.isEmpty()) return

        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "image/jpeg"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            putExtra(Intent.EXTRA_SUBJECT, "GeoCamera Survey - ${photos.size} Foto")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Bagikan ${photos.size} Foto Survei"))
    }

    fun openInMaps(context: Context, latitude: Double, longitude: Double, label: String) {
        val uri = Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude(${Uri.encode(label)})")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            // Browser fallback
            val webUri = Uri.parse("https://maps.google.com/?q=$latitude,$longitude")
            context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
        }
    }

    fun exportToZip(context: Context, photos: List<PhotoEntity>, zipNamePrefix: String): File? {
        if (photos.isEmpty()) return null
        return try {
            val cacheDir = context.cacheDir
            val zipFile = File(cacheDir, "${sanitizeFolderName(zipNamePrefix)}_${System.currentTimeMillis()}.zip")
            ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
                val buffer = ByteArray(8192)
                photos.forEach { photo ->
                    val file = File(photo.filePath)
                    if (file.exists()) {
                        val entryName = "${photo.projectName}/${file.name}"
                        zos.putNextEntry(ZipEntry(entryName))
                        BufferedInputStream(FileInputStream(file)).use { bis ->
                            var count: Int
                            while (bis.read(buffer).also { count = it } != -1) {
                                zos.write(buffer, 0, count)
                            }
                        }
                        zos.closeEntry()
                    }
                }
            }
            zipFile
        } catch (_: Exception) {
            null
        }
    }

    fun shareZipFile(context: Context, zipFile: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "com.aistudio.geocamera.qvyztp.fileprovider",
            zipFile
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Export GeoCamera: ${zipFile.name}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Bagikan Berkas ZIP Survei"))
    }
}
