package com.github.ships.core.utils

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array

class TrailPoint(val pos: Vector2, var life: Float)

class MotionTrail(val color: Color) {
    private val points = Array<TrailPoint>()
    private val maxLife = 0.5f
    private val minDistance = 0.2f

    fun update(dt: Float, currentPos: Vector2) {
        // Add new point if ship moved enough
        if (points.size == 0 || points.first().pos.dst2(currentPos) > minDistance * minDistance) {
            points.insert(0, TrailPoint(currentPos.cpy(), maxLife))
        }

        val iter = points.iterator()
        while (iter.hasNext()) {
            val p = iter.next()
            p.life -= dt
            if (p.life <= 0) iter.remove()
        }
    }

    fun render(shapeRenderer: ShapeRenderer, shipScale: Float) {
        if (points.size < 2) return

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        for (i in 0 until points.size - 1) {
            val p1 = points[i]
            val p2 = points[i + 1]

            val alpha = p1.life / maxLife
            shapeRenderer.setColor(color.r, color.g, color.b, color.a * alpha * 0.5f)

            // Draw a tapering line
            val width = 0.4f * shipScale * alpha
            shapeRenderer.rectLine(p1.pos.x, p1.pos.y, p2.pos.x, p2.pos.y, width)
        }
        shapeRenderer.end()
    }
}
