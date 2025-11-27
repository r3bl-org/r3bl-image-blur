// Copyright (c) 2025 R3BL LLC. Licensed under Apache License, Version 2.0.

package com.r3bl.imageblur

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log

/**
 * Pure image processing functions for blur and darken effects.
 */
object ImageProcessor {

    /**
     * Apply gaussian blur to a bitmap using stack blur algorithm.
     * Scale down -> blur -> scale up for frosted glass effect.
     */
    fun blur(bitmap: Bitmap): Bitmap {
        Log.d(Config.LOG_TAG, "ImageProcessor.blur() started")
        Log.d(Config.LOG_TAG, "  Input: ${bitmap.width}x${bitmap.height}, config=${bitmap.config}")
        Log.d(Config.LOG_TAG, "  Config: SCALE_FACTOR=${Config.SCALE_FACTOR}, BLUR_RADIUS=${Config.BLUR_RADIUS}")

        val scaleFactor = Config.SCALE_FACTOR
        val scaledWidth = (bitmap.width * scaleFactor).toInt().coerceAtLeast(1)
        val scaledHeight = (bitmap.height * scaleFactor).toInt().coerceAtLeast(1)
        Log.d(Config.LOG_TAG, "  Scaled dimensions: ${scaledWidth}x${scaledHeight}")

        // Scale down for performance and extra blur
        Log.d(Config.LOG_TAG, "  Scaling down...")
        val scaleDownStart = System.currentTimeMillis()
        val scaled = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
        val scaleDownTime = System.currentTimeMillis() - scaleDownStart
        Log.d(Config.LOG_TAG, "  Scale down completed in ${scaleDownTime}ms")
        Log.d(Config.LOG_TAG, "  Scaled bitmap: ${scaled.width}x${scaled.height}, config=${scaled.config}")

        // Apply stack blur
        val blurRadius = Config.BLUR_RADIUS.toInt().coerceIn(1, 25)
        Log.d(Config.LOG_TAG, "  Applying stack blur with radius=$blurRadius...")
        val stackBlurStart = System.currentTimeMillis()
        val blurred = stackBlur(scaled, blurRadius)
        val stackBlurTime = System.currentTimeMillis() - stackBlurStart
        Log.d(Config.LOG_TAG, "  Stack blur completed in ${stackBlurTime}ms")
        Log.d(Config.LOG_TAG, "  Blurred bitmap: ${blurred.width}x${blurred.height}, config=${blurred.config}")

        // Scale back to original size (this adds to the blur effect)
        Log.d(Config.LOG_TAG, "  Scaling up to original size...")
        val scaleUpStart = System.currentTimeMillis()
        val result = Bitmap.createScaledBitmap(blurred, bitmap.width, bitmap.height, true)
        val scaleUpTime = System.currentTimeMillis() - scaleUpStart
        Log.d(Config.LOG_TAG, "  Scale up completed in ${scaleUpTime}ms")
        Log.d(Config.LOG_TAG, "  Final bitmap: ${result.width}x${result.height}, config=${result.config}")
        Log.d(Config.LOG_TAG, "ImageProcessor.blur() completed (scaleDown=${scaleDownTime}ms, stackBlur=${stackBlurTime}ms, scaleUp=${scaleUpTime}ms)")

        return result
    }

    /**
     * Stack blur algorithm - fast and works in any context.
     */
    private fun stackBlur(bitmap: Bitmap, radius: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val pix = IntArray(w * h)
        bitmap.getPixels(pix, 0, w, 0, 0, w, h)

        val wm = w - 1
        val hm = h - 1
        val wh = w * h
        val div = radius + radius + 1

        val r = IntArray(wh)
        val g = IntArray(wh)
        val b = IntArray(wh)
        var rsum: Int
        var gsum: Int
        var bsum: Int
        var x: Int
        var y: Int
        var i: Int
        var p: Int
        var yp: Int
        var yi: Int
        var yw: Int
        val vmin = IntArray(w.coerceAtLeast(h))

        var divsum = (div + 1) shr 1
        divsum *= divsum
        val dv = IntArray(256 * divsum)
        i = 0
        while (i < 256 * divsum) {
            dv[i] = i / divsum
            i++
        }

        yi = 0
        yw = 0

        val stack = Array(div) { IntArray(3) }
        var stackpointer: Int
        var stackstart: Int
        var sir: IntArray
        var rbs: Int
        val r1 = radius + 1
        var routsum: Int
        var goutsum: Int
        var boutsum: Int
        var rinsum: Int
        var ginsum: Int
        var binsum: Int

        y = 0
        while (y < h) {
            bsum = 0
            gsum = 0
            rsum = 0
            boutsum = 0
            goutsum = 0
            routsum = 0
            binsum = 0
            ginsum = 0
            rinsum = 0
            i = -radius
            while (i <= radius) {
                p = pix[yi + (i.coerceIn(0, wm))]
                sir = stack[i + radius]
                sir[0] = (p and 0xff0000) shr 16
                sir[1] = (p and 0x00ff00) shr 8
                sir[2] = p and 0x0000ff
                rbs = r1 - kotlin.math.abs(i)
                rsum += sir[0] * rbs
                gsum += sir[1] * rbs
                bsum += sir[2] * rbs
                if (i > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                }
                i++
            }
            stackpointer = radius

            x = 0
            while (x < w) {
                r[yi] = dv[rsum]
                g[yi] = dv[gsum]
                b[yi] = dv[bsum]

                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum

                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]

                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]

                if (y == 0) {
                    vmin[x] = (x + radius + 1).coerceAtMost(wm)
                }
                p = pix[yw + vmin[x]]

                sir[0] = (p and 0xff0000) shr 16
                sir[1] = (p and 0x00ff00) shr 8
                sir[2] = p and 0x0000ff

                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]

