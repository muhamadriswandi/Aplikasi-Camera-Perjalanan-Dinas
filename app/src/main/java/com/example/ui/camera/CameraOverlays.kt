package com.example.ui.camera

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsNotFixed
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.example.data.repository.WatermarkPosition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.SurveyGpsError
import com.example.ui.theme.SurveyGpsGreen
import com.example.ui.theme.SurveyGpsWarning
import com.example.ui.theme.SurveyPrimaryAmber
import com.example.util.GpsData
import com.example.util.OrientationData

@Composable
fun CameraGridOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        val oneThirdW = width / 3f
        val twoThirdW = width * 2f / 3f
        val oneThirdH = height / 3f
        val twoThirdH = height * 2f / 3f

        val gridColor = Color.White.copy(alpha = 0.35f)
        val strokeW = 1.dp.toPx()

        // Vertical lines
        drawLine(gridColor, Offset(oneThirdW, 0f), Offset(oneThirdW, height), strokeWidth = strokeW)
        drawLine(gridColor, Offset(twoThirdW, 0f), Offset(twoThirdW, height), strokeWidth = strokeW)

        // Horizontal lines
        drawLine(gridColor, Offset(0f, oneThirdH), Offset(width, oneThirdH), strokeWidth = strokeW)
        drawLine(gridColor, Offset(0f, twoThirdH), Offset(width, twoThirdH), strokeWidth = strokeW)
    }
}

@Composable
fun HorizonLevelOverlay(
    orientation: OrientationData,
    modifier: Modifier = Modifier
) {
    val rollAnim by animateFloatAsState(targetValue = orientation.rollDegrees, label = "roll")
    val pitchAnim by animateFloatAsState(targetValue = orientation.pitchDegrees, label = "pitch")
    val levelColor by animateColorAsState(
        targetValue = if (orientation.isLevel) SurveyGpsGreen else Color.White.copy(alpha = 0.7f),
        label = "levelColor"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(140.dp)
                .rotate(-rollAnim)
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val lineLength = 50.dp.toPx()
            val pitchOffset = (pitchAnim * 1.5f).coerceIn(-40f, 40f)

            // Left horizon wing
            drawLine(
                color = levelColor,
                start = Offset(center.x - lineLength - 16.dp.toPx(), center.y + pitchOffset),
                end = Offset(center.x - 16.dp.toPx(), center.y + pitchOffset),
                strokeWidth = 2.dp.toPx()
            )

            // Right horizon wing
            drawLine(
                color = levelColor,
                start = Offset(center.x + 16.dp.toPx(), center.y + pitchOffset),
                end = Offset(center.x + lineLength + 16.dp.toPx(), center.y + pitchOffset),
                strokeWidth = 2.dp.toPx()
            )

            // Center target dot
            drawCircle(
                color = levelColor,
                radius = 3.dp.toPx(),
                center = Offset(center.x, center.y + pitchOffset)
            )
        }

        // Degree readout badge
        if (orientation.isLevel) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = 70.dp)
                    .background(SurveyGpsGreen.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "LEVEL 0°",
                    color = Color.Black,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun GpsStatusChip(
    gpsData: GpsData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (statusBg, statusBorder, statusText, statusIcon) = when {
        gpsData.isLocked && gpsData.accuracy <= 10f -> {
            Tuple4(
                SurveyGpsGreen.copy(alpha = 0.2f),
                SurveyGpsGreen,
                "GPS Terkunci (±${gpsData.accuracy.toInt()}m)",
                Icons.Default.GpsFixed
            )
        }
        gpsData.isLocked -> {
            Tuple4(
                SurveyPrimaryAmber.copy(alpha = 0.2f),
                SurveyPrimaryAmber,
                "GPS Cukup (±${gpsData.accuracy.toInt()}m)",
                Icons.Default.GpsFixed
            )
        }
        gpsData.latitude != 0.0 -> {
            Tuple4(
                SurveyGpsWarning.copy(alpha = 0.2f),
                SurveyGpsWarning,
                "Mencari Akurasi...",
                Icons.Default.GpsNotFixed
            )
        }
        else -> {
            Tuple4(
                SurveyGpsError.copy(alpha = 0.2f),
                SurveyGpsError,
                "Menunggu GPS...",
                Icons.Default.GpsNotFixed
            )
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .testTag("gps_status_chip")
            .background(Color(0xCC0F172A), RoundedCornerShape(20.dp))
            .border(1.dp, statusBorder, RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(statusBorder, CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = statusText,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun CompassHeadingChip(
    orientation: OrientationData,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(Color(0xCC0F172A), RoundedCornerShape(20.dp))
            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Navigation,
            contentDescription = "Bearing",
            tint = SurveyPrimaryAmber,
            modifier = Modifier
                .size(14.dp)
                .rotate(orientation.azimuthDegrees)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "${orientation.azimuthDegrees.toInt()}° ${orientation.cardinalDirection}",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
