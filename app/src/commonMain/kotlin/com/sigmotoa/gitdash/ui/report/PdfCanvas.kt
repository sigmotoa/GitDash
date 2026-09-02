package com.sigmotoa.gitdash.ui.report

enum class PdfAlign { LEFT, CENTER, RIGHT }

/**
 * Primitivas de dibujo mínimas para generar los PDF de informes. El sistema de
 * coordenadas es el mismo que el de Android/UIKit: origen arriba-izquierda, `y`
 * hacia abajo, en puntos (1 pt = 1/72"). Para el texto, `y` es la línea base.
 */
interface PdfCanvas {
    fun fillRect(x: Float, y: Float, w: Float, h: Float, color: Long)
    fun line(x1: Float, y1: Float, x2: Float, y2: Float, color: Long, width: Float)
    fun text(
        s: String,
        x: Float,
        baseline: Float,
        size: Float,
        color: Long,
        bold: Boolean = false,
        align: PdfAlign = PdfAlign.LEFT,
    )

    /** Dibuja [png] recortada a un círculo de [radius] centrado en ([cx], [cy]). */
    fun circleImage(png: ByteArray, cx: Float, cy: Float, radius: Float)
}

/**
 * Dibuja una página de tamaño [widthPt] x [heightPt] y devuelve el PDF (una sola
 * página) como bytes. Android usa `android.graphics.pdf.PdfDocument`; iOS usa
 * `UIGraphicsPDFRenderer`.
 */
expect fun renderSinglePagePdf(
    widthPt: Float,
    heightPt: Float,
    block: PdfCanvas.() -> Unit,
): ByteArray
