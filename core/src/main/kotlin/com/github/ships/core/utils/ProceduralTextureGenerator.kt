package com.github.ships.core.utils

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils
import java.util.*

object ProceduralTextureGenerator {
    private val random = Random()

    fun createShipPixmap(width: Int, height: Int, color: Color, level: Int): Pixmap {
        val pixmap = Pixmap(width, height, Pixmap.Format.RGBA8888)
        pixmap.setColor(0f, 0f, 0f, 0f)
        pixmap.fill()
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
        return pixmap
    }

    fun createShipTexture(width: Int, height: Int, color: Color, level: Int): Texture {
        val pixmap = createShipPixmap(width, height, color, level)
        val texture = Texture(pixmap)
        pixmap.dispose()
        return texture
    }

    fun createWhitePixel(): TextureRegion {
        val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
        pixmap.setColor(Color.WHITE)
        pixmap.fill()
        val texture = Texture(pixmap)
        pixmap.dispose()
        return TextureRegion(texture)
    }

    fun createCircleTexture(radius: Int, color: Color): Texture {
        val pixmap = Pixmap(radius * 2, radius * 2, Pixmap.Format.RGBA8888)
        pixmap.setColor(color)
        pixmap.fillCircle(radius, radius, radius)
        val texture = Texture(pixmap)
        pixmap.dispose()
        return texture
    }

    fun createRectangleTexture(width: Int, height: Int, color: Color): Texture {
        val pixmap = Pixmap(width, height, Pixmap.Format.RGBA8888)
        val innerColor = color.cpy()
        val outerColor = color.cpy().mul(0.5f)
        pixmap.setColor(outerColor)
        pixmap.fill()
        pixmap.setColor(innerColor)
        pixmap.fillRectangle(width / 4, 0, width / 2, height)
        pixmap.setColor(Color.WHITE)
        pixmap.drawLine(width / 2, 0, width / 2, height)
        val texture = Texture(pixmap)
        pixmap.dispose()
        return texture
    }

    private fun downsample(source: Pixmap, scale: Int): Pixmap {
        val result = Pixmap(source.width, source.height, source.format)
        for (y in 0 until source.height step scale) {
            for (x in 0 until source.width step scale) {
                val color = source.getPixel(x, y)
                for (dy in 0 until scale) {
                    for (dx in 0 until scale) {
                        if (x + dx < source.width && y + dy < source.height) {
                            result.drawPixel(x + dx, y + dy, color)
                        }
                    }
                }
            }
        }
        return result
    }

    fun createSciFiJoystickBase(size: Int): Texture {
        // Draw at full res, then pixelate
        val temp = Pixmap(size, size, Pixmap.Format.RGBA8888)
        val center = size / 2
        val radius = size / 2 - 2

        // Background ring gradient
        for (r in radius downTo 0 step 4) {
            val grad = r.toFloat() / radius
            temp.setColor(0.1f, 0.2f * grad, 0.3f * grad, 0.5f)
            temp.fillCircle(center, center, r)
        }

        temp.setColor(0.4f, 0.6f, 1f, 0.8f)
        temp.drawCircle(center, center, radius)
        temp.drawCircle(center, center, radius - 4)

        // Technical crosshairs
        temp.drawLine(center - 20, center, center + 20, center)
        temp.drawLine(center, center - 20, center, center + 20)

        val pixelated = downsample(temp, 8) // High pixelation factor
        val texture = Texture(pixelated)
        temp.dispose()
        pixelated.dispose()
        return texture
    }

    fun createSciFiJoystickKnob(size: Int): Texture {
        val temp = Pixmap(size, size, Pixmap.Format.RGBA8888)
        val center = size / 2
        val radius = size / 2 - 2

        // Knob body with gradient
        for (r in radius downTo 0 step 2) {
            val grad = 1f - (r.toFloat() / radius)
            temp.setColor(0.3f + 0.2f * grad, 0.3f + 0.2f * grad, 0.4f + 0.4f * grad, 0.9f)
            temp.fillCircle(center, center, r)
        }

        temp.setColor(0.6f, 0.8f, 1f, 1f)
        temp.drawCircle(center, center, radius)

        val pixelated = downsample(temp, 4)
        val texture = Texture(pixelated)
        temp.dispose()
        pixelated.dispose()
        return texture
    }

    fun createSciFiButton(width: Int, height: Int, down: Boolean): Texture {
        val temp = Pixmap(width, height, Pixmap.Format.RGBA8888)
        val baseColor = if (down) Color(0.1f, 0.3f, 0.5f, 0.7f) else Color(0.05f, 0.1f, 0.2f, 0.5f)

        temp.setColor(baseColor)
        temp.fill()

        // Technical outline
        temp.setColor(0.4f, 0.7f, 1f, 0.9f)
        temp.drawRectangle(0, 0, width, height)
        temp.drawRectangle(4, 4, width - 8, height - 8)

        val pixelated = downsample(temp, 6)
        val texture = Texture(pixelated)
        temp.dispose()
        pixelated.dispose()
        return texture
    }
}
