package com.github.ships.core.utils

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Pool

class TrailPoint(val pos: Vector2 = Vector2(), var life: Float = 0f, var offset: Vector2 = Vector2())

class MotionTrail(var color: Color) {
    private val points = Array<TrailPoint>()
    private val maxLife = 4.0f
    private val minDistance = 0.15f

    private val pointPool = object : Pool<TrailPoint>() {
        override fun newObject(): TrailPoint = TrailPoint()
    }

    fun update(dt: Float, currentPos: Vector2) {
        if (points.size == 0 || points[0].pos.dst2(currentPos) > minDistance * minDistance) {
            val p = pointPool.obtain()
            p.pos.set(currentPos)
            p.life = maxLife
            p.offset.set(0f, 0f)
            points.insert(0, p)
        }

        val iter = points.iterator()
        while (iter.hasNext()) {
            val p = iter.next()
            p.life -= dt

            if (p.life <= 0) {
                iter.remove()
                pointPool.free(p)
            } else {
                val ageRatio = 1f - (p.life / maxLife)
                val turbulence = ageRatio * 1f
                p.offset.add(
                    MathUtils.random(-turbulence, turbulence) * dt * 5f,
                    MathUtils.random(-turbulence, turbulence) * dt * 5f
                )
            }
        }
    }

    fun render(shapeRenderer: ShapeRenderer, shipScale: Float) {
        if (points.size < 2) return

        for (i in 0 until points.size - 1) {
            val p1 = points[i]
            val p2 = points[i + 1]

            val lifeRatio = p1.life / maxLife
            val easedAlpha = Interpolation.pow2In.apply(lifeRatio)

            val renderColor = color.cpy()
            renderColor.lerp(Color.BLACK, 1f - easedAlpha)
            renderColor.a = easedAlpha

            // Fixed: Reduced start width and increased growth to 4x in all axes
            // Base width is now thinner (0.05f), growing to 0.6f (4x)
            val growFactor = 1.5f + (1.0f - easedAlpha) * 24.0f
            val width = 0.02f * shipScale * growFactor

            shapeRenderer.setColor(renderColor)
            shapeRenderer.rectLine(
                p1.pos.x + p1.offset.x, p1.pos.y + p1.offset.y,
                p2.pos.x + p2.offset.x, p2.pos.y + p2.offset.y,
                width
            )
        }
    }
}
