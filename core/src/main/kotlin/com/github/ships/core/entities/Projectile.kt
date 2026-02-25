package com.github.ships.core.entities

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
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
    val isEnemy = owner is EnemyShip

    val texture: Texture = if (isEnemy) {
        ProceduralTextureGenerator.getRectangleTexture(20, 20)
    } else {
        ProceduralTextureGenerator.getRectangleTexture(6, 40)
    }

    var active = true
    var lifeTime = if (isEnemy) 10f else 15f
    private var rotationTimer = 0f

    private val damageScale = (1.5f + (damage - 1f) * 0.1f) * chargeMultiplier

    private val baseWidth = (if (isEnemy) 0.5f else 0.15f) * damageScale
    private val baseHeight = (if (isEnemy) 0.5f else 1.0f) * damageScale

    init {
        val spawnX = x + direction.x * (baseHeight / 2f)
        val spawnY = y + direction.y * (baseHeight / 2f)

        val bodyDef = BodyDef().apply {
            type = BodyDef.BodyType.DynamicBody
            position.set(spawnX, spawnY)
            bullet = true
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

    fun update(dt: Float, cameraPos: Vector3, zoom: Float, worldWidth: Float, worldHeight: Float) {
        lifeTime -= dt

        val worldVisibleW = worldWidth * zoom
        val worldVisibleH = worldHeight * zoom
        val left = cameraPos.x - worldVisibleW / 2f
        val right = cameraPos.x + worldVisibleW / 2f
        val bottom = cameraPos.y - worldVisibleH / 2f
        val top = cameraPos.y + worldVisibleH / 2f

        val isOffScreen = body.position.x < left || body.position.x > right ||
                          body.position.y < bottom || body.position.y > top

        if (isEnemy) {
            if (lifeTime <= 0) active = false
            rotationTimer += dt * 500f

            // Fixed: Use owner.ventingAtmos correctly
            owner.ventingAtmos.update(dt, body.position, direction.cpy().scl(-2f))
        } else {
            if (isOffScreen) active = false
            if (lifeTime <= 0) active = false
        }

        val worldSpeed = baseScreenSpeed * worldVisibleW
        body.setLinearVelocity(direction.x * worldSpeed, direction.y * worldSpeed)
    }

    fun render(batch: SpriteBatch, zoom: Float, worldWidth: Float) {
        val angle = if (isEnemy) rotationTimer else body.angle * 57.295776f

        val unitsPerPixel = (worldWidth * zoom) / Gdx.graphics.width.toFloat()
        val minWorldSize = 5.0f * unitsPerPixel

        val renderWidth = Math.max(baseWidth, minWorldSize)
        val renderHeight = Math.max(baseHeight, minWorldSize * (baseHeight / baseWidth))

        val renderColor = color.cpy()
        if (renderColor.a < 0.5f) renderColor.a = 0.5f

        batch.setColor(renderColor)
        batch.draw(texture, body.position.x - renderWidth / 2f, body.position.y - renderHeight / 2f,
            renderWidth / 2f, renderHeight / 2f,
            renderWidth, renderHeight,
            1f, 1f, angle, 0, 0, texture.width, texture.height, false, false)
        batch.setColor(Color.WHITE)
    }

    fun dispose() {
    }
}
