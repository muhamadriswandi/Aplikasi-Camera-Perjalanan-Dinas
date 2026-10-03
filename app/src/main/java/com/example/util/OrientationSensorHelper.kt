package com.example.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs
import kotlin.math.roundToInt

data class OrientationData(
    val azimuthDegrees: Float = 0f,
    val pitchDegrees: Float = 0f,
    val rollDegrees: Float = 0f,
    val isLevel: Boolean = false,
    val cardinalDirection: String = "U", // Utara / N
    val isLandscape: Boolean = false,
    val rotationAngle: Float = 0f
)

class OrientationSensorHelper(context: Context) : SensorEventListener {
    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    private val rotationSensor =
        sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val accelerometer =
        sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetic =
        sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val _orientationData = MutableStateFlow(OrientationData())
    val orientationData: StateFlow<OrientationData> = _orientationData.asStateFlow()

    private val gravity = FloatArray(3)
    private val geomagnetic = FloatArray(3)
    private var hasGravity = false
    private var hasGeomagnetic = false

    private var isListening = false

    fun start() {
        if (isListening || sensorManager == null) return
        isListening = true

        if (rotationSensor != null) {
            sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            accelerometer?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            }
            magnetic?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            }
        }
    }

    fun stop() {
        if (!isListening) return
        isListening = false
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val rMatrix = FloatArray(9)
        val orientation = FloatArray(3)

        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rMatrix, event.values)
            SensorManager.getOrientation(rMatrix, orientation)
            updateOrientation(orientation)
        } else if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, gravity, 0, 3)
            hasGravity = true
            if (hasGeomagnetic) {
                if (SensorManager.getRotationMatrix(rMatrix, null, gravity, geomagnetic)) {
                    SensorManager.getOrientation(rMatrix, orientation)
                    updateOrientation(orientation)
                }
            }
        } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, geomagnetic, 0, 3)
            hasGeomagnetic = true
            if (hasGravity) {
                if (SensorManager.getRotationMatrix(rMatrix, null, gravity, geomagnetic)) {
                    SensorManager.getOrientation(rMatrix, orientation)
                    updateOrientation(orientation)
                }
            }
        }
    }

    private fun updateOrientation(orientation: FloatArray) {
        // orientation[0] = azimuth (-pi to pi)
        // orientation[1] = pitch (-pi/2 to pi/2)
        // orientation[2] = roll (-pi to pi)
        var azimuth = Math.toDegrees(orientation[0].toDouble()).toFloat()
        if (azimuth < 0) azimuth += 360f

        val pitch = Math.toDegrees(orientation[1].toDouble()).toFloat()
        val roll = Math.toDegrees(orientation[2].toDouble()).toFloat()

        val isLevel = abs(pitch) < 3.0f && abs(roll) < 3.0f

        val absRoll = abs(roll)
        val isLandscape = absRoll in 45f..135f
        val rotationAngle = when {
            roll in 45f..135f -> 270f
            roll in -135f..-45f -> 90f
            absRoll >= 135f -> 180f
            else -> 0f
        }

        val cardinal = when ((azimuth / 45f).roundToInt() % 8) {
            0 -> "U"   // Utara / N
            1 -> "TL"  // Timur Laut / NE
            2 -> "T"   // Timur / E
            3 -> "TG"  // Tenggara / SE
            4 -> "S"   // Selatan / S
            5 -> "BD"  // Barat Daya / SW
            6 -> "B"   // Barat / W
            7 -> "BL"  // Barat Laut / NW
            else -> "U"
        }

        _orientationData.value = OrientationData(
            azimuthDegrees = azimuth,
            pitchDegrees = pitch,
            rollDegrees = roll,
            isLevel = isLevel,
            cardinalDirection = cardinal,
            isLandscape = isLandscape,
            rotationAngle = rotationAngle
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
