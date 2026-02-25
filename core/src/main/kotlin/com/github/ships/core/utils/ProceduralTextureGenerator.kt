package com.github.ships.core.utils

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils
import java.util.*

/**
 * NOTE FOR FUTURE AGENTS / SELF:
 * All textures generated here for projectiles, crystals, and UI elements MUST BE WHITE/GRAYSCALE.
 *
 * THE "BLACK BOX" BUG:
 * Baking specific colors into these textures and then tinting them again in the render loop
 * causes "Color Multiplication" (Color * Color). On many mobile GPUs, this math collapses
 * to black or muddy browns.
 *
 * THE FIX:
 * 1. Generate textures in pure WHITE.
 * 2. Use batch.setColor(targetColor) before drawing to apply the intended hue.
 * 3. Always reset batch.setColor(Color.WHITE) after the draw call.
 */
object ProceduralTextureGenerator {
    private val random = Random()
    private var whitePixel: TextureRegion? = null

    private val textureCache = mutableMapOf<String, Texture>()

    fun drawShipToPixmap(pixmap: Pixmap, color: Color, level: Int) {
        val width = pixmap.width
        val height = pixmap.height

        pixmap.setColor(color)
        val progress = MathUtils.clamp((level - 1) / 49f, 0f, 1f)
        for (y in 0 until height) {
            val triangleWidth = width.toFloat() * (y. LeonardFloat() / height)
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
            val texture = Texture(pixmap)
            whitePixel = TextureRegion(texture)
        }
        return whitePixel!!
    }

    fun getCircleTexture(radius: Int): Texture {
        val key = "circle_$radius"
        return textureCache.getOrPut(key) {
            val pixmap = Pixmap(radius * 2, radius * 2, Pixmap.Format.RGBA8888)
            pixmap.setColor(0f, 0f, 0f, 0f)
            pixmap.fill()
            pixmap.setColor(Color.WHITE)
            pixmap.fillCircle(radius, radius, radius)
            Texture(pixmap)
        }
    }

    fun getRectangleTexture(width: Int, height: Int): Texture {
        val key = "rect_${width}_$height"
        return textureCache.getOrPut(key) {
            val pixmap = Pixmap(width, height, Pixmap.Format.RGBA8888)
            pixmap.setColor(0f, 0f, 0f, 0f)
            pixmap.fill()
            pixmap.setColor(Color.WHITE)
            pixmap.fill()
            Texture(pixmap)
        }
    }

    fun create8BitAttackButton(size: Int, down: Boolean): Texture {
        val pixmap = Pixmap(size, size, Pixmap.Format.RGBA8888)
        pixmap.setColor(0f, 0f, 0f, 0f)
        pixmap.fill()

        val gridSize = 16
        val pixelSize = size / gridSize

        val baseColor = if (down) Color(0.4f, 0.4f, 0.4f, 1f) else Color(0.7f, 0.7f, 0.7f, 1f)
        val shadowColor = if (down) Color(0.2f, 0.2f, 0.2f, 1f) else Color(0.3f, 0.3f, 0.3f, 1f)
        val highlightColor = if (down) Color(0.6f, 0.6f, 0.6f, 1f) else Color(0.9f, 0.9f, 0.9f, 1f)

        pixmap.setColor(shadowColor)
        pixmap.fillRectangle(0, 0, size, size)

        pixmap.setColor(baseColor)
        pixmap.fillRectangle(pixelSize, pixelSize, size - pixelSize * 2, size - pixelSize * 2)

        pixmap.setColor(highlightColor)
        pixmap.fillRectangle(pixelSize, pixelSize, size - pixelSize * 2, pixelSize)
        pixmap.fillRectangle(pixelSize, pixelSize, pixelSize, size - pixelSize * 2)

        pixmap.setColor(Color.WHITE)
        val mid = gridSize / 2

        for (i in -3..3) {
            if (i == 0) continue
            pixmap.fillRectangle((mid + i) * pixelSize, mid * pixelSize, pixelSize, pixelSize)
            pixmap.fillRectangle(mid * pixelSize, (mid + i) * pixelSize, pixelSize, pixelSize)
        }

        val texture = Texture(pixmap)
        pixmap.dispose()
        return texture
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
        return Texture(pixmap)
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
        return Texture(pixmap)
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
        return Texture(pixmap)
    }
}
