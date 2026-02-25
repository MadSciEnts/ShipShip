package com.github.ships.core.utils

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import java.util.*

class Starfield(poolSize: Int = 0) { // poolSize unused in new robust version
    private val starTexture: Texture = ProceduralTextureGenerator.getCircleTexture(8, Color.WHITE)
    private val rand = Random()

    fun render(batch: SpriteBatch, cameraPos: Vector2, zoom: Float, vw: Float, vh: Float, delta: Float) {
        val worldVisibleW = vw * zoom
        val worldVisibleH = vh * zoom
        val unitsPerPixel = worldVisibleW / Gdx.graphics.width.toFloat()

        // Three layers of parallax
        for (l in 0 until 3) {
            val pFactor = 0.05f / (l + 1)

            // Offset world by parallax factor
            val offsetX = cameraPos.x * (1f - pFactor)
            val offsetY = cameraPos.y * (1f - pFactor)

            // Grid-based deterministic star placement
            // Grid size grows with zoom to keep pixel density same
            val gridSize = 400f * zoom
            val startX = MathUtils.floor((cameraPos.x - worldVisibleW / 2f - offsetX) / gridSize)
            val endX = MathUtils.ceil((cameraPos.x + worldVisibleW / 2f - offsetX) / gridSize)
            val startY = MathUtils.floor((cameraPos.y - worldVisibleH / 2f - offsetY) / gridSize)
            val endY = MathUtils.ceil((cameraPos.y + worldVisibleH / 2f - offsetY) / gridSize)

            for (gx in startX..endX) {
                for (gy in startY..endY) {
                    // Seed random with grid coords for consistency
                    val seed = (gx * 73856093L) xor (gy * 19349663L) xor (l * 83492791L)
                    rand.setSeed(seed)

                    // 15-20 stars per grid cell
                    val starsInCell = 15 + rand.nextInt(5)
                    for (i in 0 until starsInCell) {
                        val lx = rand.nextFloat() * gridSize
                        val ly = rand.nextFloat() * gridSize

                        val drawX = gx * gridSize + lx + offsetX
                        val drawY = gy * gridSize + ly + offsetY

                        // Twinkle and size
                        val twinkle = (MathUtils.sin(drawX + drawY + (Gdx.graphics.frameId * 0.05f)) + 1f) / 2f
                        val sizePx = 3f + rand.nextFloat() * 7f
                        val renderSize = sizePx * unitsPerPixel

                        val hue = rand.nextFloat() * 360f
                        val sat = 0.2f + rand.nextFloat() * 0.4f
                        val color = Color().fromHsv(hue, sat, 1f)
                        color.a = (0.4f + 0.6f * twinkle) * (1.0f / (l + 1))

                        batch.setColor(color)
                        batch.draw(starTexture, drawX - renderSize / 2f, drawY - renderSize / 2f, renderSize, renderSize)
                    }
                }
            }
        }
        batch.setColor(Color.WHITE)
    }

    fun dispose() {
        // Shared texture
    }
}
