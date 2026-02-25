package com.github.ships.core.utils

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Pool

class AtmosPoint(val pos: Vector2 = Vector2(), var life: Float = 0f, var offset: Vector2 = Vector2(), val velocity: Vector2 = Vector2())

class VentingAtmos {
    private val points = Array<AtmosPoint>()
    private val maxPointLife = 0.4f
    private val minDistance = 0.05f
    private val color = Color(0.7f, 0.7f, 0.7f, 0.4f)

    private var elapsed = 0f
    private val maxEmitterDuration = 4.0f

    private val pointPool = object : Pool<AtmosPoint>() {
        override fun newObject(): AtmosPoint = AtmosPoint()
    }

    fun update(dt: Float, currentPos: Vector2, spawnVelocity: Vector2 = Vector2.Zero) {
        elapsed += dt

        // Slowly stop emitting as we approach 4 seconds
        val emissionChance = MathUtils.clamp((maxEmitterDuration - elapsed) / 1.0f, 0f, 1f)

        if (elapsed < maxEmitterDuration && MathUtils.random() < emissionChance) {
            if (points.size == 0 || points[0].pos.dst2(currentPos) > minDistance * minDistance) {
                val p = pointPool.obtain()
                p.pos.set(currentPos)
                p.life = maxPointLife
                p.offset.set(0f, 0f)
                p.velocity.set(spawnVelocity)
                points.insert(0, p)
            }
        }

        val iter = points.iterator()
        while (iter.hasNext()) {
            val p = iter.next()
            p.life -= dt

            if (p.life <= 0) {
                iter.remove()
                pointPool.free(p)
            } else {
                // Move point by its velocity
                p.pos.add(p.velocity.x * dt, p.velocity.y * dt)

                val ageRatio = 1f - (p.life / maxPointLife)
                val turbulence = ageRatio * 1.5f
                p.offset.add(
                    MathUtils.random(-turbulence, turbulence) * dt * 5f,
                    MathUtils.random(-turbulence, turbulence) * dt * 5f
                )
            }
        }
    }

    fun isFinished(): Boolean = elapsed >= maxEmitterDuration && points.size == 0

    fun render(shapeRenderer: ShapeRenderer, shipScale: Float) {
        if (points.size < 2) return

        for (i in 0 until points.size - 1) {
            val p1 = points[i]
            val p2 = points[i + 1]

            val lifeRatio = p1.life / maxPointLife
            val easedAlpha = Interpolation.pow2In.apply(lifeRatio)

            val renderColor = color.cpy()
            renderColor.a *= easedAlpha

            // Atmospheric dissipation effect
            val growFactor = 1.0f + (1.0f - easedAlpha) * 5.0f
            val width = 0.04f * shipScale * growFactor

            shapeRenderer.setColor(renderColor)
            shapeRenderer.rectLine(
                p1.pos.x + p1.offset.x, p1.pos.y + p1.offset.y,
                p2.pos.x + p2.offset.x, p2.pos.y + p2.offset.y,
                width
            )
        }
    }
}
