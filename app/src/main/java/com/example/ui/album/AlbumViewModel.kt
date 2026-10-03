package com.example.ui.album

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.CategoryEntity
import com.example.data.local.GeoCameraDatabase
import com.example.data.local.PhotoEntity
import com.example.data.local.ProjectEntity
import com.example.data.repository.SurveyRepository
import com.example.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AlbumGroupInfo(
    val title: String,
    val category: AlbumCategory,
    val photoCount: Int,
    val totalSizeBytes: Long,
    val formattedSize: String,
    val coverPhotoPath: String?,
    val dateRangeText: String,
    val projectColorHex: String? = null
)

enum class AlbumCategory(val label: String) {
    PROJECT("Proyek"),
    DAILY("Harian"),
    MONTHLY("Bulanan"),
    YEARLY("Tahunan")
}

sealed interface AlbumUiEvent {
    data class ShowToast(val message: String) : AlbumUiEvent
    data class ZipReady(val zipFile: File) : AlbumUiEvent
}

class AlbumViewModel(application: Application) : AndroidViewModel(application) {
    private val db = GeoCameraDatabase.getDatabase(application, viewModelScope)
    val repository = SurveyRepository(db.photoDao(), db.projectDao(), db.categoryDao())

    val allPhotos: StateFlow<List<PhotoEntity>> = repository.allPhotos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allProjects: StateFlow<List<ProjectEntity>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories: StateFlow<List<CategoryEntity>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedCategory = MutableStateFlow(AlbumCategory.PROJECT)
    val selectedCategory: StateFlow<AlbumCategory> = _selectedCategory.asStateFlow()

    private val _isGridView = MutableStateFlow(true)
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()

    // Multi-selection state for inside an album
    private val _selectedPhotoIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedPhotoIds: StateFlow<Set<Long>> = _selectedPhotoIds.asStateFlow()

    private val _uiEvents = MutableSharedFlow<AlbumUiEvent>()
    val uiEvents: SharedFlow<AlbumUiEvent> = _uiEvents.asSharedFlow()

