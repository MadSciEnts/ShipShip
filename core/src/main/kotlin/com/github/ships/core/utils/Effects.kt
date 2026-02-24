package com.github.ships.core.utils

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Interpolation
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Pool

class DamageParticle(var x: Float = 0f, var y: Float = 0f, val color: Color = Color.WHITE.cpy(), var isDebris: Boolean = false, var isExplosion: Boolean = false, var baseSize: Float = 0.12f) {
    var lifeTime = 0f
    var maxLife = 0.2f
    val velocity = Vector2()
    var rotation = 0f
    var rotateSpeed = 0f

    fun init(x: Float, y: Float, color: Color, isDebris: Boolean, isExplosion: Boolean, size: Float, lifetime: Float = 0.2f) {
        this.x = x
        this.y = y
        this.color.set(color)
        this.isDebris = isDebris
        this.isExplosion = isExplosion
        this.baseSize = size
        this.maxLife = lifetime
        this.lifeTime = maxLife
        this.rotation = MathUtils.random(360f)
        this.rotateSpeed = MathUtils.random(-60f, 60f)

        if (isDebris) {
            this.velocity.set(MathUtils.random(-0.3f, 0.3f), MathUtils.random(-0.3f, 0.3f))
        } else if (isExplosion) {
            this.velocity.set(MathUtils.random(-3f, 3f), MathUtils.random(-3f, 3f))
        } else {
            this.velocity.set(MathUtils.random(-5f, 5f), MathUtils.random(-5f, 5f))
        }
    }

    fun update(dt: Float) {
        lifeTime -= dt
        x += velocity.x * dt
        y += velocity.y * dt
        rotation += rotateSpeed * dt

        if (isExplosion) {
            velocity.scl(0.95f)
        }
    }

    fun getCurrentSize(): Float {
        val lifeRatio = lifeTime / maxLife
        if (isExplosion) {
            val progress = 1f - lifeRatio
            return if (progress < 0.2f) {
                baseSize * (progress / 0.2f)
            } else {
                baseSize * lifeRatio
            }
        }
        return baseSize * lifeRatio
    }
}

class ShieldEffect(var x: Float = 0f, var y: Float = 0f, var radius: Float = 0f) {
    var lifeTime = 0.3f
    val maxLife = 0.3f
}

class EffectManager {
    private val particles = Array<DamageParticle>()
    private val shields = Array<ShieldEffect>()

    private val particlePool = object : Pool<DamageParticle>() {
        override fun newObject(): DamageParticle = DamageParticle()
    }
    private val shieldPool = object : Pool<ShieldEffect>() {
        override fun newObject(): ShieldEffect = ShieldEffect()
    }

    fun spawnImpact(x: Float, y: Float, shipWidth: Float) {
        for (i in 0 until 6) {
            val p = particlePool.obtain()
            p.init(x, y, Color(MathUtils.random(), MathUtils.random(), MathUtils.random(), 1f), false, false, 0.12f)
            particles.add(p)
        }

        for (i in 0 until MathUtils.random(1, 5)) {
            val p = particlePool.obtain()
            val size = shipWidth * MathUtils.random(0.05f, 0.1f)
            val grey = MathUtils.random(0.3f, 0.6f)
            p.init(x, y, Color(grey, grey, grey, 1f), true, false, size, MathUtils.random(1f, 5f))
            particles.add(p)
        }
    }

    fun spawnExplosion(x: Float, y: Float, shipWidth: Float, shipHeight: Float) {
        for (i in 0 until 30) {
            val p = particlePool.obtain()

            // Generate half-saturated colors for explosion
            val color = Color()
            val type = MathUtils.random(2)
            when(type) {
                0 -> color.fromHsv(0f, 0.5f, 1f) // Half-saturated Red
                1 -> color.fromHsv(60f, 0.5f, 1f) // Half-saturated Yellow
                else -> color.set(1f, 1f, 1f, 1f) // White
            }

            val rx = x + MathUtils.random(-shipWidth / 2f, shipWidth / 2f)
            val ry = y + MathUtils.random(-shipHeight / 2f, shipHeight / 2f)

            val size = Math.max(shipWidth, shipHeight) * 0.25f
            p.init(rx, ry, color, false, true, size, 0.5f)
            particles.add(p)
        }

        for (i in 0 until 15) {
            val p = particlePool.obtain()
            val rx = x + MathUtils.random(-shipWidth / 2f, shipWidth / 2f)
            val ry = y + MathUtils.random(-shipHeight / 2f, shipHeight / 2f)
            val size = Math.max(shipWidth, shipHeight) * MathUtils.random(0.05f, 0.15f)
            val grey = MathUtils.random(0.2f, 0.5f)
            p.init(rx, ry, Color(grey, grey, grey, 1f), true, false, size, 2.0f)
            particles.add(p)
        }
    }

    fun spawnShieldHit(x: Float, y: Float, radius: Float) {
        val s = shieldPool.obtain()
        s.x = x
        s.y = y
        s.radius = radius
        s.lifeTime = s.maxLife
        shields.add(s)
    }

    fun update(dt: Float) {
        val pIter = particles.iterator()
        while (pIter.hasNext()) {
            val p = pIter.next()
            p.update(dt)
            if (p.lifeTime <= 0) {
                pIter.remove()
                particlePool.free(p)
            }
        }

        val sIter = shields.iterator()
        while (sIter.hasNext()) {
            val s = sIter.next()
            s.lifeTime -= dt
            if (s.lifeTime <= 0) {
                sIter.remove()
                shieldPool.free(s)
            }
        }
    }

    fun render(shapeRenderer: ShapeRenderer) {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        for (p in particles) {
            val lifeRatio = p.lifeTime / p.maxLife
            val currentSize = p.getCurrentSize()

            if (p.isExplosion) {
                shapeRenderer.setColor(p.color.r, p.color.g, p.color.b, p.color.a * lifeRatio * 0.8f)
                shapeRenderer.circle(p.x, p.y, currentSize, 12)
            } else if (p.isDebris) {
                val renderColor = p.color.cpy().lerp(Color.BLACK, 1f - lifeRatio)
                renderColor.a = lifeRatio
                shapeRenderer.setColor(renderColor)
                shapeRenderer.rect(p.x - currentSize / 2, p.y - currentSize / 2,
                    currentSize / 2, currentSize / 2, currentSize, currentSize, 1f, 1f, p.rotation)
            } else {
                shapeRenderer.setColor(p.color.r, p.color.g, p.color.b, p.color.a * lifeRatio)
                shapeRenderer.circle(p.x, p.y, currentSize, 6)
            }
        }

        for (s in shields) {
            val alpha = s.lifeTime / s.maxLife
            shapeRenderer.setColor(0.2f, 0.6f, 1f, alpha * 0.5f)
            shapeRenderer.circle(s.x, s.y, s.radius * (1.2f - alpha * 0.2f), 20)
        }
        shapeRenderer.end()
    }
}
