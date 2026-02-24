package com.github.ships.core.entities

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.*
import com.github.ships.core.weapons.ProjectileWeapon
import com.github.ships.core.weapons.Rarity
import kotlin.math.min
import kotlin.math.pow

class EnemyShip(world: World, x: Float, y: Float, spawnLevel: Int) : Ship(world, x, y, Color(0.5f, 0.6f, 0.5f, 1f)) {
    private val baseForce = 5f
    private val sensorRange = 15f
    private val baseAttackRange = 10f

    // Callback for death effects
    var onDeath: ((Ship) -> Unit)? = null

    init {
        level = spawnLevel
        scale = 1.1f.pow(level - 1f)
        maxHealth = level * 5f
        health = maxHealth
        shield = 0f
        maxShield = 0f
        createBody()
        baseMaxSpeed = 2f
        weaponPorts.add(ProjectileWeapon(Rarity.COMMON, 1.0f))
    }

    override fun createBody() {
        val bodyDef = BodyDef().apply {
            type = BodyDef.BodyType.DynamicBody
            position.set(x, y)
            linearDamping = 1f
            angularDamping = 1f
        }
        body = world.createBody(bodyDef)
        body.userData = this
        val shape = CircleShape()
        shape.radius = 0.5f * scale
        val fixtureDef = FixtureDef().apply {
            this.shape = shape
            density = 1f
        }
        body.createFixture(fixtureDef)
        shape.dispose()
    }

    override fun die() {
        onDeath?.invoke(this)
    }

    fun updateAI(player: PlayerShip, dt: Float, onProjectileCreated: (Projectile) -> Unit, potentialTargets: List<Ship>) {
        val playerPos = player.body.position
        val distance = playerPos.dst(body.position)
        val adjustedSensorRange = sensorRange * scale
        val weaponRange = baseAttackRange * scale
        val playerWidth = 1.5f * player.scale
        val desiredDistance = min(weaponRange * 0.5f, playerWidth * 2f)
        if (distance < adjustedSensorRange) {
            val toPlayer = playerPos.cpy().sub(body.position).nor()
            val force = Vector2()
            if (distance > desiredDistance + 1f) {
                force.set(toPlayer).scl(baseForce * scale * scale)
            } else if (distance < desiredDistance - 1f) {
                force.set(toPlayer).scl(-baseForce * scale * scale)
            } else {
                body.linearVelocity = body.linearVelocity.scl(0.95f)
            }
            body.applyForceToCenter(force, true)
            val targetAngle = toPlayer.angleRad()
            body.setTransform(body.position, targetAngle)
            if (distance < weaponRange) {
                fireWeapons(toPlayer, onProjectileCreated, potentialTargets)
            }
        }
    }
}
