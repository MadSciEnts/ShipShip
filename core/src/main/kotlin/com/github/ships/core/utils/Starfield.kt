package com.github.ships.core.utils

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import java.util.*

class Starfield(private val poolSizePerLayer: Int = 5000) {
    private data class StarData(val x: Float, val y: Float, val sizeMod: Float, val hue: Float, val sat: Float, val pulseSpeed: Float) {
        var state = MathUtils.random(0f, MathUtils.PI2)
    }

    private val starTexture: Texture = ProceduralTextureGenerator.createCircleTexture(16, Color.WHITE)
    private val layers = Array<Array<StarData>>()
    private val tileSize = 1500f

    private var lastTier = 0
    private val zoomStep = 2.5f

    init {
        for (l in 0 until 3) {
            layers.add(generateLayer(l))
        }
    }

    private fun generateLayer(tier: Int): Array<StarData> {
        val rand = Random(tier.toLong() * 12345L)
        val stars = Array<StarData>(poolSizePerLayer)
        for (i in 0 until poolSizePerLayer) {
            stars.add(StarData(
                rand.nextFloat() * tileSize,
                rand.nextFloat() * tileSize,
                0.5f + rand.nextFloat() * 1.5f,
                rand.nextFloat() * 360f,
                0.4f + rand.nextFloat() * 0.5f,
                1f + rand.nextFloat() * 3f
            ))
        }
        return stars
    }

    fun render(batch: SpriteBatch, cameraPos: Vector2, zoom: Float, vw: Float, vh: Float, delta: Float) {
        val logZoom = Math.log(zoom.toDouble()).toFloat() / Math.log(zoomStep.toDouble()).toFloat()
        val currentTier = Math.floor(logZoom.toDouble()).toInt()

        if (currentTier > lastTier) {
            for (t in lastTier until currentTier) {
                layers.removeIndex(0)
                layers.add(generateLayer(t + 3))
            }
            lastTier = currentTier
        }

        val worldVisibleW = vw * zoom
        val worldVisibleH = vh * zoom
        val unitsPerPixel = worldVisibleW / Gdx.graphics.width.toFloat()

        // Dynamic density logic to meet user requirements:
        // Target: High density (500 stars) at level 1 (zoom ~1), decreasing as zoom out.
        // Formula to keep stars on screen between 30 and 500.
        // Number of stars seen per tile is roughly poolSize * (visible_area / tile_area).
        val areaInTiles = (worldVisibleW * worldVisibleH) / (tileSize * tileSize)

        // We want stars_on_screen = stars_per_tile * areaInTiles.
        // At zoom 1, areaInTiles is small (~0.04). If we want 500 stars, stars_per_tile must be huge.
        // We calculate how many stars to pick from the pool to keep total on screen in [30, 500] range.
        val targetStarsOnScreen = MathUtils.lerp(500f, 30f, MathUtils.clamp(zoom / 100f, 0f, 1f))
        val starsToDraw = (targetStarsOnScreen / areaInTiles).toInt().coerceIn(10, poolSizePerLayer)

        for (i in 0 until layers.size) {
            val tier = lastTier + i
            val stars = layers[i]
            val depth = (tier - logZoom)

            var alpha = 1.0f
            if (depth < 0.5f) alpha = MathUtils.clamp(depth * 2f, 0f, 1f)
            else if (depth > 1.5f) alpha = MathUtils.clamp((2.5f - depth) * 2f, 0f, 1f)

            if (alpha <= 0.01f) continue

            val pFactor = 0.02f * Math.pow(2.0, i.toDouble()).toFloat()
            val offsetX = cameraPos.x * (1f - pFactor)
            val offsetY = cameraPos.y * (1f - pFactor)

            val startTileX = MathUtils.floor((cameraPos.x - worldVisibleW / 2f - offsetX) / tileSize)
            val endTileX = MathUtils.floor((cameraPos.x + worldVisibleW / 2f - offsetX) / tileSize)
            val startTileY = MathUtils.floor((cameraPos.y - worldVisibleH / 2f - offsetY) / tileSize)
            val endTileY = MathUtils.floor((cameraPos.y + worldVisibleH / 2f - offsetY) / tileSize)

            for (idx in 0 until starsToDraw) {
                val star = stars[idx]
                star.state += delta * star.pulseSpeed
                val pulse = (MathUtils.sin(star.state) + 1f) / 2f * 0.3f + 0.7f

                val targetPx = (2f + star.sizeMod * 4f)
                val renderSize = (targetPx * unitsPerPixel).coerceIn(2f * unitsPerPixel, 10f * unitsPerPixel)

                val starColor = Color().fromHsv(star.hue, star.sat, 1f)
                starColor.a = alpha * pulse * 0.6f
                batch.setColor(starColor)

                for (tx in startTileX..endTileX) {
                    for (ty in startTileY..endTileY) {
                        val drawX = tx * tileSize + star.x + offsetX
                        val drawY = ty * tileSize + star.y + offsetY

                        if (drawX > cameraPos.x - worldVisibleW / 2f - renderSize &&
                            drawX < cameraPos.x + worldVisibleW / 2f + renderSize &&
                            drawY > cameraPos.y - worldVisibleH / 2f - renderSize &&
                            drawY < cameraPos.y + worldVisibleH / 2f + renderSize) {

                            batch.draw(starTexture, drawX - renderSize / 2f, drawY - renderSize / 2f, renderSize, renderSize)
                        }
                    }
                }
            }
        }
        batch.setColor(Color.WHITE)
    }

    fun dispose() {
        starTexture.dispose()
    }
}
