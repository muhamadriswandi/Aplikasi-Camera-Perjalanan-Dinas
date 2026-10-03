package com.example.ui.album

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.PhotoEntity
import com.example.ui.theme.SurveyGpsGreen
import com.example.ui.theme.SurveyPrimaryAmber
import com.example.util.FileUtils
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    categoryStr: String,
    albumTitle: String,
    viewModel: AlbumViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPhotoDetail: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allPhotos by viewModel.allPhotos.collectAsState()
    val allProjects by viewModel.allProjects.collectAsState()
    val isGridView by viewModel.isGridView.collectAsState()
    val selectedIds by viewModel.selectedPhotoIds.collectAsState()

    val isSelectionMode = selectedIds.isNotEmpty()

    // Filter photos based on category
    val albumPhotos = remember(allPhotos, categoryStr, albumTitle) {
        when (categoryStr) {
            AlbumCategory.PROJECT.name -> allPhotos.filter { it.projectName.ifBlank { "Umum" } == albumTitle }
            AlbumCategory.DAILY.name -> allPhotos.filter { it.dateFormatted == albumTitle }
            AlbumCategory.MONTHLY.name -> {
                val monthNames = arrayOf("", "Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September", "Oktober", "November", "Desember")
                allPhotos.filter {
                    val m = if (it.month in 1..12) monthNames[it.month] else "Bulan ${it.month}"
                    "$m ${it.year}" == albumTitle
                }
            }
            AlbumCategory.YEARLY.name -> allPhotos.filter { it.year.toString() == albumTitle }
            else -> allPhotos
        }
    }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showDeleteProjectDialog by remember { mutableStateOf(false) }
    var deletePhotosPermanently by remember { mutableStateOf(false) }
    var showMoveMenu by remember { mutableStateOf(false) }

    BackHandler {
        if (isSelectionMode) {
            viewModel.clearSelection()
        } else {
            onNavigateBack()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isSelectionMode) "${selectedIds.size} Dipilih" else albumTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 18.sp
                        )
                        if (!isSelectionMode) {
                            Text(
                                text = "${albumPhotos.size} foto • ${FileUtils.formatFileSize(albumPhotos.sumOf { it.fileSizeBytes })}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (isSelectionMode) viewModel.clearSelection() else onNavigateBack()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali"
                        )
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        // Select All
                        IconButton(onClick = { viewModel.selectAllPhotos(albumPhotos) }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Pilih Semua")
                        }
                        // Share
                        IconButton(onClick = {
                            val targets = albumPhotos.filter { selectedIds.contains(it.id) }
                            FileUtils.shareMultiplePhotos(context, targets)
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Bagikan")
                        }
                        // Move to Project
                        IconButton(onClick = { showMoveMenu = true }) {
                            Icon(Icons.Default.DriveFileMove, contentDescription = "Pindahkan Proyek")
                        }
                        // Export ZIP
                        IconButton(onClick = {
                            viewModel.exportSelectedToZip(albumPhotos, albumTitle)
                        }) {
                            Icon(Icons.Default.Archive, contentDescription = "Ekspor ZIP")
                        }
                        // Delete
                        IconButton(onClick = { showDeleteConfirmDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = MaterialTheme.colorScheme.error)
                        }

                        // Dropdown for Move Project
                        DropdownMenu(
                            expanded = showMoveMenu,
                            onDismissRequest = { showMoveMenu = false }
                        ) {
                            allProjects.forEach { proj ->
                                DropdownMenuItem(
                                    text = { Text(proj.name) },
                                    onClick = {
                                        viewModel.moveSelectedPhotos(proj.name)
                                        showMoveMenu = false
                                    }
                                )
                            }
                        }
                    } else {
                        // Toggle Grid/List
                        IconButton(onClick = { viewModel.toggleViewMode() }) {
                            Icon(
                                imageVector = if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                                contentDescription = "Ganti Tampilan"
                            )
                        }
                        // Export entire album to ZIP
                        IconButton(onClick = {
                            viewModel.exportSelectedToZip(albumPhotos, albumTitle)
                        }) {
                            Icon(Icons.Default.Archive, contentDescription = "Ekspor ZIP", tint = SurveyPrimaryAmber)
                        }

                        // Delete Project Action if viewing a Project Album
                        if (categoryStr == AlbumCategory.PROJECT.name && albumTitle != "Umum") {
                            IconButton(
                                onClick = { showDeleteProjectDialog = true },
                                modifier = Modifier.testTag("detail_delete_project_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Hapus Proyek",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        if (albumPhotos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Tidak ada foto di album ini",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (isGridView) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                items(albumPhotos, key = { it.id }) { photo ->
                    val isSelected = selectedIds.contains(photo.id)
                    PhotoGridItem(
                        photo = photo,
                        isSelected = isSelected,
                        isSelectionMode = isSelectionMode,
                        onClick = {
                            if (isSelectionMode) {
                                viewModel.togglePhotoSelection(photo.id)
                            } else {
                                onNavigateToPhotoDetail(photo.id)
                            }
                        },
                        onLongClick = {
                            viewModel.togglePhotoSelection(photo.id)
                        }
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                items(albumPhotos, key = { it.id }) { photo ->
                    val isSelected = selectedIds.contains(photo.id)
                    PhotoListItem(
                        photo = photo,
                        isSelected = isSelected,
                        isSelectionMode = isSelectionMode,
                        onClick = {
                            if (isSelectionMode) {
                                viewModel.togglePhotoSelection(photo.id)
                            } else {
                                onNavigateToPhotoDetail(photo.id)
                            }
                        },
                        onLongClick = {
                            viewModel.togglePhotoSelection(photo.id)
                        }
                    )
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Hapus Foto Terpilih?") },
            text = { Text("${selectedIds.size} foto yang dipilih akan dihapus secara permanen dari perangkat.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSelectedPhotos(albumPhotos)
                        showDeleteConfirmDialog = false
                    }
                ) {
                    Text("Hapus", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Delete Entire Project Dialog
    if (showDeleteProjectDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteProjectDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Hapus Proyek '$albumTitle'?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Column {
                    if (albumPhotos.isNotEmpty()) {
                        Text(
                            text = "Terdapat ${albumPhotos.size} foto dalam proyek ini. Tentukan tindakan untuk berkas foto:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Option 1: Pindahkan foto ke 'Umum'
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { deletePhotosPermanently = false }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = !deletePhotosPermanently,
                                onClick = { deletePhotosPermanently = false },
                                colors = RadioButtonDefaults.colors(selectedColor = SurveyPrimaryAmber)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text("Simpan Foto", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("Pindahkan ${albumPhotos.size} foto ke album 'Umum'", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        // Option 2: Hapus permanen
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { deletePhotosPermanently = true }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = deletePhotosPermanently,
                                onClick = { deletePhotosPermanently = true },
                                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.error)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text("Hapus Permanen", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                                Text("Hapus proyek beserta seluruh ${albumPhotos.size} fotonya dari memori", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        Text(
                            text = "Proyek ini belum memiliki foto. Yakin ingin menghapus proyek '$albumTitle'?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteProject(albumTitle, deletePhotosPermanently)
                        showDeleteProjectDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text("Hapus Proyek", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteProjectDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

@Composable
fun PhotoGridItem(
    photo: PhotoEntity,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .testTag("photo_grid_item_${photo.id}")
            .height(115.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1E293B))
            .border(
                width = if (isSelected) 2.5.dp else 0.5.dp,
                color = if (isSelected) SurveyPrimaryAmber else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = File(photo.filePath),
            contentDescription = photo.fileName,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Selection Checkmark Overlay
        if (isSelectionMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(22.dp)
                    .background(if (isSelected) SurveyPrimaryAmber else Color(0x80000000), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (isSelected) Color.Black else Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Bottom timestamp scrim
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color(0x99000000))
                .padding(horizontal = 4.dp, vertical = 2.dp)
        ) {
            Text(
                text = photo.timeFormatted,
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
fun PhotoListItem(
    photo: PhotoEntity,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("photo_list_item_${photo.id}")
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) SurveyPrimaryAmber.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E293B))
            ) {
                AsyncImage(
                    model = File(photo.filePath),
                    contentDescription = photo.fileName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Info Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = photo.fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${photo.dateFormatted} • ${photo.timeFormatted}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (photo.note.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Ket: ${photo.note}",
                        style = MaterialTheme.typography.bodySmall,
                        color = SurveyPrimaryAmber,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                val locText = listOf(photo.village, photo.district).filter { it.isNotBlank() }.joinToString(", ")
                if (locText.isNotBlank()) {
                    Text(
                        text = locText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 1
                    )
                }
            }

            if (isSelectionMode) {
                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (isSelected) SurveyPrimaryAmber else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
