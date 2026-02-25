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

    private val textureCache = mutableMapOf<String, Texture>()

    private val evolutionColors = arrayOf(
        Color(0.0f, 1.0f, 1.0f, 1f),
        Color(1.0f, 1.0f, 0.0f, 1f),
        Color(1.0f, 0.0f, 0.0f, 1f),
        Color(0.0f, 1.0f, 0.0f, 1f),
        Color(1.0f, 1.0f, 1.0f, 1f)
    )

    fun getEvolutionColor(level: Int): Color {
        val baseColor = evolutionColors[(level - 1) % evolutionColors.size].cpy()
        // 30% brighter and less saturated (desaturated)
        // Increased desaturation and brightness per request
        return baseColor.lerp(Color.GRAY, 0.7f).lerp(Color.WHITE, 0.5f)
    }

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

        // Draw windows based on evolution level
        val windowColor = getEvolutionColor(level)
        pixmap.setColor(windowColor)

        // Cockpit (always present at top center)
        val cockpitW = (width * 0.2f).toInt().coerceAtLeast(4)
        val cockpitH = (height * 0.12f).toInt().coerceAtLeast(4)
        val cockpitY = (height * 0.75f).toInt()
        pixmap.fillRectangle((width - cockpitW) / 2, cockpitY, cockpitW, cockpitH)

        // Calculate multiple arrays of lined up windows to represent level exactly
        if (level > 1) {
            val N = level - 1
            // Base window size
            val baseWinSize = (width * 0.05f).toInt().coerceAtLeast(2)
            // Constraint: gap >= size. Step = size + gap = 2 * size.
            val step = 2 * baseWinSize

            val potentialPos = mutableListOf<Pair<Int, Int>>()

            // Available vertical range for body windows: from 0.1 to 0.65
            for (winY in (height * 0.1f).toInt()..(height * 0.65f).toInt() step step) {
                val t = winY.toFloat() / height.toFloat()
                val triangleWidth = width.toFloat() * t
                val rectWidth = width.toFloat() * 0.9f
                val hullWidthAtY = MathUtils.lerp(triangleWidth, rectWidth, progress)

                // Add center slot
                potentialPos.add(width / 2 to winY)

                // Add symmetrical pair slots
                var col = 1
                while (true) {
                    val xOffset = col * step
                    // Ensure window stays within hull with some margin
                    if (xOffset + baseWinSize < hullWidthAtY * 0.45f) {
                        potentialPos.add(width / 2 - xOffset to winY)
                        potentialPos.add(width / 2 + xOffset to winY)
                        col++
                    } else {
                        break
                    }
                }
            }

            // Sort to prioritize strips: center strip first, then inner pairs, then outer pairs.
            // This ensures they "line up" in arrays as N increases.
            potentialPos.sortWith(compareBy({ Math.abs(it.first - width / 2) }, { it.second }))

            if (potentialPos.isNotEmpty()) {
                if (N <= potentialPos.size) {
                    // Spread N windows across potential slots
                    for (i in 0 until N) {
                        val idx = (i * potentialPos.size / N)
                        val pos = potentialPos[idx]
                        pixmap.fillRectangle(pos.first - baseWinSize / 2, pos.second, baseWinSize, baseWinSize)
                    }
                } else {
                    // Clump together to form larger windows if no more room
                    val areaScale = Math.sqrt(N.toDouble() / potentialPos.size.toDouble())
                    val currentWinSize = (baseWinSize * areaScale).toInt().coerceAtMost(step - 1)

                    for (pos in potentialPos) {
                        pixmap.fillRectangle(pos.first - currentWinSize / 2, pos.second, currentWinSize, currentWinSize)
                    }
                }
            }
        }
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

    fun create8BitChargeButton(size: Int, chargeRatio: Float): Texture {
        val pixmap = Pixmap(size, size, Pixmap.Format.RGBA8888)
        pixmap.setColor(0f, 0f, 0f, 0f)
        pixmap.fill()

        val gridSize = 16
        val pixelSize = size / gridSize

        // Darker than 50% grey default color, scaling up to be much brighter
        val baseVal = 0.1f + chargeRatio * 0.75f
        val baseColor = Color(baseVal, baseVal, baseVal, 1f)
        val shadowColor = Color(baseVal * 0.5f, baseVal * 0.5f, baseVal * 0.5f, 1f)
        val highlightColor = Color(Math.min(1f, baseVal + 0.4f), Math.min(1f, baseVal + 0.4f), Math.min(1f, baseVal + 0.4f), 1f)

        pixmap.setColor(shadowColor)
        pixmap.fillRectangle(0, 0, size, size)

        pixmap.setColor(baseColor)
        pixmap.fillRectangle(pixelSize, pixelSize, size - pixelSize * 2, size - pixelSize * 2)

        pixmap.setColor(highlightColor)
        pixmap.fillRectangle(pixelSize, pixelSize, size - pixelSize * 2, pixelSize)
        pixmap.fillRectangle(pixelSize, pixelSize, pixelSize, size - pixelSize * 2)

        // Draw little white lights around the border based on chargeRatio
        val numLights = (chargeRatio * 12).toInt()
        val lightColor = Color.WHITE.cpy()

        // Face border coords (excluding corners)
        val lightCoords = arrayOf(
            4 to 0, 8 to 0, 11 to 0,
            16 to 4, 16 to 8, 16 to 11,
            11 to 16, 8 to 16, 4 to 16,
            0 to 11, 0 to 8, 0 to 4
        )

        for (i in 0 until numLights) {
            val coord = lightCoords[i]
            pixmap.setColor(lightColor)
            pixmap.fillRectangle(coord.first * pixelSize, coord.second * pixelSize, pixelSize, pixelSize)
        }

        // Center Bolt Icon
        pixmap.setColor(Color.WHITE)
        val mid = gridSize / 2
        pixmap.fillRectangle((mid - 1) * pixelSize, (mid - 3) * pixelSize, pixelSize * 2, pixelSize)
        pixmap.fillRectangle((mid - 2) * pixelSize, (mid - 2) * pixelSize, pixelSize * 2, pixelSize)
        pixmap.fillRectangle((mid - 3) * pixelSize, (mid - 1) * pixelSize, pixelSize * 4, pixelSize)
        pixmap.fillRectangle((mid - 1) * pixelSize, mid * pixelSize, pixelSize * 4, pixelSize)
        pixmap.fillRectangle(mid * pixelSize, (mid + 1) * pixelSize, pixelSize * 2, pixelSize)
        pixmap.fillRectangle((mid + 1) * pixelSize, (mid + 2) * pixelSize, pixelSize * 2, pixelSize)

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
