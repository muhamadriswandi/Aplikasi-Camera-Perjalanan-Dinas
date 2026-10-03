package com.example.ui.camera

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.CategoryEntity
import com.example.data.local.GeoCameraDatabase
import com.example.data.local.PhotoEntity
import com.example.data.local.ProjectEntity
import com.example.data.repository.AppSettings
import com.example.data.repository.SettingsRepository
import com.example.data.repository.SurveyRepository
import com.example.data.repository.WatermarkPosition
import com.example.util.ExifHelper
import com.example.util.FileUtils
import com.example.util.GpsData
import com.example.util.GpsLocationHelper
import com.example.util.OrientationData
import com.example.util.OrientationSensorHelper
import com.example.util.WatermarkProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

sealed interface CameraUiEvent {
    data class ShowToast(val message: String) : CameraUiEvent
    data class PhotoSaved(val photoId: Long, val filePath: String) : CameraUiEvent
}

class CameraViewModel(application: Application) : AndroidViewModel(application) {

    private val db = GeoCameraDatabase.getDatabase(application, viewModelScope)
    val surveyRepository = SurveyRepository(db.photoDao(), db.projectDao(), db.categoryDao())
    val settingsRepository = SettingsRepository(application)

    private val gpsHelper = GpsLocationHelper(application, viewModelScope)
    private val orientationHelper = OrientationSensorHelper(application)

