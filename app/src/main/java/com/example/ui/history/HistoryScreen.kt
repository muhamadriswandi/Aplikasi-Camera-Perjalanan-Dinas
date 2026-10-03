package com.example.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.PhotoEntity
import com.example.ui.album.AlbumViewModel
import com.example.ui.theme.SurveyPrimaryAmber
import java.io.File
import java.util.Calendar

enum class HistoryFilterTime(val label: String) {
    ALL("Semua"),
    TODAY("Hari Ini"),
    THIS_WEEK("Minggu Ini"),
    THIS_MONTH("Bulan Ini")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    albumViewModel: AlbumViewModel,
    onNavigateToPhotoDetail: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val allPhotos by albumViewModel.allPhotos.collectAsState()
    val allProjects by albumViewModel.allProjects.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedTimeFilter by remember { mutableStateOf(HistoryFilterTime.ALL) }
    var selectedProjectFilter by remember { mutableStateOf<String?>(null) }

    val filteredPhotos = remember(allPhotos, searchQuery, selectedTimeFilter, selectedProjectFilter) {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        val currentYear = cal.get(Calendar.YEAR)
        val currentMonth = cal.get(Calendar.MONTH) + 1
        val currentDay = cal.get(Calendar.DAY_OF_MONTH)

        allPhotos.filter { photo ->
            // Search Query
            val matchesQuery = if (searchQuery.isBlank()) true else {
                photo.fileName.contains(searchQuery, ignoreCase = true) ||
                photo.projectName.contains(searchQuery, ignoreCase = true) ||
                photo.village.contains(searchQuery, ignoreCase = true) ||
                photo.district.contains(searchQuery, ignoreCase = true) ||
                photo.regency.contains(searchQuery, ignoreCase = true) ||
                photo.address.contains(searchQuery, ignoreCase = true) ||
                photo.note.contains(searchQuery, ignoreCase = true) ||
                photo.dateFormatted.contains(searchQuery, ignoreCase = true) ||
                "${photo.latitude},${photo.longitude}".contains(searchQuery)
            }

            // Project filter
            val matchesProject = selectedProjectFilter == null || photo.projectName == selectedProjectFilter

            // Time filter
            val matchesTime = when (selectedTimeFilter) {
                HistoryFilterTime.ALL -> true
                HistoryFilterTime.TODAY -> photo.year == currentYear && photo.month == currentMonth && photo.day == currentDay
                HistoryFilterTime.THIS_WEEK -> (now - photo.timestamp) <= 7 * 24 * 60 * 60 * 1000L
                HistoryFilterTime.THIS_MONTH -> photo.year == currentYear && photo.month == currentMonth
            }

            matchesQuery && matchesProject && matchesTime
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = SurveyPrimaryAmber,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Riwayat Survei",
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
        ) {
            // Search Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari proyek, desa, catatan, koordinat...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Cari", tint = SurveyPrimaryAmber)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Bersihkan")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SurveyPrimaryAmber
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("history_search_input")
                )
            }

            // Filter Chips Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "Filter",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )

                // Time Filters
                HistoryFilterTime.values().forEach { timeFilter ->
                    val isSelected = selectedTimeFilter == timeFilter
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedTimeFilter = timeFilter },
                        label = { Text(timeFilter.label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SurveyPrimaryAmber,
                            selectedLabelColor = Color.Black
                        )
                    )
                }

                // Project Filters
                allProjects.forEach { proj ->
                    val isSelected = selectedProjectFilter == proj.name
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedProjectFilter = if (isSelected) null else proj.name
                        },
                        label = { Text(proj.name, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SurveyPrimaryAmber,
                            selectedLabelColor = Color.Black
                        )
                    )
                }
            }

            // Results Counter
            Text(
                text = "Ditemukan ${filteredPhotos.size} foto",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            // Results List
            if (filteredPhotos.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Tidak ada foto yang cocok dengan pencarian",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredPhotos, key = { it.id }) { photo ->
                        HistoryPhotoItem(
                            photo = photo,
                            onClick = { onNavigateToPhotoDetail(photo.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryPhotoItem(
    photo: PhotoEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("history_item_${photo.id}")
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
            // Photo Thumbnail
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E293B)),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = File(photo.filePath),
                    contentDescription = photo.fileName,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                // Project Badge
                Box(
                    modifier = Modifier
                        .background(SurveyPrimaryAmber.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = photo.projectName,
                        color = SurveyPrimaryAmber,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Date and Time
                Text(
                    text = "${photo.dateFormatted} • ${photo.timeFormatted}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )

                // Note or Coordinates
                if (photo.note.isNotBlank()) {
                    Text(
                        text = "Ket: ${photo.note}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Location / GPS summary
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    val locSummary = listOf(photo.village, photo.district, photo.regency).filter { it.isNotBlank() }.joinToString(", ")
                    Text(
                        text = if (locSummary.isNotBlank()) locSummary else "Lat: ${photo.latitude}, Lng: ${photo.longitude}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
