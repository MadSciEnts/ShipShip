package com.github.ships.core.entities

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.*
import com.github.ships.core.utils.ProceduralTextureGenerator

class Missile(
    val world: World,
    x: Float,
    y: Float,
    val target: Ship?,
    val speed: Float,
    val damage: Float,
    val color: Color
) {
    var body: Body
    private val texture: Texture = ProceduralTextureGenerator.getCircleTexture(6)
    var active = true
    var lifeTime = 5f
    private val steeringForce = 5f

    init {
        val bodyDef = BodyDef().apply {
            type = BodyDef.BodyType.DynamicBody
            position.set(x, y)
            linearDamping = 0.5f
        }
        body = world.createBody(bodyDef)
        body.userData = this

        val shape = PolygonShape()
        shape.setAsBox(0.1f, 0.05f)
        val fixtureDef = FixtureDef().apply {
            this.shape = shape
            isSensor = true
        }
        body.createFixture(fixtureDef)
        shape.dispose()
    }

    fun update(dt: Float) {
        lifeTime -= dt
        if (lifeTime <= 0) active = false

        if (target != null && target.health > 0) {
            val desiredVelocity = target.body.position.cpy().sub(body.position).nor().scl(speed)
            val currentVelocity = body.linearVelocity
            val steering = desiredVelocity.sub(currentVelocity).scl(steeringForce)
            body.applyForceToCenter(steering, true)
            body.setTransform(body.position, body.linearVelocity.angleRad())
        } else {
            val forward = Vector2(1f, 0f).setAngleRad(body.angle).scl(speed)
            body.linearVelocity = forward
        }
    }

    fun render(batch: SpriteBatch) {
        batch.setColor(color)
        batch.draw(texture, body.position.x - 0.1f, body.position.y - 0.05f, 0.1f, 0.05f, 0.2f, 0.1f, 1f, 1f, body.angle * 57.295776f, 0, 0, texture.width, texture.height, false, false)
        batch.setColor(Color.WHITE)
    }

    fun dispose() {
    }
}
