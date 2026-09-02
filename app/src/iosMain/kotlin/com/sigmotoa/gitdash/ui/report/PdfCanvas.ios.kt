@file:OptIn(ExperimentalForeignApi::class)

package com.sigmotoa.gitdash.ui.report

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.CGContextAddEllipseInRect
import platform.CoreGraphics.CGContextAddLineToPoint
import platform.CoreGraphics.CGContextClip
import platform.CoreGraphics.CGContextFillRect
import platform.CoreGraphics.CGContextMoveToPoint
import platform.CoreGraphics.CGContextRef
import platform.CoreGraphics.CGContextRestoreGState
import platform.CoreGraphics.CGContextSaveGState
import platform.CoreGraphics.CGContextSetFillColorWithColor
import platform.CoreGraphics.CGContextSetLineWidth
import platform.CoreGraphics.CGContextSetStrokeColorWithColor
import platform.CoreGraphics.CGContextStrokePath
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.create
import platform.UIKit.NSFontAttributeName
import platform.UIKit.NSForegroundColorAttributeName
import platform.UIKit.UIColor
import platform.UIKit.UIFont
import platform.UIKit.UIGraphicsPDFRenderer
import platform.UIKit.UIGraphicsPDFRendererFormat
import platform.UIKit.UIImage
import platform.UIKit.drawAtPoint
import platform.UIKit.drawInRect
import platform.UIKit.sizeWithAttributes
import platform.posix.memcpy

actual fun renderSinglePagePdf(
    widthPt: Float,
    heightPt: Float,
    block: PdfCanvas.() -> Unit,
): ByteArray {
    val bounds = CGRectMake(0.0, 0.0, widthPt.toDouble(), heightPt.toDouble())
    val renderer = UIGraphicsPDFRenderer(bounds = bounds, format = UIGraphicsPDFRendererFormat())

    val data: NSData = renderer.PDFDataWithActions { rendererContext ->
        rendererContext?.beginPage()
        val cg = rendererContext?.CGContext ?: return@PDFDataWithActions
        IosPdfCanvas(cg).block()
    }
    return data.toByteArray()
}

private class IosPdfCanvas(private val ctx: CGContextRef) : PdfCanvas {

    override fun fillRect(x: Float, y: Float, w: Float, h: Float, color: Long) {
        CGContextSetFillColorWithColor(ctx, uiColor(color).CGColor)
        CGContextFillRect(ctx, CGRectMake(x.toDouble(), y.toDouble(), w.toDouble(), h.toDouble()))
    }

    override fun line(x1: Float, y1: Float, x2: Float, y2: Float, color: Long, width: Float) {
        CGContextSetStrokeColorWithColor(ctx, uiColor(color).CGColor)
        CGContextSetLineWidth(ctx, width.toDouble())
        CGContextMoveToPoint(ctx, x1.toDouble(), y1.toDouble())
        CGContextAddLineToPoint(ctx, x2.toDouble(), y2.toDouble())
        CGContextStrokePath(ctx)
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
        val font = if (bold) {
            UIFont.boldSystemFontOfSize(size.toDouble())
        } else {
            UIFont.systemFontOfSize(size.toDouble())
        }
        val attrs = mapOf<Any?, Any>(
            NSFontAttributeName to font,
            NSForegroundColorAttributeName to uiColor(color),
        )
        val ns = NSString.create(string = s)
        val textWidth = ns.sizeWithAttributes(attrs).useContents { this.width }
        val drawX = when (align) {
            PdfAlign.LEFT -> x.toDouble()
            PdfAlign.CENTER -> x - textWidth / 2.0
            PdfAlign.RIGHT -> x - textWidth
        }
        // Android's drawText places `y` at the baseline; drawAtPoint places it at the top.
        val topY = baseline.toDouble() - font.ascender
        ns.drawAtPoint(CGPointMake(drawX, topY), withAttributes = attrs)
    }

    override fun circleImage(png: ByteArray, cx: Float, cy: Float, radius: Float) {
        val image = UIImage(data = png.toNSData()) ?: return
        val rect = CGRectMake(
            (cx - radius).toDouble(),
            (cy - radius).toDouble(),
            (radius * 2).toDouble(),
            (radius * 2).toDouble(),
        )
        CGContextSaveGState(ctx)
        CGContextAddEllipseInRect(ctx, rect)
        CGContextClip(ctx)
        image.drawInRect(rect)
        CGContextRestoreGState(ctx)
    }
}

private fun uiColor(argb: Long): UIColor {
    val a = ((argb shr 24) and 0xFF).toDouble() / 255.0
    val r = ((argb shr 16) and 0xFF).toDouble() / 255.0
    val g = ((argb shr 8) and 0xFF).toDouble() / 255.0
    val b = (argb and 0xFF).toDouble() / 255.0
    return UIColor.colorWithRed(red = r, green = g, blue = b, alpha = a)
}

private fun NSData.toByteArray(): ByteArray {
    val len = length.toInt()
    if (len == 0) return ByteArray(0)
    val out = ByteArray(len)
    out.usePinned { pinned -> memcpy(pinned.addressOf(0), bytes, length) }
    return out
}

private fun ByteArray.toNSData(): NSData {
    if (isEmpty()) return NSData()
    return usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
    }
}
