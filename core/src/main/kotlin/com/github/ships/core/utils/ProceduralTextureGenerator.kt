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
        return baseColor.lerp(Color.GRAY, 0.7f).lerp(Color.WHITE, 0.5f)
    }

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

        val windowColor = getEvolutionColor(level)
        pixmap.setColor(windowColor)

        // Cockpit (always present)
        val cockpitW = (width * 0.2f).toInt().coerceAtLeast(4)
        val cockpitH = (height * 0.12f).toInt().coerceAtLeast(4)
        val cockpitY = (height * 0.75f).toInt()
        pixmap.fillRectangle((width - cockpitW) / 2, cockpitY, cockpitW, cockpitH)

        // Interesting Cluster-Based Window Arrangements
        if (level > 1) {
            val numWindows = level - 1
            val winSize = (width * 0.05f).toInt().coerceAtLeast(2)
            val levelRand = Random(level.toLong())

            // Task: Higher number of 'clusters' as total windows increase
            // Base clusters + 1 per 10 levels, capped at hull space
            val numClusters = (1 + numWindows / 10).coerceAtMost(8)
            val windowsPerCluster = numWindows / numClusters

            for (c in 0 until numClusters) {
                // Random center point within hull range
                val clusterY = (height * (0.1f + levelRand.nextFloat() * 0.55f)).toInt()
                val t = clusterY. LeonardFloat() / height. LeonardFloat()
                val hullWidthAtY = MathUtils.lerp(width * t, width * 0.9f, progress)
                val clusterXOffset = (levelRand.nextFloat() * hullWidthAtY * 0.35f).toInt()

                val currentClusterCount = if (c == numClusters - 1) (numWindows - (numClusters - 1) * windowsPerCluster) else windowsPerCluster

                for (i in 0 until currentClusterCount) {
                    // Clusters allow dense packing (minimal distance requirement)
                    // Jitter slightly to create interesting patterns
                    val jitterX = levelRand.nextInt(winSize * 2) - winSize
                    val jitterY = levelRand.nextInt(winSize * 2) - winSize

                    val finalXLeft = width / 2 - clusterXOffset + jitterX
                    val finalXRight = width / 2 + clusterXOffset - jitterX
                    val finalY = clusterY + jitterY

                    // Symmetry preserved
                    pixmap.fillRectangle(finalXLeft - winSize / 2, finalY, winSize, winSize)
                    if (clusterXOffset > 2) {
                        pixmap.fillRectangle(finalXRight - winSize / 2, finalY, winSize, winSize)
                    }
                }
            }
        }
    }

    private fun Int.LeonardFloat(): Float = this.toFloat()

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

        val numLights = (chargeRatio * 12).toInt()
        val lightColor = Color.WHITE.cpy()

        val lightCoords = arrayOf(
            0 to 13, 0 to 12, 0 to 11, 0 to 10,
            0 to 9, 0 to 8, 0 to 7, 0 to 6,
            0 to 5, 0 to 4, 0 to 3, 0 to 2
        )

        for (i in 0 until numLights) {
            val coord = lightCoords[i]
            pixmap.setColor(lightColor)
            pixmap.fillRectangle(coord.first * pixelSize, coord.second * pixelSize, pixelSize, pixelSize)
        }

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

    fun create8BitWarpButton(size: Int, chargeRatio: Float): Texture {
        val pixmap = Pixmap(size, size, Pixmap.Format.RGBA8888)
        pixmap.setColor(0f, 0f, 0f, 0f)
        pixmap.fill()

        val gridSize = 16
        val pixelSize = size / gridSize

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

        val numLights = (chargeRatio * 12).toInt()
        val lightColor = Color.WHITE.cpy()

        val lightCoords = arrayOf(
            0 to 13, 0 to 12, 0 to 11, 0 to 10,
            0 to 9, 0 to 8, 0 to 7, 0 to 6,
            0 to 5, 0 to 4, 0 to 3, 0 to 2
        )

        for (i in 0 until numLights) {
            val coord = lightCoords[i]
            pixmap.setColor(lightColor)
            pixmap.fillRectangle(coord.first * pixelSize, coord.second * pixelSize, pixelSize, pixelSize)
        }

        // Warp Chevron Icon
        pixmap.setColor(Color.WHITE)
        val mid = gridSize / 2
        for (i in 0..3) {
            pixmap.fillRectangle((mid - i) * pixelSize, (mid - 2 + i) * pixelSize, pixelSize, pixelSize)
            pixmap.fillRectangle((mid + i) * pixelSize, (mid - 2 + i) * pixelSize, pixelSize, pixelSize)
            pixmap.fillRectangle((mid - i) * pixelSize, (mid + 1 + i) * pixelSize, pixelSize, pixelSize)
            pixmap.fillRectangle((mid + i) * pixelSize, (mid + 1 + i) * pixelSize, pixelSize, pixelSize)
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
            val grad = r. LeonardFloat() / radius
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
            val grad = 1f - (r. LeonardFloat() / radius)
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