    val gpsState: StateFlow<GpsData> = gpsHelper.gpsData
    val orientationState: StateFlow<OrientationData> = orientationHelper.orientationData
    val settingsState: StateFlow<AppSettings> = settingsRepository.settings
    val projectsList: StateFlow<List<ProjectEntity>> = surveyRepository.allProjects
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())
    val categoriesList: StateFlow<List<CategoryEntity>> = surveyRepository.allCategories
        .stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), emptyList())

    // Camera State
    private val _lensFacing = MutableStateFlow(CameraSelector.LENS_FACING_BACK)
    val lensFacing: StateFlow<Int> = _lensFacing.asStateFlow()

    private val _flashMode = MutableStateFlow(ImageCapture.FLASH_MODE_OFF)
    val flashMode: StateFlow<Int> = _flashMode.asStateFlow()

    private val _zoomRatio = MutableStateFlow(1.0f)
    val zoomRatio: StateFlow<Float> = _zoomRatio.asStateFlow()

    private val _isCapturing = MutableStateFlow(false)
    val isCapturing: StateFlow<Boolean> = _isCapturing.asStateFlow()

    private val _selectedProject = MutableStateFlow("Pendataan Retribusi Pasar")
    val selectedProject: StateFlow<String> = _selectedProject.asStateFlow()

    private val _surveyNote = MutableStateFlow("")
    val surveyNote: StateFlow<String> = _surveyNote.asStateFlow()

    private val _surveyVillage = MutableStateFlow("")
    val surveyVillage: StateFlow<String> = _surveyVillage.asStateFlow()

    private val _surveyDistrict = MutableStateFlow("")
    val surveyDistrict: StateFlow<String> = _surveyDistrict.asStateFlow()

    private val _surveyRegency = MutableStateFlow("")
    val surveyRegency: StateFlow<String> = _surveyRegency.asStateFlow()

    private val _latestPhoto = MutableStateFlow<PhotoEntity?>(null)
    val latestPhoto: StateFlow<PhotoEntity?> = _latestPhoto.asStateFlow()

    private val _showSurveyDialog = MutableStateFlow(false)
    val showSurveyDialog: StateFlow<Boolean> = _showSurveyDialog.asStateFlow()

    private val _showGpsWarningDialog = MutableStateFlow(false)
    val showGpsWarningDialog: StateFlow<Boolean> = _showGpsWarningDialog.asStateFlow()

    private val _uiEvents = MutableSharedFlow<CameraUiEvent>()
    val uiEvents: SharedFlow<CameraUiEvent> = _uiEvents.asSharedFlow()

    val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    init {
        startSensors()
        viewModelScope.launch {
            surveyRepository.allPhotos.collect { photos ->
                _latestPhoto.value = photos.firstOrNull()
            }
        }
        viewModelScope.launch {
            surveyRepository.allProjects.collect { projects ->
                if (projects.isNotEmpty() && _selectedProject.value.isBlank()) {
                    _selectedProject.value = projects.first().name
                }
            }
        }
    }

    fun startSensors() {
        gpsHelper.startListening()
        orientationHelper.start()
    }

    fun stopSensors() {
        gpsHelper.stopListening()
        orientationHelper.stop()
    }

    fun toggleCameraLens() {
        val nextLens = if (_lensFacing.value == CameraSelector.LENS_FACING_BACK) {
            CameraSelector.LENS_FACING_FRONT
        } else {
            CameraSelector.LENS_FACING_BACK
        }
        _lensFacing.value = nextLens
        triggerVibration()
        val label = if (nextLens == CameraSelector.LENS_FACING_FRONT) "Kamera Depan" else "Kamera Belakang"
        viewModelScope.launch {
            _uiEvents.emit(CameraUiEvent.ShowToast("Beralih ke $label"))
        }
    }

    fun cycleFlashMode() {
        _flashMode.value = when (_flashMode.value) {
            ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_AUTO
            ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON
            else -> ImageCapture.FLASH_MODE_OFF
        }
    }

    fun cycleWatermarkPosition() {
        val isLandscape = orientationState.value.isLandscape
        val current = if (isLandscape) settingsState.value.watermarkPositionLandscape else settingsState.value.watermarkPositionPortrait
        val next = when (current) {
            WatermarkPosition.TOP_LEFT -> WatermarkPosition.TOP_CENTER
            WatermarkPosition.TOP_CENTER -> WatermarkPosition.TOP_RIGHT
            WatermarkPosition.TOP_RIGHT -> WatermarkPosition.BOTTOM_RIGHT
            WatermarkPosition.BOTTOM_RIGHT -> WatermarkPosition.BOTTOM_CENTER
            WatermarkPosition.BOTTOM_CENTER -> WatermarkPosition.BOTTOM_LEFT
            WatermarkPosition.BOTTOM_LEFT -> WatermarkPosition.TOP_LEFT
        }
        if (isLandscape) {
            setWatermarkPositionLandscape(next)
        } else {
            setWatermarkPositionPortrait(next)
        }
    }

    fun setWatermarkPositionPortrait(position: WatermarkPosition) {
        settingsRepository.updateSettings(
            settingsState.value.copy(
                watermarkPositionPortrait = position,
                watermarkPosition = position
            )
        )
        triggerVibration()
        viewModelScope.launch {
            _uiEvents.emit(CameraUiEvent.ShowToast("Posisi Watermark Potret: ${position.label}"))
        }
    }

    fun setWatermarkPositionLandscape(position: WatermarkPosition) {
        settingsRepository.updateSettings(
            settingsState.value.copy(
                watermarkPositionLandscape = position
            )
        )
        triggerVibration()
        viewModelScope.launch {
            _uiEvents.emit(CameraUiEvent.ShowToast("Posisi Watermark Landscape: ${position.label}"))
        }
    }

    fun setWatermarkPosition(position: WatermarkPosition) {
        setWatermarkPositionPortrait(position)
    }

    fun setZoom(ratio: Float) {
        _zoomRatio.value = ratio.coerceIn(0.5f, 10.0f)
    }

    fun setProject(projectName: String) {
        _selectedProject.value = projectName
    }

    fun setSurveyNote(note: String) {
        _surveyNote.value = note
    }

    fun setLocationOverrides(village: String, district: String, regency: String) {
        _surveyVillage.value = village
        _surveyDistrict.value = district
        _surveyRegency.value = regency
    }

    fun openSurveyDialog() {
        _showSurveyDialog.value = true
    }

    fun dismissSurveyDialog() {
        _showSurveyDialog.value = false
    }

    fun dismissGpsWarning() {
        _showGpsWarningDialog.value = false
    }

    fun onCaptureClicked(imageCapture: ImageCapture) {
        if (_isCapturing.value) return

        val settings = settingsState.value
        val gps = gpsState.value

        // Check GPS lock if required
        if (settings.requireGpsLock && !gps.isLocked) {
            _showGpsWarningDialog.value = true
            return
        }

        takePictureInternal(imageCapture)
    }

    fun forceCaptureWithoutGps(imageCapture: ImageCapture) {
        _showGpsWarningDialog.value = false
        takePictureInternal(imageCapture)
    }

    private fun takePictureInternal(imageCapture: ImageCapture) {
        _isCapturing.value = true
        triggerVibration()

        imageCapture.takePicture(
            cameraExecutor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    processCapturedImage(image)
                }

                override fun onError(exception: ImageCaptureException) {
                    _isCapturing.value = false
                    viewModelScope.launch {
                        _uiEvents.emit(CameraUiEvent.ShowToast("Gagal mengambil foto: ${exception.localizedMessage}"))
                    }
                }
            }
        )
    }

    private fun processCapturedImage(imageProxy: ImageProxy) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                val bitmap = imageProxy.toBitmap()
                imageProxy.close()

                val isFrontCamera = _lensFacing.value == CameraSelector.LENS_FACING_FRONT
                val rotatedBitmap = if (rotationDegrees != 0 || isFrontCamera) {
                    val matrix = Matrix().apply {
                        if (rotationDegrees != 0) postRotate(rotationDegrees.toFloat())
                        if (isFrontCamera) postScale(-1f, 1f, bitmap.width / 2f, bitmap.height / 2f)
                    }
                    Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                } else {
                    bitmap
                }

                val currentGps = gpsState.value
                val currentOrientation = orientationState.value
                val settings = settingsState.value
                val timestamp = System.currentTimeMillis()

                val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
                val year = cal.get(Calendar.YEAR)
                val month = cal.get(Calendar.MONTH) + 1
                val day = cal.get(Calendar.DAY_OF_MONTH)

                val datePattern = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
                val timePattern = SimpleDateFormat("HH:mm:ss", Locale("id", "ID"))
                val dateFormatted = datePattern.format(Date(timestamp))
                val timeFormatted = timePattern.format(Date(timestamp))

                val customPattern = SimpleDateFormat(settings.dateFormatPattern, Locale("id", "ID"))
                val fullDateTime = customPattern.format(Date(timestamp))

                val finalVillage = _surveyVillage.value.ifBlank { currentGps.village }
                val finalDistrict = _surveyDistrict.value.ifBlank { currentGps.district }
                val finalRegency = _surveyRegency.value.ifBlank { currentGps.regency }
                val projectName = _selectedProject.value.ifBlank { "Umum" }
                val note = _surveyNote.value

                val isLandscapePhoto = rotatedBitmap.width > rotatedBitmap.height
                val activePosition = if (isLandscapePhoto) {
                    settings.watermarkPositionLandscape
                } else {
                    settings.watermarkPositionPortrait
                }

                // Apply watermark if enabled
                val finalBitmap = if (settings.watermarkEnabled) {
                    val metadata = WatermarkProcessor.WatermarkMetadata(
                        projectName = projectName,
                        note = note,
                        latitude = currentGps.latitude,
                        longitude = currentGps.longitude,
                        altitude = currentGps.altitude,
                        accuracy = currentGps.accuracy,
                        bearing = if (currentGps.bearing != 0f) currentGps.bearing else currentOrientation.azimuthDegrees,
                        cardinalDirection = currentOrientation.cardinalDirection,
                        dmsString = currentGps.toDmsString(),
                        dateTimeString = fullDateTime,
                        regency = finalRegency,
                        district = finalDistrict,
                        village = finalVillage,
                        fullAddress = currentGps.address
                    )
                    WatermarkProcessor.applyWatermark(
                        sourceBitmap = rotatedBitmap,
                        metadata = metadata,
                        position = activePosition,
                        fontSizeOption = settings.watermarkFontSize
                    )
                } else {
                    rotatedBitmap
                }

                // Create target file in Pictures/GeoCamera/YYYY/MM/DD/Project/...
                val context = getApplication<Application>()
                val targetFile = FileUtils.createPhotoFile(context, projectName, timestamp)

                // Compress & Save JPEG
                FileOutputStream(targetFile).use { out ->
                    finalBitmap.compress(Bitmap.CompressFormat.JPEG, settings.jpegQuality, out)
                }

                // Write EXIF
                ExifHelper.writeExif(
                    filePath = targetFile.absolutePath,
                    latitude = currentGps.latitude,
                    longitude = currentGps.longitude,
                    altitude = currentGps.altitude,
                    timestamp = timestamp,
                    projectName = projectName,
                    surveyNote = note
                )

                // Save to Room DB
                val photoEntity = PhotoEntity(
                    fileName = targetFile.name,
                    filePath = targetFile.absolutePath,
                    latitude = currentGps.latitude,
                    longitude = currentGps.longitude,
                    altitude = currentGps.altitude,
                    accuracy = currentGps.accuracy,
                    bearing = if (currentGps.bearing != 0f) currentGps.bearing else currentOrientation.azimuthDegrees,
                    speed = currentGps.speed,
                    regency = finalRegency,
                    district = finalDistrict,
                    village = finalVillage,
                    address = currentGps.address,
                    projectName = projectName,
                    note = note,
                    dateFormatted = dateFormatted,
                    timeFormatted = timeFormatted,
                    year = year,
                    month = month,
                    day = day,
                    timestamp = timestamp,
                    resolution = "${finalBitmap.width}x${finalBitmap.height}",
                    fileSizeBytes = targetFile.length(),
                    deviceModel = Build.MODEL,
                    manufacturer = Build.MANUFACTURER,
                    isWatermarked = settings.watermarkEnabled
                )

                val id = surveyRepository.insertPhoto(photoEntity)
                _latestPhoto.value = photoEntity.copy(id = id)

                withContext(Dispatchers.Main) {
                    _isCapturing.value = false
                    _uiEvents.emit(CameraUiEvent.PhotoSaved(id, targetFile.absolutePath))
                    _uiEvents.emit(CameraUiEvent.ShowToast("Foto tersimpan di album $projectName"))
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _isCapturing.value = false
                    _uiEvents.emit(CameraUiEvent.ShowToast("Kesalahan saat memproses foto: ${e.localizedMessage}"))
                }
            }
        }
    }

    private fun triggerVibration() {
        if (!settingsState.value.soundVibrateFeedback) return
        try {
            val context = getApplication<Application>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(70, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v?.vibrate(VibrationEffect.createOneShot(70, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    v?.vibrate(70)
                }
            }
        } catch (_: Exception) {
        }
    }

    fun addNewProject(name: String, desc: String, category: String, colorHex: String) {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                val id = surveyRepository.insertProject(
                    ProjectEntity(
                        name = name.trim(),
                        description = desc.trim(),
                        category = category.trim().ifBlank { "Pajak & Retribusi" },
                        colorHex = colorHex
                    )
                )
                if (id > 0) {
                    _selectedProject.value = name.trim()
                    _uiEvents.emit(CameraUiEvent.ShowToast("Proyek '$name' dibuat"))
                }
            }
        }
    }

    fun addNewCategory(name: String, colorHex: String = "#F59E0B") {
        viewModelScope.launch {
            if (name.isNotBlank()) {
                surveyRepository.insertCategory(CategoryEntity(name = name.trim(), colorHex = colorHex))
                _uiEvents.emit(CameraUiEvent.ShowToast("Kategori '${name.trim()}' berhasil ditambahkan"))
            }
        }
    }

    fun editCategory(category: CategoryEntity, newName: String) {
        viewModelScope.launch {
            if (newName.isNotBlank()) {
                val oldName = category.name
                surveyRepository.updateCategory(category.copy(name = newName.trim()), oldName)
                _uiEvents.emit(CameraUiEvent.ShowToast("Kategori diperbarui menjadi '${newName.trim()}'"))
            }
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            surveyRepository.deleteCategory(category)
            _uiEvents.emit(CameraUiEvent.ShowToast("Kategori '${category.name}' berhasil dihapus"))
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopSensors()
        cameraExecutor.shutdown()
    }
}
