package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.about.AboutScreen
import com.example.ui.album.AlbumDetailScreen
import com.example.ui.album.AlbumScreen
import com.example.ui.album.AlbumUiEvent
import com.example.ui.album.AlbumViewModel
import com.example.ui.camera.CameraScreen
import com.example.ui.camera.CameraUiEvent
import com.example.ui.camera.CameraViewModel
import com.example.ui.detail.PhotoDetailScreen
import com.example.ui.history.HistoryScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.SurveyNavyDark
import com.example.ui.theme.SurveyPrimaryAmber
import com.example.util.FileUtils

enum class NavItem(val label: String, val icon: ImageVector) {
    CAMERA("Kamera", Icons.Default.CameraAlt),
    ALBUM("Album", Icons.Default.CollectionsBookmark),
    HISTORY("Riwayat", Icons.Default.History),
    SETTINGS("Pengaturan", Icons.Default.Settings),
    ABOUT("Tentang", Icons.Default.Info)
}

sealed interface ScreenState {
    data class MainTab(val tab: NavItem) : ScreenState
    data class AlbumDetail(val category: String, val title: String) : ScreenState
    data class PhotoDetail(val photoId: Long, val returnScreen: ScreenState) : ScreenState
}

@Composable
fun GeoCameraApp(
    cameraViewModel: CameraViewModel,
    albumViewModel: AlbumViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf<ScreenState>(ScreenState.MainTab(NavItem.CAMERA)) }

    // Permission check
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasCameraPermission = permissions[Manifest.permission.CAMERA] == true || hasCameraPermission
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true ||
                hasLocationPermission
        if (hasLocationPermission) {
            cameraViewModel.startSensors()
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission || !hasLocationPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Collect Toast events
    LaunchedEffect(cameraViewModel) {
        cameraViewModel.uiEvents.collect { event ->
            when (event) {
                is CameraUiEvent.ShowToast -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                is CameraUiEvent.PhotoSaved -> {}
            }
        }
    }

    LaunchedEffect(albumViewModel) {
        albumViewModel.uiEvents.collect { event ->
            when (event) {
                is AlbumUiEvent.ShowToast -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                is AlbumUiEvent.ZipReady -> FileUtils.shareZipFile(context, event.zipFile)
            }
        }
    }

    val isRootTab = currentScreen is ScreenState.MainTab
    val activeTab = (currentScreen as? ScreenState.MainTab)?.tab ?: NavItem.CAMERA

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            if (isRootTab) {
                NavigationBar(
                    containerColor = SurveyNavyDark,
                    contentColor = Color.White,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .testTag("main_bottom_nav")
                ) {
                    NavItem.values().forEach { item ->
                        val isSelected = activeTab == item
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { currentScreen = ScreenState.MainTab(item) },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = item.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.Black,
                                selectedTextColor = SurveyPrimaryAmber,
                                indicatorColor = SurveyPrimaryAmber,
                                unselectedIconColor = Color(0xFF94A3B8),
                                unselectedTextColor = Color(0xFF94A3B8)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (!hasCameraPermission && activeTab == NavItem.CAMERA && isRootTab) {
                PermissionRequestBanner(
                    onRequestPermission = {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.CAMERA,
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                )
            } else {
                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "screen_transition"
                ) { screen ->
                    when (screen) {
                        is ScreenState.MainTab -> {
                            when (screen.tab) {
                                NavItem.CAMERA -> {
                                    CameraScreen(
                                        viewModel = cameraViewModel,
                                        onNavigateToPhotoDetail = { id ->
                                            currentScreen = ScreenState.PhotoDetail(id, screen)
                                        },
                                        onNavigateToAlbum = {
                                            currentScreen = ScreenState.MainTab(NavItem.ALBUM)
                                        }
                                    )
                                }
                                NavItem.ALBUM -> {
                                    AlbumScreen(
                                        viewModel = albumViewModel,
                                        onOpenAlbumDetail = { category, title ->
                                            currentScreen = ScreenState.AlbumDetail(category, title)
                                        }
                                    )
                                }
                                NavItem.HISTORY -> {
                                    HistoryScreen(
                                        albumViewModel = albumViewModel,
                                        onNavigateToPhotoDetail = { id ->
                                            currentScreen = ScreenState.PhotoDetail(id, screen)
                                        }
                                    )
                                }
                                NavItem.SETTINGS -> {
                                    SettingsScreen(
                                        settingsRepository = cameraViewModel.settingsRepository
                                    )
                                }
                                NavItem.ABOUT -> {
                                    AboutScreen()
                                }
                            }
                        }
                        is ScreenState.AlbumDetail -> {
                            AlbumDetailScreen(
                                categoryStr = screen.category,
                                albumTitle = screen.title,
                                viewModel = albumViewModel,
                                onNavigateBack = {
                                    currentScreen = ScreenState.MainTab(NavItem.ALBUM)
                                },
                                onNavigateToPhotoDetail = { id ->
                                    currentScreen = ScreenState.PhotoDetail(id, screen)
                                }
                            )
                        }
                        is ScreenState.PhotoDetail -> {
                            PhotoDetailScreen(
                                photoId = screen.photoId,
                                surveyRepository = cameraViewModel.surveyRepository,
                                onNavigateBack = {
                                    currentScreen = screen.returnScreen
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionRequestBanner(
    onRequestPermission: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SurveyNavyDark)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(SurveyPrimaryAmber.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = SurveyPrimaryAmber,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Izin Kamera & Lokasi Diperlukan",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "GeoCamera Survey memerlukan izin Kamera untuk mengambil foto dokumentasi dan izin GPS untuk menyematkan koordinat presisi tinggi pada watermark dan EXIF.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onRequestPermission,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("grant_permissions_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = SurveyPrimaryAmber),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Berikan Izin Sekarang", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
