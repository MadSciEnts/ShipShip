package com.github.ships.core.entities

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.*
import com.github.ships.core.utils.ProceduralTextureGenerator

class ExperienceCrystal(val world: World, x: Float, y: Float, val xpAmount: Float) {
    var body: Body
    private val texture: Texture = ProceduralTextureGenerator.getCircleTexture(8)
    var active = true
    private val baseRadius = 0.15f

    init {
        val bodyDef = BodyDef().apply {
            type = BodyDef.BodyType.DynamicBody
            position.set(x, y)
            linearDamping = 0.5f
            angularDamping = 1f
        }
        body = world.createBody(bodyDef)
        body.userData = this

        val shape = CircleShape()
        shape.radius = baseRadius
        val fixtureDef = FixtureDef().apply {
            this.shape = shape
            isSensor = true
        }
        body.createFixture(fixtureDef)
        shape.dispose()

        body.applyLinearImpulse(Vector2(MathUtils.random(-0.5f, 0.5f), MathUtils.random(-0.5f, 0.5f)), body.worldCenter, true)
    }

    fun update(dt: Float, player: PlayerShip, cameraLeft: Float, cameraRight: Float, cameraBottom: Float, cameraTop: Float, zoom: Float, worldWidth: Float) {
        val pos = body.position

        if (pos.x > cameraLeft && pos.x < cameraRight && pos.y > cameraBottom && pos.y < cameraTop) {
            val playerPos = player.body.position
            val toPlayer = playerPos.cpy().sub(pos)
            val dist = toPlayer.len()

            val requiredSpeed = Math.max(dist / 1.5f, 5f * zoom)
            body.linearVelocity = toPlayer.nor().scl(requiredSpeed)
        }
    }

    fun render(batch: SpriteBatch, zoom: Float, worldWidth: Float) {
        val unitsPerPixel = (worldWidth * zoom) / Gdx.graphics.width.toFloat()
        val minWorldSize = 5.0f * unitsPerPixel
        val renderRadius = Math.max(baseRadius * zoom, minWorldSize / 2f)

        batch.setColor(Color.CYAN) // Tint white texture to Cyan
        batch.draw(texture, body.position.x - renderRadius, body.position.y - renderRadius, renderRadius * 2, renderRadius * 2)
        batch.setColor(Color.WHITE)
    }

    fun dispose() {
    }
}