    val projectAlbums: StateFlow<List<AlbumGroupInfo>> = combine(
        repository.allPhotos,
        repository.allProjects
    ) { photos, projects ->
        val photosByProject = photos.groupBy { it.projectName.ifBlank { "Umum" } }
        val allProjectNames = (projects.map { it.name } + photosByProject.keys + listOf("Umum")).distinct()
        allProjectNames.map { projName ->
            val photoList = photosByProject[projName] ?: emptyList()
            val projectEntity = projects.find { it.name == projName }
            createGroupInfo(projName, AlbumCategory.PROJECT, photoList, projectEntity?.colorHex)
        }.sortedWith(compareByDescending<AlbumGroupInfo> { it.photoCount }.thenBy { it.title })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val dailyAlbums: StateFlow<List<AlbumGroupInfo>> = repository.allPhotos.map { photos ->
        val grouped = photos.groupBy { it.dateFormatted }
        grouped.map { (dateStr, photoList) ->
            createGroupInfo(dateStr, AlbumCategory.DAILY, photoList)
        }.sortedByDescending { it.coverPhotoPath?.let { p -> File(p).lastModified() } ?: 0L }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthlyAlbums: StateFlow<List<AlbumGroupInfo>> = repository.allPhotos.map { photos ->
        val monthNames = arrayOf(
            "", "Januari", "Februari", "Maret", "April", "Mei", "Juni",
            "Juli", "Agustus", "September", "Oktober", "November", "Desember"
        )
        val grouped = photos.groupBy { photo ->
            val monthName = if (photo.month in 1..12) monthNames[photo.month] else "Bulan ${photo.month}"
            "$monthName ${photo.year}"
        }
        grouped.map { (monthStr, photoList) ->
            createGroupInfo(monthStr, AlbumCategory.MONTHLY, photoList)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val yearlyAlbums: StateFlow<List<AlbumGroupInfo>> = repository.allPhotos.map { photos ->
        val grouped = photos.groupBy { it.year.toString() }
        grouped.map { (yearStr, photoList) ->
            createGroupInfo(yearStr, AlbumCategory.YEARLY, photoList)
        }.sortedByDescending { it.title }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun createGroupInfo(
        title: String,
        category: AlbumCategory,
        photos: List<PhotoEntity>,
        projectColorHex: String? = null
    ): AlbumGroupInfo {
        val totalBytes = photos.sumOf { it.fileSizeBytes }
        val latest = photos.maxByOrNull { it.timestamp }
        val earliest = photos.minByOrNull { it.timestamp }

        val dateRangeText = if (earliest != null && latest != null) {
            if (earliest.dateFormatted == latest.dateFormatted) {
                earliest.dateFormatted
            } else {
                "${earliest.dateFormatted} - ${latest.dateFormatted}"
            }
        } else {
            ""
        }

        return AlbumGroupInfo(
            title = title,
            category = category,
            photoCount = photos.size,
            totalSizeBytes = totalBytes,
            formattedSize = FileUtils.formatFileSize(totalBytes),
            coverPhotoPath = latest?.filePath,
            dateRangeText = dateRangeText,
            projectColorHex = projectColorHex
        )
    }

    fun setCategory(category: AlbumCategory) {
        _selectedCategory.value = category
    }

    fun toggleViewMode() {
        _isGridView.value = !_isGridView.value
    }

    fun togglePhotoSelection(photoId: Long) {
        val current = _selectedPhotoIds.value.toMutableSet()
        if (current.contains(photoId)) {
            current.remove(photoId)
        } else {
            current.add(photoId)
        }
        _selectedPhotoIds.value = current
    }

    fun selectAllPhotos(photos: List<PhotoEntity>) {
        if (_selectedPhotoIds.value.size == photos.size) {
            _selectedPhotoIds.value = emptySet()
        } else {
            _selectedPhotoIds.value = photos.map { it.id }.toSet()
        }
    }

    fun clearSelection() {
        _selectedPhotoIds.value = emptySet()
    }

    fun deleteSelectedPhotos(allPhotos: List<PhotoEntity>) {
        val idsToDelete = _selectedPhotoIds.value
        val targets = allPhotos.filter { idsToDelete.contains(it.id) }
        viewModelScope.launch {
            repository.deletePhotosByIds(targets)
            _selectedPhotoIds.value = emptySet()
            _uiEvents.emit(AlbumUiEvent.ShowToast("${targets.size} foto berhasil dihapus"))
        }
    }

    fun moveSelectedPhotos(newProjectName: String) {
        val idsToMove = _selectedPhotoIds.value.toList()
        viewModelScope.launch {
            repository.movePhotosToProject(idsToMove, newProjectName)
            _selectedPhotoIds.value = emptySet()
            _uiEvents.emit(AlbumUiEvent.ShowToast("${idsToMove.size} foto dipindahkan ke $newProjectName"))
        }
    }

    fun exportSelectedToZip(photos: List<PhotoEntity>, prefix: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val targets = if (_selectedPhotoIds.value.isEmpty()) photos else photos.filter { _selectedPhotoIds.value.contains(it.id) }
            val zipFile = FileUtils.exportToZip(getApplication(), targets, prefix)
            withContext(Dispatchers.Main) {
                if (zipFile != null) {
                    _uiEvents.emit(AlbumUiEvent.ZipReady(zipFile))
                    _uiEvents.emit(AlbumUiEvent.ShowToast("ZIP berhasil dibuat: ${zipFile.name}"))
                } else {
                    _uiEvents.emit(AlbumUiEvent.ShowToast("Gagal membuat berkas ZIP"))
                }
            }
        }
    }

    fun deleteProject(projectName: String, deleteAssociatedPhotos: Boolean) {
        viewModelScope.launch {
            repository.deleteProjectByName(projectName, deleteAssociatedPhotos)
            _uiEvents.emit(AlbumUiEvent.ShowToast("Proyek '$projectName' berhasil dihapus"))
        }
    }

    fun createNewProject(name: String, desc: String, category: String, colorHex: String) {
        viewModelScope.launch {
            repository.insertProject(
                ProjectEntity(
                    name = name.trim(),
                    description = desc.trim(),
                    category = category.trim().ifBlank { "Pajak & Retribusi" },
                    colorHex = colorHex
                )
            )
            _uiEvents.emit(AlbumUiEvent.ShowToast("Proyek '$name' berhasil ditambahkan"))
        }
    }

    fun addNewCategory(name: String, colorHex: String = "#F59E0B") {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                repository.insertCategory(CategoryEntity(name = name.trim(), colorHex = colorHex))
                _uiEvents.emit(AlbumUiEvent.ShowToast("Kategori '${name.trim()}' berhasil ditambahkan"))
            }
        }
    }

    fun editCategory(category: CategoryEntity, newName: String) {
        viewModelScope.launch {
            if (newName.isNotBlank()) {
                val oldName = category.name
                repository.updateCategory(category.copy(name = newName.trim()), oldName)
                _uiEvents.emit(AlbumUiEvent.ShowToast("Kategori diperbarui menjadi '${newName.trim()}'"))
            }
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.deleteCategory(category)
            _uiEvents.emit(AlbumUiEvent.ShowToast("Kategori '${category.name}' berhasil dihapus"))
        }
    }
}
