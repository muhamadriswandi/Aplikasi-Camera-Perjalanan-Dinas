package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.GeoCameraApp
import com.example.ui.album.AlbumViewModel
import com.example.ui.camera.CameraViewModel
import com.example.ui.theme.GeoCameraTheme
import com.example.ui.theme.SurveyNavyDark

class MainActivity : ComponentActivity() {
    private val cameraViewModel: CameraViewModel by viewModels()
    private val albumViewModel: AlbumViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by cameraViewModel.settingsState.collectAsState()
            GeoCameraTheme(darkTheme = settings.darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = SurveyNavyDark
                ) {
                    GeoCameraApp(
                        cameraViewModel = cameraViewModel,
                        albumViewModel = albumViewModel
                    )
                }
            }
        }
    }
}
