package com.github.ships.core.utils

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import java.util.*

class Starfield(poolSize: Int = 0) {
    private val starTexture: Texture = ProceduralTextureGenerator.getCircleTexture(8)
    private val rand = Random()

    fun render(batch: SpriteBatch, cameraPos: Vector2, zoom: Float, vw: Float, vh: Float, delta: Float) {
        val worldVisibleW = vw * zoom
        val worldVisibleH = vh * zoom
        val unitsPerPixel = worldVisibleW / Gdx.graphics.width.toFloat()

        // Enable Additive Blending for a "glow" feel and to stop popping
        Gdx.gl.glEnable(GL20.GL_BLEND)
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE)

        for (l in 0 until 3) {
            val pFactor = when(l) {
                0 -> 0.05f // Distant
                1 -> 0.2f  // Mid
                else -> 0.5f // Near
            }

            // Offset world relative to camera to ensure stars scale TOWARDS CENTER during zoom
            val offsetX = cameraPos.x * (1f - pFactor)
            val offsetY = cameraPos.y * (1f - pFactor)

            // Grid size adjusted based on zoom to maintain density
            val gridSize = 150f * zoom

            val txStart = MathUtils.floor((cameraPos.x - worldVisibleW / 2f - offsetX) / gridSize)
            val txEnd = MathUtils.ceil((cameraPos.x + worldVisibleW / 2f - offsetX) / gridSize)
            val tyStart = MathUtils.floor((cameraPos.y - worldVisibleH / 2f - offsetY) / gridSize)
            val tyEnd = MathUtils.ceil((cameraPos.y + worldVisibleH / 2f - offsetY) / gridSize)

            for (gx in txStart..txEnd) {
                for (gy in tyStart..tyEnd) {
                    val seed = (gx * 73856093L) xor (gy * 19349663L) xor (l * 83492791L)
                    rand.setSeed(seed)

                    // 3x Density: approx 150 stars per cell
                    val starsInCell = 150 + rand.nextInt(100)
                    for (i in 0 until starsInCell) {
                        val lx = rand.nextFloat() * gridSize
                        val ly = rand.nextFloat() * gridSize

                        val drawX = gx * gridSize + lx + offsetX
                        val drawY = gy * gridSize + ly + offsetY

                        // Precise culling
                        if (drawX < cameraPos.x - worldVisibleW / 2f - 1f || drawX > cameraPos.x + worldVisibleW / 2f + 1f ||
                            drawY < cameraPos.y - worldVisibleH / 2f - 1f || drawY > cameraPos.y + worldVisibleH / 2f + 1f) continue

                        val twinkle = (MathUtils.sin(drawX * 0.1f + drawY * 0.1f + (Gdx.graphics.frameId * 0.05f))) * 0.5f + 0.5f

                        // Smaller stars: 2-6 pixels
                        val sizePx = 2f + rand.nextFloat() * 4f
                        val renderSize = sizePx * unitsPerPixel

                        val color = Color()
                        if (rand.nextFloat() > 0.5f) {
                            color.fromHsv(rand.nextFloat() * 360f, 0.3f, 1f)
                        } else {
                            color.set(1f, 1f, 1f, 1f)
                        }
                        color.a = (0.4f + 0.6f * twinkle) * (1.0f - (l * 0.2f))

                        batch.setColor(color)
                        batch.draw(starTexture, drawX - renderSize / 2f, drawY - renderSize / 2f, renderSize, renderSize)
                    }
                }
            }
        }
        // Reset to standard alpha blending
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        batch.setColor(Color.WHITE)
    }

    fun dispose() {}
}