                rsum += rinsum
                gsum += ginsum
                bsum += binsum

                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer % div]

                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]

                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]

                yi++
                x++
            }
            yw += w
            y++
        }

        x = 0
        while (x < w) {
            bsum = 0
            gsum = 0
            rsum = 0
            boutsum = 0
            goutsum = 0
            routsum = 0
            binsum = 0
            ginsum = 0
            rinsum = 0
            yp = -radius * w
            i = -radius
            while (i <= radius) {
                yi = (yp + x).coerceAtLeast(0)
                sir = stack[i + radius]
                sir[0] = r[yi]
                sir[1] = g[yi]
                sir[2] = b[yi]
                rbs = r1 - kotlin.math.abs(i)
                rsum += r[yi] * rbs
                gsum += g[yi] * rbs
                bsum += b[yi] * rbs
                if (i > 0) {
                    rinsum += sir[0]
                    ginsum += sir[1]
                    binsum += sir[2]
                } else {
                    routsum += sir[0]
                    goutsum += sir[1]
                    boutsum += sir[2]
                }
                if (i < hm) {
                    yp += w
                }
                i++
            }
            yi = x
            stackpointer = radius

            y = 0
            while (y < h) {
                pix[yi] = (0xff000000.toInt() and pix[yi]) or (dv[rsum] shl 16) or (dv[gsum] shl 8) or dv[bsum]

                rsum -= routsum
                gsum -= goutsum
                bsum -= boutsum

                stackstart = stackpointer - radius + div
                sir = stack[stackstart % div]

                routsum -= sir[0]
                goutsum -= sir[1]
                boutsum -= sir[2]

                if (x == 0) {
                    vmin[y] = (y + r1).coerceAtMost(hm) * w
                }
                p = x + vmin[y]

                sir[0] = r[p]
                sir[1] = g[p]
                sir[2] = b[p]

                rinsum += sir[0]
                ginsum += sir[1]
                binsum += sir[2]

                rsum += rinsum
                gsum += ginsum
                bsum += binsum

                stackpointer = (stackpointer + 1) % div
                sir = stack[stackpointer]

                routsum += sir[0]
                goutsum += sir[1]
                boutsum += sir[2]

                rinsum -= sir[0]
                ginsum -= sir[1]
                binsum -= sir[2]

                yi += w
                y++
            }
            x++
        }

        val result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        result.setPixels(pix, 0, w, 0, 0, w, h)
        return result
    }

    /**
     * Apply darkening overlay to a bitmap.
     * Draws a semi-transparent black layer over the image.
     */
    fun darken(bitmap: Bitmap): Bitmap {
        Log.d(Config.LOG_TAG, "ImageProcessor.darken() started")
        Log.d(Config.LOG_TAG, "  Input: ${bitmap.width}x${bitmap.height}, config=${bitmap.config}")
        Log.d(Config.LOG_TAG, "  Config: DARKEN_ALPHA=${Config.DARKEN_ALPHA}")

        val alphaValue = (Config.DARKEN_ALPHA * 255).toInt()
        Log.d(Config.LOG_TAG, "  Computed alpha value: $alphaValue (0-255)")

        Log.d(Config.LOG_TAG, "  Copying bitmap...")
        val copyStart = System.currentTimeMillis()
        val output = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val copyTime = System.currentTimeMillis() - copyStart
        Log.d(Config.LOG_TAG, "  Bitmap copy completed in ${copyTime}ms")

        Log.d(Config.LOG_TAG, "  Drawing overlay...")
        val drawStart = System.currentTimeMillis()
        val canvas = Canvas(output)
        val paint = Paint().apply {
            color = Color.argb(alphaValue, 0, 0, 0)
        }
        canvas.drawRect(0f, 0f, output.width.toFloat(), output.height.toFloat(), paint)
        val drawTime = System.currentTimeMillis() - drawStart
        Log.d(Config.LOG_TAG, "  Overlay draw completed in ${drawTime}ms")

        Log.d(Config.LOG_TAG, "  Output: ${output.width}x${output.height}, config=${output.config}")
        Log.d(Config.LOG_TAG, "ImageProcessor.darken() completed (copy=${copyTime}ms, draw=${drawTime}ms)")
        return output
    }
}
