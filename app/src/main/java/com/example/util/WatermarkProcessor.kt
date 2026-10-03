package com.example.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.example.data.repository.WatermarkPosition
import java.util.Locale

object WatermarkProcessor {

    data class WatermarkMetadata(
        val projectName: String,
        val note: String,
        val latitude: Double,
        val longitude: Double,
        val altitude: Double,
        val accuracy: Float,
        val bearing: Float,
        val cardinalDirection: String,
        val dmsString: String,
        val dateTimeString: String,
        val regency: String,
        val district: String,
        val village: String,
        val fullAddress: String
    )

    fun applyWatermark(
        sourceBitmap: Bitmap,
        metadata: WatermarkMetadata,
        position: WatermarkPosition,
        fontSizeOption: String = "Normal"
    ): Bitmap {
        val width = sourceBitmap.width
        val height = sourceBitmap.height

        // Detect orientation: portrait vs landscape
        val isLandscape = width > height
        val baseDimension = if (isLandscape) height else width

        // Responsive scale based on short dimension so it looks balanced in both portrait and landscape
        val baseScale = (baseDimension.toFloat() / 1080f).coerceIn(0.65f, 2.2f)
        val fontMultiplier = when (fontSizeOption) {
            "Kecil" -> 0.85f
            "Besar" -> 1.25f
            else -> 1.0f
        }
        val scale = baseScale * fontMultiplier

        val titleSize = 25f * scale
        val projectTitleSize = 23f * scale
        val bodySize = 19f * scale
        val captionSize = 15f * scale
        val lineSpacing = 6f * scale
        val padding = 18f * scale
        val accentBarWidth = 6f * scale

        // Max card width: allows plenty of room without covering entire frame
        val maxCardWidth = if (isLandscape) {
            (width * 0.72f).coerceIn(400f * scale, width * 0.88f)
        } else {
            (width * 0.94f)
        }
        val maxTextAllowedWidth = maxCardWidth - padding * 2 - accentBarWidth - (8f * scale)

        // Paint helper
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val renderLines = mutableListOf<RenderLine>()

        // 1. Branding Header
        renderLines.add(
            RenderLine(
                text = "🌐 GEOCAMERA SURVEY",
                textSize = titleSize,
                color = Color.parseColor("#F59E0B"),
                isBold = true
            )
        )

        // 2. Full Project Name (Word-wrapped, never truncated!)
        val projectName = metadata.projectName.ifBlank { "Umum" }
        val projectLines = wrapTextLines(
            fullText = projectName,
            textSize = projectTitleSize,
            isBold = true,
            color = Color.parseColor("#FDE047"), // Vibrant Yellow for prominence
            paint = paint,
            maxLineWidth = maxTextAllowedWidth,
            prefix = "📁 Proyek: "
        )
        renderLines.addAll(projectLines)

        // 3. Coordinates (Decimal & DMS)
        val latSign = if (metadata.latitude >= 0) "N" else "S"
        val lngSign = if (metadata.longitude >= 0) "E" else "W"
        val coordDec = String.format(Locale.US, "%.6f° %s, %.6f° %s", Math.abs(metadata.latitude), latSign, Math.abs(metadata.longitude), lngSign)
        renderLines.add(
            RenderLine(
                text = "📍 $coordDec",
                textSize = bodySize,
                color = Color.WHITE,
                isBold = true
            )
        )

        if (metadata.dmsString.isNotBlank() && metadata.dmsString != "GPS Belum Terkunci") {
            renderLines.add(
                RenderLine(
                    text = "   ${metadata.dmsString}",
                    textSize = captionSize,
                    color = Color.parseColor("#CBD5E1")
                )
            )
        }

        // 4. Accuracy, Altitude, Bearing
        val accStr = if (metadata.accuracy < 100f) String.format(Locale.US, "±%.1fm", metadata.accuracy) else "N/A"
        val altStr = if (metadata.altitude != 0.0) String.format(Locale.US, "%.1fm dpl", metadata.altitude) else "-"
        val bearStr = String.format(Locale.US, "%.0f° %s", metadata.bearing, metadata.cardinalDirection)
        renderLines.add(
            RenderLine(
                text = "🎯 Akurasi: $accStr | Elevasi: $altStr | Arah: $bearStr",
                textSize = bodySize,
                color = Color.parseColor("#E2E8F0")
            )
        )

        // 5. Date & Time
        renderLines.add(
            RenderLine(
                text = "🕒 ${metadata.dateTimeString}",
                textSize = bodySize,
                color = Color.parseColor("#E2E8F0")
            )
        )

        // 6. Location Details (Desa, Kecamatan, Kabupaten)
        val locParts = listOf(metadata.village, metadata.district, metadata.regency).filter { it.isNotBlank() }
        val locText = if (locParts.isNotEmpty()) locParts.joinToString(", ") else metadata.fullAddress
        if (locText.isNotBlank()) {
            val locLines = wrapTextLines(
                fullText = locText,
                textSize = bodySize,
                isBold = false,
                color = Color.parseColor("#38BDF8"),
                paint = paint,
                maxLineWidth = maxTextAllowedWidth,
                prefix = "🏛️ "
            )
            renderLines.addAll(locLines)
        }

        // 7. Survey Note / Object Code
        if (metadata.note.isNotBlank()) {
            val noteLines = wrapTextLines(
                fullText = metadata.note,
                textSize = bodySize,
                isBold = true,
                color = Color.parseColor("#FCD34D"),
                paint = paint,
                maxLineWidth = maxTextAllowedWidth,
                prefix = "📝 Ket: "
            )
            renderLines.addAll(noteLines)
        }

        // Measure text lines to compute exact card bounding box
        var maxMeasuredTextWidth = 0f
        var totalTextHeight = 0f

        renderLines.forEach { line ->
            paint.textSize = line.textSize
            paint.typeface = if (line.isBold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            val w = paint.measureText(line.text)
            if (w > maxMeasuredTextWidth) maxMeasuredTextWidth = w
            totalTextHeight += line.textSize + lineSpacing
        }

        val cardWidth = (maxMeasuredTextWidth + padding * 2 + accentBarWidth + 10f * scale).coerceAtMost(maxCardWidth)
        val cardHeight = totalTextHeight + padding * 2

        // Determine 6-position coordinates:
        // TOP_LEFT, TOP_CENTER, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT
        val margin = 20f * scale

        val cardLeft: Float = when (position) {
            WatermarkPosition.TOP_LEFT, WatermarkPosition.BOTTOM_LEFT -> margin
            WatermarkPosition.TOP_CENTER, WatermarkPosition.BOTTOM_CENTER -> ((width - cardWidth) / 2f).coerceAtLeast(margin)
            WatermarkPosition.TOP_RIGHT, WatermarkPosition.BOTTOM_RIGHT -> (width - cardWidth - margin).coerceAtLeast(margin)
        }

        val cardTop: Float = when (position) {
            WatermarkPosition.TOP_LEFT, WatermarkPosition.TOP_CENTER, WatermarkPosition.TOP_RIGHT -> margin
            WatermarkPosition.BOTTOM_LEFT, WatermarkPosition.BOTTOM_CENTER, WatermarkPosition.BOTTOM_RIGHT -> (height - cardHeight - margin).coerceAtLeast(margin)
        }

        val cardRect = RectF(cardLeft, cardTop, cardLeft + cardWidth, cardTop + cardHeight)

        // Create mutable bitmap & canvas
        val outputBitmap = sourceBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(outputBitmap)

        // Draw Scrim Background (Frosted Dark Slate, high contrast)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D90B132B") // ~85% opacity dark navy slate
            style = Paint.Style.FILL
        }
        val cornerRadius = 16f * scale
        canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, bgPaint)

