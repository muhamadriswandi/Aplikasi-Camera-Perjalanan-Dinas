package com.example.ui.album

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.DateRange
import com.example.ui.components.ManageCategoriesDialog
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.SurveyNavyDark
import com.example.ui.theme.SurveyPrimaryAmber
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumScreen(
    viewModel: AlbumViewModel,
    onOpenAlbumDetail: (category: String, title: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val isGridView by viewModel.isGridView.collectAsState()

    val projectAlbums by viewModel.projectAlbums.collectAsState()
    val dailyAlbums by viewModel.dailyAlbums.collectAsState()
    val monthlyAlbums by viewModel.monthlyAlbums.collectAsState()
    val yearlyAlbums by viewModel.yearlyAlbums.collectAsState()
    val allCategories by viewModel.allCategories.collectAsState()

    var showCreateProjectDialog by remember { mutableStateOf(false) }
    var showManageCategoriesDialog by remember { mutableStateOf(false) }
    var newProjName by remember { mutableStateOf("") }
    var newProjDesc by remember { mutableStateOf("") }
    var newProjCategory by remember { mutableStateOf("Pajak & Retribusi") }
    var showCategoryMenu by remember { mutableStateOf(false) }

    // Delete Project State
    var projectToDelete by remember { mutableStateOf<AlbumGroupInfo?>(null) }
    var deletePhotosPermanently by remember { mutableStateOf(false) }

    val currentAlbums = when (selectedCategory) {
        AlbumCategory.PROJECT -> projectAlbums
        AlbumCategory.DAILY -> dailyAlbums
        AlbumCategory.MONTHLY -> monthlyAlbums
        AlbumCategory.YEARLY -> yearlyAlbums
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Collections,
                            contentDescription = null,
                            tint = SurveyPrimaryAmber,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Koleksi Album",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    // Manage Categories Button
                    IconButton(
                        onClick = { showManageCategoriesDialog = true },
                        modifier = Modifier.testTag("album_manage_categories_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Category,
                            contentDescription = "Kelola Kategori",
                            tint = SurveyPrimaryAmber
                        )
                    }

                    IconButton(
                        onClick = { viewModel.toggleViewMode() },
                        modifier = Modifier.testTag("toggle_album_view_mode_btn")
                    ) {
                        Icon(
                            imageVector = if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                            contentDescription = "Ganti Tampilan",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            if (selectedCategory == AlbumCategory.PROJECT) {
                FloatingActionButton(
                    onClick = { showCreateProjectDialog = true },
                    containerColor = SurveyPrimaryAmber,
                    contentColor = Color.Black,
                    modifier = Modifier.testTag("fab_add_project")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah Proyek")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Category Tabs: Proyek, Harian, Bulanan, Tahunan
            PrimaryTabRow(
                selectedTabIndex = selectedCategory.ordinal,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = SurveyPrimaryAmber
            ) {
                AlbumCategory.values().forEach { category ->
                    val isSelected = selectedCategory == category
                    Tab(
                        selected = isSelected,
                        onClick = { viewModel.setCategory(category) },
                        text = {
                            Text(
                                text = category.label,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = when (category) {
                                    AlbumCategory.PROJECT -> Icons.Default.Folder
                                    AlbumCategory.DAILY -> Icons.Default.CalendarToday
                                    AlbumCategory.MONTHLY -> Icons.Default.DateRange
                                    AlbumCategory.YEARLY -> Icons.Default.PhotoLibrary
                                },
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
            }

            // Summary Subheader
            val totalPhotos = currentAlbums.sumOf { it.photoCount }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${currentAlbums.size} Album • $totalPhotos Total Foto",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }

            // Album Content List or Grid
            if (currentAlbums.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.PhotoAlbum,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Belum ada foto dalam album ${selectedCategory.label.lowercase()}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (isGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(currentAlbums, key = { it.title }) { album ->
                        val canDelete = selectedCategory == AlbumCategory.PROJECT && album.title != "Umum"
                        AlbumGridCard(
                            album = album,
                            onClick = { onOpenAlbumDetail(album.category.name, album.title) },
                            onDeleteClick = if (canDelete) {
                                {
                                    projectToDelete = album
                                    deletePhotosPermanently = false
                                }
                            } else null
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(currentAlbums, key = { it.title }) { album ->
                        val canDelete = selectedCategory == AlbumCategory.PROJECT && album.title != "Umum"
                        AlbumListCard(
                            album = album,
                            onClick = { onOpenAlbumDetail(album.category.name, album.title) },
                            onDeleteClick = if (canDelete) {
                                {
                                    projectToDelete = album
                                    deletePhotosPermanently = false
                                }
                            } else null
                        )
                    }
                }
            }
        }
    }

    // Dialog Delete Project
    if (projectToDelete != null) {
        val target = projectToDelete!!
        AlertDialog(
            onDismissRequest = { projectToDelete = null },
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
                    text = "Hapus Proyek '${target.title}'?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            },
            text = {
                Column {
                    if (target.photoCount > 0) {
                        Text(
                            text = "Terdapat ${target.photoCount} foto dalam proyek ini. Tentukan tindakan untuk berkas foto:",
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
                                Text("Pindahkan ${target.photoCount} foto ke album 'Umum'", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                Text("Hapus proyek beserta seluruh ${target.photoCount} fotonya dari memori", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        Text(
                            text = "Proyek ini belum memiliki foto. Yakin ingin menghapus proyek '${target.title}' dari daftar?",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteProject(target.title, deletePhotosPermanently)
                        projectToDelete = null
                    }
                ) {
                    Text("Hapus Proyek", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { projectToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }

    // Dialog Create Project with Category
    if (showCreateProjectDialog) {
        val categoryOptions = if (allCategories.isNotEmpty()) {
            allCategories.map { it.name }
        } else {
            listOf("Pajak & Retribusi", "Infrastruktur & PU", "Pengawasan & Audit", "Pertanahan & Aset", "Umum")
        }

        AlertDialog(
            onDismissRequest = { showCreateProjectDialog = false },
            title = { Text("Tambah Album Proyek", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    // Category Picker
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = newProjCategory,
                            onValueChange = { newProjCategory = it },
                            label = { Text("Kategori Kegiatan") },
                            singleLine = true,
                            trailingIcon = {
                                TextButton(onClick = { showCategoryMenu = true }) {
                                    Text("Pilih", fontSize = 12.sp, color = SurveyPrimaryAmber)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )

                        DropdownMenu(
                            expanded = showCategoryMenu,
                            onDismissRequest = { showCategoryMenu = false }
                        ) {
                            categoryOptions.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat) },
                                    onClick = {
                                        newProjCategory = cat
                                        showCategoryMenu = false
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("+ Kelola Kategori...", color = SurveyPrimaryAmber, fontWeight = FontWeight.Bold) },
                                onClick = {
                                    showCategoryMenu = false
                                    showManageCategoriesDialog = true
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = newProjName,
                        onValueChange = { newProjName = it },
                        label = { Text("Nama Proyek") },
                        placeholder = { Text("Misal: Inspeksi Drainase") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_new_project_name_input")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newProjDesc,
                        onValueChange = { newProjDesc = it },
                        label = { Text("Keterangan Singkat") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newProjName.isNotBlank()) {
                            viewModel.createNewProject(newProjName, newProjDesc, newProjCategory, "#F59E0B")
                            newProjName = ""
                            newProjDesc = ""
                            showCreateProjectDialog = false
                        }
                    },
                    enabled = newProjName.isNotBlank()
                ) {
                    Text("Simpan", color = SurveyPrimaryAmber, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateProjectDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // Manage Categories Dialog (Tambah, Edit, Hapus)
    if (showManageCategoriesDialog) {
        ManageCategoriesDialog(
            categories = allCategories,
            projects = viewModel.allProjects.collectAsState().value,
            onAddCategory = { viewModel.addNewCategory(it) },
            onEditCategory = { cat, newName -> viewModel.editCategory(cat, newName) },
            onDeleteCategory = { viewModel.deleteCategory(it) },
            onDismiss = { showManageCategoriesDialog = false }
        )
    }
}

@Composable
fun AlbumGridCard(
    album: AlbumGroupInfo,
    onClick: () -> Unit,
    onDeleteClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("album_card_${album.title}")
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column {
            // Cover Photo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(Color(0xFF1E293B)),
                contentAlignment = Alignment.Center
            ) {
                if (!album.coverPhotoPath.isNullOrBlank() && File(album.coverPhotoPath).exists()) {
                    AsyncImage(
                        model = File(album.coverPhotoPath),
                        contentDescription = album.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = SurveyPrimaryAmber,
                        modifier = Modifier.size(40.dp)
                    )
                }

                // Delete Button Overlay (for projects)
                if (onDeleteClick != null) {
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(28.dp)
                            .background(Color(0xCC0F172A), CircleShape)
                            .testTag("delete_project_btn_${album.title}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Hapus Proyek",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Photo Count Pill Badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .background(Color(0xDD0F172A), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "${album.photoCount} Foto",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Info Section
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = album.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = album.formattedSize,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (album.dateRangeText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = album.dateRangeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun AlbumListCard(
    album: AlbumGroupInfo,
    onClick: () -> Unit,
    onDeleteClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E293B)),
                contentAlignment = Alignment.Center
            ) {
                if (!album.coverPhotoPath.isNullOrBlank() && File(album.coverPhotoPath).exists()) {
                    AsyncImage(
                        model = File(album.coverPhotoPath),
                        contentDescription = album.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = SurveyPrimaryAmber,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = album.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${album.photoCount} Foto",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = SurveyPrimaryAmber
                    )
                    Text(
                        text = " • ${album.formattedSize}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (album.dateRangeText.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = album.dateRangeText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Delete action button if project album
            if (onDeleteClick != null) {
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.testTag("delete_project_list_btn_${album.title}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Hapus Proyek",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
