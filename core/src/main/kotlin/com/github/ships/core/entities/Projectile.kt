package com.github.ships.core.entities

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.*
import com.github.ships.core.utils.ProceduralTextureGenerator

class Projectile(
    val world: World,
    x: Float,
    y: Float,
    val direction: Vector2,
    val baseScreenSpeed: Float,
    var damage: Float,
    val color: Color,
    val owner: Ship,
    val chargeMultiplier: Float = 1.0f
) {
    var body: Body
    // Using a more square-ish or wide texture for enemy projectiles
    val isEnemy = owner is EnemyShip
    val texture: Texture = if (isEnemy) {
        ProceduralTextureGenerator.createRectangleTexture(40, 12, color)
    } else {
        ProceduralTextureGenerator.createRectangleTexture(12, 40, color)
    }

    var active = true
    var lifeTime = 2f

    private val damageScale = (1.5f + (damage - 1f) * 0.1f) * chargeMultiplier

    // Swap width and height for enemy projectiles to make them "wider than they are long"
    private val baseWidth = (if (isEnemy) 1.0f else 0.3f) * damageScale
    private val baseHeight = (if (isEnemy) 0.3f else 1.0f) * damageScale

    init {
        val bodyDef = BodyDef().apply {
            type = BodyDef.BodyType.DynamicBody
            position.set(x, y)
            bullet = true
            // Match texture rotation logic
            angle = direction.angleRad() - (Math.PI.toFloat() / 2f)
        }
        body = world.createBody(bodyDef)
        body.userData = this

        val shape = PolygonShape()
        shape.setAsBox(baseWidth / 2f, baseHeight / 2f)
        val fixtureDef = FixtureDef().apply {
            this.shape = shape
            isSensor = true
        }
        body.createFixture(fixtureDef)
        shape.dispose()
    }

    fun update(dt: Float, zoom: Float, worldWidth: Float) {
        lifeTime -= dt
        if (lifeTime <= 0) active = false

        val worldSpeed = baseScreenSpeed * (worldWidth * zoom)
        body.setLinearVelocity(direction.x * worldSpeed, direction.y * worldSpeed)
    }

    fun render(batch: SpriteBatch, zoom: Float, worldWidth: Float) {
        val angle = body.angle * 57.295776f
        val pixelsPerUnit = Gdx.graphics.width.toFloat() / (worldWidth * zoom)
        val minWorldSize = 5.0f / pixelsPerUnit

        val renderWidth = Math.max(baseWidth, minWorldSize)
        val renderHeight = Math.max(baseHeight, minWorldSize * (baseHeight / baseWidth))

        batch.setColor(color)
        batch.draw(texture, body.position.x - renderWidth / 2f, body.position.y - renderHeight / 2f,
            renderWidth / 2f, renderHeight / 2f,
            renderWidth, renderHeight,
            1f, 1f, angle, 0, 0, texture.width, texture.height, false, false)
        batch.setColor(Color.WHITE)
    }

    fun dispose() {
        texture.dispose()
    }
}
