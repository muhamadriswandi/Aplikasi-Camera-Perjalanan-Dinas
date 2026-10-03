package com.example.ui.camera

import android.view.MotionEvent
import android.view.Surface
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.MeteringPointFactory
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrandingWatermark
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.PhotoAlbum
import androidx.compose.material.icons.filled.WarningAmber
import com.example.data.repository.WatermarkPosition
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.example.ui.theme.SurveyNavyDark
import com.example.ui.theme.SurveyPrimaryAmber
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScreen(
    viewModel: CameraViewModel,
    onNavigateToPhotoDetail: (Long) -> Unit,
    onNavigateToAlbum: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val gpsData by viewModel.gpsState.collectAsState()
    val orientationData by viewModel.orientationState.collectAsState()
    val settings by viewModel.settingsState.collectAsState()
    val projects by viewModel.projectsList.collectAsState()
    val categories by viewModel.categoriesList.collectAsState()

    val lensFacing by viewModel.lensFacing.collectAsState()
    val flashMode by viewModel.flashMode.collectAsState()
    val zoomRatio by viewModel.zoomRatio.collectAsState()
    val isCapturing by viewModel.isCapturing.collectAsState()
    val selectedProject by viewModel.selectedProject.collectAsState()
    val surveyNote by viewModel.surveyNote.collectAsState()
    val surveyVillage by viewModel.surveyVillage.collectAsState()
    val surveyDistrict by viewModel.surveyDistrict.collectAsState()
    val surveyRegency by viewModel.surveyRegency.collectAsState()
    val latestPhoto by viewModel.latestPhoto.collectAsState()

    val showSurveyDialog by viewModel.showSurveyDialog.collectAsState()
    val showGpsWarningDialog by viewModel.showGpsWarningDialog.collectAsState()

    var showGridState by remember { mutableStateOf(settings.showGrid) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var cameraControl: CameraControl? by remember { mutableStateOf(null) }
    var camera: Camera? by remember { mutableStateOf(null) }
    var previewViewInstance by remember { mutableStateOf<PreviewView?>(null) }
    var cameraProviderInstance by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    val flipRotation by animateFloatAsState(
        targetValue = if (lensFacing == CameraSelector.LENS_FACING_FRONT) 180f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "flipRotation"
    )

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    DisposableEffect(lifecycleOwner) {
        viewModel.startSensors()
        onDispose {
            viewModel.stopSensors()
        }
    }

    // Bind camera helper to guarantee immediate real-time camera binding
    fun bindCameraToPreview(
        provider: ProcessCameraProvider,
        previewView: PreviewView,
        targetLens: Int
    ) {
        val preview = Preview.Builder().build().also {
            it.surfaceProvider = previewView.surfaceProvider
        }

        val capture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setFlashMode(flashMode)
            .build()
        imageCapture = capture

        val requestedSelector = CameraSelector.Builder()
            .requireLensFacing(targetLens)
            .build()

        val actualSelector = if (provider.hasCamera(requestedSelector)) {
            requestedSelector
        } else {
            if (targetLens == CameraSelector.LENS_FACING_FRONT && provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                CameraSelector.DEFAULT_BACK_CAMERA
            } else if (provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                requestedSelector
            }
        }

        try {
            provider.unbindAll()
            val cam = provider.bindToLifecycle(
                lifecycleOwner,
                actualSelector,
                preview,
                capture
            )
            camera = cam
            cameraControl = cam.cameraControl
            try {
                cam.cameraControl.setZoomRatio(zoomRatio)
            } catch (_: Exception) {}
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    var showWatermarkPosDialog by remember { mutableStateOf(false) }

    LaunchedEffect(flashMode) {
        imageCapture?.flashMode = flashMode
    }

    // Dynamic rotation for imageCapture so landscape captures produce landscape photos
    LaunchedEffect(orientationData.rotationAngle) {
        val targetRot = when (orientationData.rotationAngle.toInt()) {
            90 -> Surface.ROTATION_90
            180 -> Surface.ROTATION_180
            270 -> Surface.ROTATION_270
            else -> Surface.ROTATION_0
        }
        try {
            imageCapture?.targetRotation = targetRot
        } catch (_: Exception) {}
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // CameraX Viewfinder with key(lensFacing) for immediate real-time camera switching
        key(lensFacing) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }

                    // Tap-to-focus handler
                    previewView.setOnTouchListener { _, motionEvent ->
                        if (motionEvent.action == MotionEvent.ACTION_UP) {
                            val factory: MeteringPointFactory = previewView.meteringPointFactory
                            val point = factory.createPoint(motionEvent.x, motionEvent.y)
                            val action = FocusMeteringAction.Builder(point).build()
                            cameraControl?.startFocusAndMetering(action)
                        }
                        true
                    }

                    previewViewInstance = previewView

                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val provider = cameraProviderFuture.get()
                        cameraProviderInstance = provider
                        bindCameraToPreview(provider, previewView, lensFacing)
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, _, zoom, _ ->
                            val currentZoom = camera?.cameraInfo?.zoomState?.value?.zoomRatio ?: 1f
                            val newZoom = (currentZoom * zoom).coerceIn(1f, 8f)
                            viewModel.setZoom(newZoom)
                            cameraControl?.setZoomRatio(newZoom)
                        }
                    }
            )
        }

        // Grid Overlay
        if (showGridState) {
            CameraGridOverlay()
        }

        // Horizon Level Indicator Overlay
        if (settings.showLevelIndicator) {
            HorizonLevelOverlay(orientation = orientationData)
        }

        // Top Gradient & Control Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(SurveyNavyDark.copy(alpha = 0.85f), Color.Transparent)
                    )
                )
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Top Row: GPS Status & Bearing
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GpsStatusChip(
                        gpsData = gpsData,
                        onClick = { viewModel.openSurveyDialog() }
                    )

                    CompassHeadingChip(orientation = orientationData)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Action Controls: Flash, Grid, Flip Camera
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Flash Button
                        IconButton(
                            onClick = {
                                viewModel.cycleFlashMode()
                                imageCapture?.flashMode = viewModel.flashMode.value
                            },
                            modifier = Modifier
                                .testTag("flash_toggle_btn")
                                .background(Color(0x990F172A), CircleShape)
                                .size(40.dp)
                        ) {
                            Icon(
                                imageVector = when (flashMode) {
                                    ImageCapture.FLASH_MODE_ON -> Icons.Default.FlashOn
                                    ImageCapture.FLASH_MODE_AUTO -> Icons.Default.FlashAuto
                                    else -> Icons.Default.FlashOff
                                },
                                contentDescription = "Flash",
                                tint = if (flashMode != ImageCapture.FLASH_MODE_OFF) SurveyPrimaryAmber else Color.White
                            )
                        }

                        // Grid Toggle Button
                        IconButton(
                            onClick = { showGridState = !showGridState },
                            modifier = Modifier
                                .testTag("grid_toggle_btn")
                                .background(Color(0x990F172A), CircleShape)
                                .size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GridOn,
                                contentDescription = "Grid",
                                tint = if (showGridState) SurveyPrimaryAmber else Color.White.copy(alpha = 0.6f)
                            )
                        }

                        // Watermark Position Config Button (Portrait & Landscape)
                        IconButton(
                            onClick = { showWatermarkPosDialog = true },
                            modifier = Modifier
                                .testTag("watermark_pos_toggle_btn")
                                .background(Color(0x990F172A), CircleShape)
                                .size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.BrandingWatermark,
                                contentDescription = "Posisi Watermark",
                                tint = SurveyPrimaryAmber
                            )
                        }
                    }

                    // Flip Lens Button
                    IconButton(
                        onClick = { viewModel.toggleCameraLens() },
                        modifier = Modifier
                            .testTag("camera_flip_btn")
                            .background(Color(0x990F172A), CircleShape)
                            .size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cameraswitch,
                            contentDescription = "Ganti Kamera",
                            tint = Color.White,
                            modifier = Modifier.rotate(flipRotation)
                        )
                    }
                }
            }
        }

        // Bottom Controls Scrim & Action Center
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, SurveyNavyDark.copy(alpha = 0.95f))
                    )
                )
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Zoom Chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(1.0f, 2.0f, 5.0f).forEach { zoomLevel ->
                        val isSelected = (zoomRatio - zoomLevel).let { kotlin.math.abs(it) < 0.2f }
                        Box(
                            modifier = Modifier
                                .testTag("zoom_btn_${zoomLevel.toInt()}x")
                                .background(
                                    if (isSelected) SurveyPrimaryAmber else Color(0x990F172A),
                                    CircleShape
                                )
                                .clickable {
                                    viewModel.setZoom(zoomLevel)
                                    cameraControl?.setZoomRatio(zoomLevel)
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${zoomLevel.toInt()}x",
                                color = if (isSelected) Color.Black else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Survey Quick-Form Pill: Clicking opens Survey Form (Full Project Name, never limited!)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .testTag("survey_quick_pill")
                        .fillMaxWidth(0.92f)
                        .background(Color(0xDD1E293B), RoundedCornerShape(24.dp))
                        .border(1.dp, SurveyPrimaryAmber.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
                        .clickable { viewModel.openSurveyDialog() }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.EditNote,
                        contentDescription = null,
                        tint = SurveyPrimaryAmber,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selectedProject.ifBlank { "Pilih Proyek Survei" },
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 17.sp
                        )
                        Text(
                            text = if (surveyNote.isNotBlank()) "Ket: $surveyNote" else "Ketuk untuk isi catatan objek / no aset",
                            color = Color(0xFFCBD5E1),
                            fontSize = 11.sp,
                            lineHeight = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Shutter Row: Gallery Shortcut | Shutter Button | Album Shortcut
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Latest Photo Thumbnail
                    Box(
                        modifier = Modifier
                            .testTag("latest_photo_thumbnail")
                            .size(54.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x80334155))
                            .border(1.5.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .clickable {
                                latestPhoto?.let { onNavigateToPhotoDetail(it.id) } ?: onNavigateToAlbum()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (latestPhoto != null && File(latestPhoto!!.filePath).exists()) {
                            AsyncImage(
                                model = File(latestPhoto!!.filePath),
                                contentDescription = "Foto Terbaru",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.PhotoAlbum,
                                contentDescription = "Album",
                                tint = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Main Capture Shutter Button
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .border(4.dp, Color.White, CircleShape)
                            .padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .testTag("camera_shutter_btn")
                                .fillMaxSize()
                                .background(if (isCapturing) Color.Gray else SurveyPrimaryAmber, CircleShape)
                                .clickable(enabled = !isCapturing && imageCapture != null) {
                                    imageCapture?.let { viewModel.onCaptureClicked(it) }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCapturing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(28.dp),
                                    color = Color.White,
                                    strokeWidth = 3.dp
                                )
                            }
                        }
                    }

                    // Album Shortcut Button
                    IconButton(
                        onClick = onNavigateToAlbum,
                        modifier = Modifier
                            .testTag("open_album_btn")
                            .size(54.dp)
                            .background(Color(0x80334155), CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoAlbum,
                            contentDescription = "Daftar Album",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet for Survey Form
    if (showSurveyDialog) {
        SurveyFormSheet(
            sheetState = sheetState,
            projects = projects,
            categories = categories,
            selectedProject = selectedProject,
            surveyNote = surveyNote,
            surveyVillage = surveyVillage,
            surveyDistrict = surveyDistrict,
            surveyRegency = surveyRegency,
            gpsData = gpsData,
            onProjectSelected = { viewModel.setProject(it) },
            onNoteChanged = { viewModel.setSurveyNote(it) },
            onLocationOverridesChanged = { v, d, r -> viewModel.setLocationOverrides(v, d, r) },
            onAddNewProject = { name, desc, cat, color -> viewModel.addNewProject(name, desc, cat, color) },
            onAddCategory = { viewModel.addNewCategory(it) },
            onEditCategory = { cat, newName -> viewModel.editCategory(cat, newName) },
            onDeleteCategory = { viewModel.deleteCategory(it) },
            onDismiss = { viewModel.dismissSurveyDialog() }
        )
    }

    // GPS Warning Dialog
    if (showGpsWarningDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissGpsWarning() },
            icon = {
                Icon(
                    imageVector = Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = SurveyPrimaryAmber,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text("Menunggu Lokasi GPS...", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("Sinyal GPS belum terkunci atau akurasi belum memenuhi syarat. Apakah Anda tetap ingin mengambil foto tanpa koordinat valid?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        imageCapture?.let { viewModel.forceCaptureWithoutGps(it) }
                    }
                ) {
                    Text("Tetap Ambil Foto", color = SurveyPrimaryAmber, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissGpsWarning() }) {
                    Text("Tunggu GPS")
                }
            }
        )
    }

    // Watermark Position Dialog (Portrait & Landscape selection)
    if (showWatermarkPosDialog) {
        WatermarkPositionDialog(
            currentOrientationIsLandscape = orientationData.isLandscape,
            portraitPosition = settings.watermarkPositionPortrait,
            landscapePosition = settings.watermarkPositionLandscape,
            onSelectPortrait = { viewModel.setWatermarkPositionPortrait(it) },
            onSelectLandscape = { viewModel.setWatermarkPositionLandscape(it) },
            onDismiss = { showWatermarkPosDialog = false }
        )
    }
}

@Composable
fun WatermarkPositionDialog(
    currentOrientationIsLandscape: Boolean,
    portraitPosition: WatermarkPosition,
    landscapePosition: WatermarkPosition,
    onSelectPortrait: (WatermarkPosition) -> Unit,
    onSelectLandscape: (WatermarkPosition) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(if (currentOrientationIsLandscape) 1 else 0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.BrandingWatermark,
                contentDescription = null,
                tint = SurveyPrimaryAmber,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = "Posisi Watermark Foto",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Real-time camera orientation status banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F172A), RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = if (currentOrientationIsLandscape) "📷 Orientasi Saat Ini: LANDSCAPE (Mendatar)" else "📱 Orientasi Saat Ini: POTRET (Tegak)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SurveyPrimaryAmber
                    )
                }

                // Mode Tabs (Potret vs Landscape)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1E293B), RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Potret Tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (selectedTab == 0) SurveyPrimaryAmber else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedTab = 0 }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "📱 Mode Potret",
                            fontSize = 12.sp,
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 0) Color.Black else Color.White
                        )
                    }

                    // Landscape Tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (selectedTab == 1) SurveyPrimaryAmber else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { selectedTab = 1 }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🔄 Mode Landscape",
                            fontSize = 12.sp,
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 1) Color.Black else Color.White
                        )
                    }
                }

                val currentSelectedPos = if (selectedTab == 0) portraitPosition else landscapePosition
                val modeLabel = if (selectedTab == 0) "Potret" else "Landscape"

                Text(
                    text = "Pilih posisi watermark saat foto diambil dalam mode $modeLabel:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // 3x2 Grid Selector Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0F172A), RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFF334155), RoundedCornerShape(12.dp))
                        .padding(8.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        // Top row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(WatermarkPosition.TOP_LEFT, WatermarkPosition.TOP_CENTER, WatermarkPosition.TOP_RIGHT).forEach { pos ->
                                val isSelected = currentSelectedPos == pos
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            if (isSelected) SurveyPrimaryAmber else Color(0xFF1E293B),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) SurveyPrimaryAmber else Color(0xFF475569),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            if (selectedTab == 0) onSelectPortrait(pos) else onSelectLandscape(pos)
                                        }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = pos.label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.Black else Color.White
                                    )
                                }
                            }
                        }

                        // Bottom row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(WatermarkPosition.BOTTOM_LEFT, WatermarkPosition.BOTTOM_CENTER, WatermarkPosition.BOTTOM_RIGHT).forEach { pos ->
                                val isSelected = currentSelectedPos == pos
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(
                                            if (isSelected) SurveyPrimaryAmber else Color(0xFF1E293B),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) SurveyPrimaryAmber else Color(0xFF475569),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            if (selectedTab == 0) onSelectPortrait(pos) else onSelectLandscape(pos)
                                        }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = pos.label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.Black else Color.White
                                    )
                                }
                            }
                        }
                    }
                }

                Text(
                    text = "Posisi aktif: ${currentSelectedPos.label}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SurveyPrimaryAmber
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Selesai", color = SurveyPrimaryAmber, fontWeight = FontWeight.Bold)
            }
        }
    )
}
