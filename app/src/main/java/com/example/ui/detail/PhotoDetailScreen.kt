package com.example.ui.detail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.PhotoEntity
import com.example.data.repository.SurveyRepository
import com.example.ui.theme.SurveyNavyDark
import com.example.ui.theme.SurveyPrimaryAmber
import com.example.util.ExifHelper
import com.example.util.FileUtils
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoDetailScreen(
    photoId: Long,
    surveyRepository: SurveyRepository,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var photo by remember { mutableStateOf<PhotoEntity?>(null) }
    var exifDetails by remember { mutableStateOf(ExifHelper.ExifDetails()) }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editNote by remember { mutableStateOf("") }
    var editProjectName by remember { mutableStateOf("") }

    // Zoom and pan state
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 4f)
        offset += offsetChange
    }

    LaunchedEffect(photoId) {
        val found = surveyRepository.getPhotoById(photoId)
        photo = found
        found?.let {
            editNote = it.note
            editProjectName = it.projectName
            exifDetails = ExifHelper.readExif(it.filePath)
        }
    }

    BackHandler {
        onNavigateBack()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = photo?.fileName ?: "Detail Foto",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    photo?.let { currentPhoto ->
                        IconButton(
                            onClick = { FileUtils.sharePhoto(context, currentPhoto) },
                            modifier = Modifier.testTag("detail_share_btn")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = "Bagikan")
                        }
                        IconButton(
                            onClick = { showEditDialog = true },
                            modifier = Modifier.testTag("detail_edit_btn")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit")
                        }
                        IconButton(
                            onClick = { showDeleteDialog = true },
                            modifier = Modifier.testTag("detail_delete_btn")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        if (photo == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Memuat data foto...")
            }
        } else {
            val currentPhoto = photo!!
            val fileExists = File(currentPhoto.filePath).exists()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
            ) {
                // Interactive Photo Preview Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .background(Color.Black)
                        .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (fileExists) {
                        AsyncImage(
                            model = File(currentPhoto.filePath),
                            contentDescription = currentPhoto.fileName,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offset.x,
                                    translationY = offset.y
                                )
                                .transformable(state = transformState)
                        )
                    } else {
                        Text("Berkas foto tidak ditemukan", color = Color.White)
                    }

                    // Reset zoom button overlay
                    if (scale > 1f) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                        ) {
                            TextButton(
                                onClick = {
                                    scale = 1f
                                    offset = Offset.Zero
                                },
                                modifier = Modifier.background(Color(0x99000000), RoundedCornerShape(8.dp))
                            ) {
                                Text("Reset Zoom", color = Color.White, fontSize = 11.sp)
                            }
                        }
                    }
                }

                // Action Buttons Row: Buka di Maps, Salin Koordinat
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val coordText = "${currentPhoto.latitude}, ${currentPhoto.longitude}"
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Koordinat GPS", coordText))
                            Toast.makeText(context, "Koordinat berhasil disalin!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("copy_coordinates_btn")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Salin Koordinat", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            FileUtils.openInMaps(
                                context,
                                currentPhoto.latitude,
                                currentPhoto.longitude,
                                "${currentPhoto.projectName}: ${currentPhoto.note}"
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("open_in_maps_btn")
                    ) {
                        Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp), tint = SurveyPrimaryAmber)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Buka di Maps", fontSize = 12.sp)
                    }
                }

                // GPS & Location Section
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = SurveyPrimaryAmber, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Informasi Lokasi & GPS", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        DetailDataRow("Latitude", String.format(Locale.US, "%.6f°", currentPhoto.latitude))
                        DetailDataRow("Longitude", String.format(Locale.US, "%.6f°", currentPhoto.longitude))
                        DetailDataRow("Akurasi GPS", if (currentPhoto.accuracy > 0) "±${currentPhoto.accuracy.toInt()} meter" else "N/A")
                        DetailDataRow("Elevasi / Altitude", if (currentPhoto.altitude != 0.0) "${currentPhoto.altitude.toInt()} m dpl" else "-")
                        DetailDataRow("Arah / Bearing", "${currentPhoto.bearing.toInt()}°")

                        val adminLoc = listOf(currentPhoto.village, currentPhoto.district, currentPhoto.regency).filter { it.isNotBlank() }
                        if (adminLoc.isNotEmpty()) {
                            DetailDataRow("Wilayah", adminLoc.joinToString(", "))
                        }
                        if (currentPhoto.address.isNotBlank()) {
                            DetailDataRow("Alamat Lengkap", currentPhoto.address)
                        }
                    }
                }

                // Project & Survey Information Section
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = SurveyPrimaryAmber, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Data Proyek & Survei", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        DetailDataRow("Nama Proyek", currentPhoto.projectName)
                        DetailDataRow("Catatan / No. Aset", currentPhoto.note.ifBlank { "Tidak ada catatan" })
                        DetailDataRow("Tanggal Survei", currentPhoto.dateFormatted)
                        DetailDataRow("Waktu Survei", currentPhoto.timeFormatted)
                        DetailDataRow("Watermark", if (currentPhoto.isWatermarked) "Aktif (Tercetak)" else "Tidak Aktif")
                    }
                }

                // File & EXIF Technical Metadata Section
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .padding(bottom = 24.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Metadata File & EXIF", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        Spacer(modifier = Modifier.height(10.dp))

                        DetailDataRow("Nama File", currentPhoto.fileName)
                        DetailDataRow("Ukuran File", FileUtils.formatFileSize(currentPhoto.fileSizeBytes))
                        DetailDataRow("Resolusi", currentPhoto.resolution.ifBlank { "N/A" })
                        DetailDataRow("Perangkat", "${currentPhoto.manufacturer} ${currentPhoto.deviceModel}".trim())
                        DetailDataRow("Path Penyimpanan", currentPhoto.filePath)
                        if (exifDetails.userComment.isNotBlank()) {
                            DetailDataRow("EXIF Comment", exifDetails.userComment)
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog && photo != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Hapus Foto?") },
            text = { Text("Foto ${photo!!.fileName} akan dihapus secara permanen dari perangkat dan database.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            surveyRepository.deletePhoto(photo!!)
                            showDeleteDialog = false
                            Toast.makeText(context, "Foto berhasil dihapus", Toast.LENGTH_SHORT).show()
                            onNavigateBack()
                        }
                    }
                ) {
                    Text("Hapus Permanen", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Edit Survey Note & Project Dialog
    if (showEditDialog && photo != null) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Informasi Survei") },
            text = {
                Column {
                    OutlinedTextField(
                        value = editProjectName,
                        onValueChange = { editProjectName = it },
                        label = { Text("Nama Proyek") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editNote,
                        onValueChange = { editNote = it },
                        label = { Text("Keterangan Objek / No. Aset") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            val updated = photo!!.copy(
                                projectName = editProjectName.trim(),
                                note = editNote.trim()
                            )
                            surveyRepository.updatePhoto(updated)
                            photo = updated
                            showEditDialog = false
                            Toast.makeText(context, "Perubahan disimpan", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Simpan Perubahan", color = SurveyPrimaryAmber, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
private fun DetailDataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(130.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
    }
}
