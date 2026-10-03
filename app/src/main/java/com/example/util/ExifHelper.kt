package com.example.util

import android.media.ExifInterface
import android.os.Build
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

object ExifHelper {

    data class ExifDetails(
        val dateTime: String = "",
        val deviceMake: String = "",
        val deviceModel: String = "",
        val latitude: Double? = null,
        val longitude: Double? = null,
        val altitude: Double? = null,
        val userComment: String = ""
    )

    fun writeExif(
        filePath: String,
        latitude: Double,
        longitude: Double,
        altitude: Double,
        timestamp: Long,
        projectName: String,
        surveyNote: String
    ) {
        try {
            val file = File(filePath)
            if (!file.exists()) return

            val exif = ExifInterface(filePath)

            // Date Time in standard EXIF format "yyyy:MM:dd HH:mm:ss"
            val exifDatePattern = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.US)
            val dateStr = exifDatePattern.format(Date(timestamp))
            exif.setAttribute(ExifInterface.TAG_DATETIME, dateStr)
            exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, dateStr)
            exif.setAttribute(ExifInterface.TAG_DATETIME_DIGITIZED, dateStr)

            // Device details
            exif.setAttribute(ExifInterface.TAG_MAKE, Build.MANUFACTURER)
            exif.setAttribute(ExifInterface.TAG_MODEL, Build.MODEL)

            // GPS Coordinates
            if (latitude != 0.0 || longitude != 0.0) {
                exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, decimalToDms(latitude))
                exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, if (latitude >= 0) "N" else "S")

                exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, decimalToDms(longitude))
                exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, if (longitude >= 0) "E" else "W")

                if (altitude != 0.0) {
                    val altVal = abs(altitude)
                    exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE, "${(altVal * 100).toInt()}/100")
                    exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF, if (altitude >= 0) "0" else "1")
                }
            }

            // User Comment for Project and Note
            val comment = "Project: $projectName | Note: $surveyNote | App: GeoCamera Survey"
            exif.setAttribute(ExifInterface.TAG_USER_COMMENT, comment)

            exif.saveAttributes()
        } catch (_: Exception) {
            // Graceful error handling
        }
    }

    fun readExif(filePath: String): ExifDetails {
        return try {
            val exif = ExifInterface(filePath)
            val latLong = FloatArray(2)
            val hasCoords = exif.getLatLong(latLong)

            ExifDetails(
                dateTime = exif.getAttribute(ExifInterface.TAG_DATETIME) ?: "",
                deviceMake = exif.getAttribute(ExifInterface.TAG_MAKE) ?: "",
                deviceModel = exif.getAttribute(ExifInterface.TAG_MODEL) ?: "",
                latitude = if (hasCoords) latLong[0].toDouble() else null,
                longitude = if (hasCoords) latLong[1].toDouble() else null,
                altitude = exif.getAltitude(0.0),
                userComment = exif.getAttribute(ExifInterface.TAG_USER_COMMENT) ?: ""
            )
        } catch (_: Exception) {
            ExifDetails()
        }
    }

    private fun decimalToDms(coordinate: Double): String {
        val absCoord = abs(coordinate)
        val degrees = absCoord.toInt()
        val minutesDouble = (absCoord - degrees) * 60
        val minutes = minutesDouble.toInt()
        val secondsDouble = (minutesDouble - minutes) * 60
        val seconds = (secondsDouble * 1000).toInt()

        return "$degrees/1,$minutes/1,$seconds/1000"
    }
}
