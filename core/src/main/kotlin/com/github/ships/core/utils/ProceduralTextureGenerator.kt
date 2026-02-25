package com.github.ships.core.utils

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils
import java.util.*

object ProceduralTextureGenerator {
    private val random = Random()
    private var whitePixel: TextureRegion? = null
    private val circleCache = mutableMapOf<Pair<Int, Int>, Texture>()
    private val rectCache = mutableMapOf<Triple<Int, Int, Int>, Texture>()

    fun drawShipToPixmap(pixmap: Pixmap, color: Color, level: Int) {
        val width = pixmap.width
        val height = pixmap.height

        pixmap.setColor(color)
        val progress = MathUtils.clamp((level - 1) / 49f, 0f, 1f)
        for (y in 0 until height) {
            val triangleWidth = width.toFloat() * (y.toFloat() / height)
            val rectWidth = width.toFloat() * 0.9f
            val currentWidth = MathUtils.lerp(triangleWidth, rectWidth, progress).toInt()
            val startX = (width - currentWidth) / 2
            for (x in startX until startX + currentWidth) {
                if (random.nextFloat() > 0.05f) {
                    pixmap.drawPixel(x, y)
                }
            }
        }
        pixmap.setColor(Color.SKY)
        val cockpitW = (width * 0.2f).toInt()
        val cockpitH = (height * 0.15f).toInt()
        pixmap.fillRectangle((width - cockpitW) / 2, (height * 0.7f).toInt(), cockpitW, cockpitH)
    }

    fun createShipPixmap(width: Int, height: Int, color: Color, level: Int): Pixmap {
        val pixmap = Pixmap(width, height, Pixmap.Format.RGBA8888)
        pixmap.setColor(0f, 0f, 0f, 0f)
        pixmap.fill()
        drawShipToPixmap(pixmap, color, level)
        return pixmap
    }

    fun createWhitePixel(): TextureRegion {
        if (whitePixel == null) {
            val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
            pixmap.setColor(Color.WHITE)
            pixmap.fill()
            whitePixel = TextureRegion(Texture(pixmap))
        }
        return whitePixel!!
    }

    fun getCircleTexture(radius: Int, color: Color): Texture {
        val key = radius to Color.rgba8888(color)
        return circleCache.getOrPut(key) {
            val pixmap = Pixmap(radius * 2, radius * 2, Pixmap.Format.RGBA8888)
            pixmap.setColor(0f, 0f, 0f, 0f)
            pixmap.fill()
            pixmap.setColor(color)
            pixmap.fillCircle(radius, radius, radius)
            Texture(pixmap)
        }
    }

    fun getRectangleTexture(width: Int, height: Int, color: Color): Texture {
        val key = Triple(width, height, Color.rgba8888(color))
        return rectCache.getOrPut(key) {
            val pixmap = Pixmap(width, height, Pixmap.Format.RGBA8888)
            pixmap.setColor(0f, 0f, 0f, 0f)
            pixmap.fill()

            val innerColor = color.cpy()
            val outerColor = color.cpy().mul(0.5f)
            pixmap.setColor(outerColor)
            pixmap.fill()
            pixmap.setColor(innerColor)
            pixmap.fillRectangle(width / 4, 0, width / 2, height)
            pixmap.setColor(Color.WHITE)
            pixmap.drawLine(width / 2, 0, width / 2, height)
            Texture(pixmap)
        }
    }

    fun createSciFiJoystickBase(size: Int): Texture {
        val pixmap = Pixmap(size, size, Pixmap.Format.RGBA8888)
        pixmap.setColor(0f, 0f, 0f, 0f)
        pixmap.fill()
        val center = size / 2
        val radius = size / 2 - 2
        for (r in radius downTo 0 step 4) {
            val grad = r.toFloat() / radius
            pixmap.setColor(0.1f, 0.2f * grad, 0.3f * grad, 0.5f)
            pixmap.fillCircle(center, center, r)
        }
        pixmap.setColor(0.4f, 0.6f, 1f, 0.8f)
        pixmap.drawCircle(center, center, radius)
        pixmap.drawCircle(center, center, radius - 4)
        pixmap.drawLine(center - 20, center, center + 20, center)
        pixmap.drawLine(center, center - 20, center, center + 20)
        val texture = Texture(pixmap)
        pixmap.dispose()
        return texture
    }

    fun createSciFiJoystickKnob(size: Int): Texture {
        val pixmap = Pixmap(size, size, Pixmap.Format.RGBA8888)
        pixmap.setColor(0f, 0f, 0f, 0f)
        pixmap.fill()
        val center = size / 2
        val radius = size / 2 - 2
        for (r in radius downTo 0 step 2) {
            val grad = 1f - (r.toFloat() / radius)
            pixmap.setColor(0.3f + 0.2f * grad, 0.3f + 0.2f * grad, 0.4f + 0.4f * grad, 0.9f)
            pixmap.fillCircle(center, center, r)
        }
        pixmap.setColor(0.6f, 0.8f, 1f, 1f)
        pixmap.drawCircle(center, center, radius)
        val texture = Texture(pixmap)
        pixmap.dispose()
        return texture
    }

    fun createSciFiButton(width: Int, height: Int, down: Boolean): Texture {
        val pixmap = Pixmap(width, height, Pixmap.Format.RGBA8888)
        pixmap.setColor(0f, 0f, 0f, 0f)
        pixmap.fill()
        val baseColor = if (down) Color(0.1f, 0.3f, 0.5f, 0.7f) else Color(0.05f, 0.1f, 0.2f, 0.5f)
        pixmap.setColor(baseColor)
        pixmap.fill()
        pixmap.setColor(0.4f, 0.7f, 1f, 0.9f)
        pixmap.drawRectangle(0, 0, width, height)
        pixmap.drawRectangle(4, 4, width - 8, height - 8)
        val texture = Texture(pixmap)
        pixmap.dispose()
        return texture
    }
}