        // Draw Subtle Cyan / Slate Border
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#5038BDF8")
            style = Paint.Style.STROKE
            strokeWidth = 2f * scale
        }
        canvas.drawRoundRect(cardRect, cornerRadius, cornerRadius, borderPaint)

        // Draw Amber Left Indicator Accent Bar
        val accentRect = RectF(
            cardLeft + 2f * scale,
            cardTop + cornerRadius * 0.7f,
            cardLeft + accentBarWidth + 2f * scale,
            cardTop + cardHeight - cornerRadius * 0.7f
        )
        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F59E0B")
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(accentRect, 3f * scale, 3f * scale, accentPaint)

        // Draw all lines of text without truncation
        val textStartX = cardLeft + padding + accentBarWidth + 4f * scale
        var currentY = cardTop + padding

        renderLines.forEach { line ->
            paint.textSize = line.textSize
            paint.typeface = if (line.isBold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            paint.color = line.color

            currentY += line.textSize
            canvas.drawText(line.text, textStartX, currentY, paint)
            currentY += lineSpacing
        }

        return outputBitmap
    }

    private fun wrapTextLines(
        fullText: String,
        textSize: Float,
        isBold: Boolean,
        color: Int,
        paint: Paint,
        maxLineWidth: Float,
        prefix: String = ""
    ): List<RenderLine> {
        if (fullText.isBlank()) return emptyList()

        paint.textSize = textSize
        paint.typeface = if (isBold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT

        val words = fullText.trim().split(Regex("\\s+"))
        val wrapped = mutableListOf<String>()
        var currentLine = ""

        for (word in words) {
            val candidate = if (currentLine.isEmpty()) {
                if (wrapped.isEmpty() && prefix.isNotBlank()) "$prefix$word" else word
            } else {
                "$currentLine $word"
            }

            if (paint.measureText(candidate) <= maxLineWidth) {
                currentLine = candidate
            } else {
                if (currentLine.isNotEmpty()) {
                    wrapped.add(currentLine)
                }
                // Indent continuation lines if there is a prefix
                val indent = if (prefix.isNotBlank()) "   " else ""
                currentLine = "$indent$word"

                // In case a single word is longer than maxLineWidth, safely chunk it
                while (paint.measureText(currentLine) > maxLineWidth && currentLine.length > 6) {
                    var splitIdx = currentLine.length - 1
                    while (splitIdx > 1 && paint.measureText(currentLine.substring(0, splitIdx)) > maxLineWidth) {
                        splitIdx--
                    }
                    wrapped.add(currentLine.substring(0, splitIdx))
                    currentLine = "$indent" + currentLine.substring(splitIdx)
                }
            }
        }

        if (currentLine.isNotEmpty()) {
            wrapped.add(currentLine)
        }

        return wrapped.map { text ->
            RenderLine(text = text, textSize = textSize, color = color, isBold = isBold)
        }
    }

    private data class RenderLine(
        val text: String,
        val textSize: Float,
        val color: Int,
        val isBold: Boolean = false
    )
}
