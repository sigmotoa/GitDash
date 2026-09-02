package com.sigmotoa.gitdash.ui.report

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.pdf.PdfDocument
import java.io.ByteArrayOutputStream

actual fun renderSinglePagePdf(
    widthPt: Float,
    heightPt: Float,
    block: PdfCanvas.() -> Unit,
): ByteArray {
    val pdf = PdfDocument()
    val page = pdf.startPage(
        PdfDocument.PageInfo.Builder(widthPt.toInt(), heightPt.toInt(), 1).create(),
    )
    AndroidPdfCanvas(page.canvas).block()
    pdf.finishPage(page)

    return ByteArrayOutputStream().use { out ->
        pdf.writeTo(out)
        out.toByteArray()
    }.also { pdf.close() }
}

private class AndroidPdfCanvas(private val canvas: Canvas) : PdfCanvas {

    override fun fillRect(x: Float, y: Float, w: Float, h: Float, color: Long) {
        canvas.drawRect(RectF(x, y, x + w, y + h), paint(color).apply { style = Paint.Style.FILL })
    }

    override fun line(x1: Float, y1: Float, x2: Float, y2: Float, color: Long, width: Float) {
        canvas.drawLine(
            x1, y1, x2, y2,
            paint(color).apply { style = Paint.Style.STROKE; strokeWidth = width },
        )
    }

    override fun text(
        s: String,
        x: Float,
        baseline: Float,
        size: Float,
        color: Long,
        bold: Boolean,
        align: PdfAlign,
    ) {
        canvas.drawText(
            s, x, baseline,
            paint(color).apply {
                textSize = size
                isFakeBoldText = bold
                textAlign = when (align) {
                    PdfAlign.LEFT -> Paint.Align.LEFT
                    PdfAlign.CENTER -> Paint.Align.CENTER
                    PdfAlign.RIGHT -> Paint.Align.RIGHT
                }
            },
        )
    }

    override fun circleImage(png: ByteArray, cx: Float, cy: Float, radius: Float) {
        val src = BitmapFactory.decodeByteArray(png, 0, png.size) ?: return
        val diameter = (radius * 2).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(src, diameter, diameter, true)
        val shader = BitmapShader(scaled, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
            setLocalMatrix(Matrix().apply { setTranslate(cx - radius, cy - radius) })
        }
        canvas.drawCircle(cx, cy, radius, Paint(Paint.ANTI_ALIAS_FLAG).apply { this.shader = shader })
        if (scaled !== src) scaled.recycle()
        src.recycle()
    }

    private fun paint(color: Long) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color.toInt()
    }
}
