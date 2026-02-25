package com.github.ships.core.entities

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.*
import com.github.ships.core.weapons.ProjectileWeapon
import com.github.ships.core.weapons.Rarity
import kotlin.math.min
import kotlin.math.pow

class EnemyShip(world: World, x: Float, y: Float, spawnLevel: Int) : Ship(world, x, y, Color(0.5f, 0.6f, 0.5f, 1f)) {
    private enum class State { WANDER, PURSUE, RETREAT }
    private var state = State.WANDER

    private val baseForce = 5f
    private val sensorRange = 35f // Increased search range further
    private val baseAttackRange = 15f // Increased weapon range for enemies

    private var wanderTimer = 0f
    private val wanderDir = Vector2()
    private var isAggravated = false

    var onDeath: ((Ship) -> Unit)? = null

    init {
        level = spawnLevel
        val effectiveLevel = min(level.toFloat(), 60f)
        scale = 1.02f.pow(effectiveLevel - 1f)

        maxHealth = level * 5f
        health = maxHealth
        shield = 0f
        maxShield = 0f

        createBody()
        baseMaxSpeed = 6f
        // Enemies fire more frequently (reduced cooldown from 1.0 to 0.4)
        weaponPorts.add(ProjectileWeapon(Rarity.COMMON, 0.8f))
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

    override fun takeDamage(amount: Float, impactPoint: Vector2?) {
        super.takeDamage(amount, impactPoint)
        isAggravated = true // Pursue if attacked
    }

    override fun die() {
        onDeath?.invoke(this)
    }

    fun updateAI(player: PlayerShip, dt: Float, onProjectileCreated: (Projectile) -> Unit, potentialTargets: List<Ship>, allies: List<EnemyShip>) {
        val playerPos = player.body.position
        val distance = playerPos.dst(body.position)
        val adjustedSensorRange = sensorRange * scale
        val weaponRange = baseAttackRange * scale

        val isOverwhelmed = health < maxHealth * 0.3f

        if (distance < adjustedSensorRange || isAggravated) {
            state = if (isOverwhelmed && allies.size < 2) State.RETREAT else State.PURSUE
        } else {
            state = State.WANDER
        }

        val force = Vector2()
        val toPlayer = playerPos.cpy().sub(body.position).nor()

        when (state) {
            State.WANDER -> {
                wanderTimer -= dt
                if (wanderTimer <= 0) {
                    wanderTimer = MathUtils.random(2f, 5f)
                    wanderDir.set(MathUtils.random(-1f, 1f), MathUtils.random(-1f, 1f)).nor()
                }
                force.set(wanderDir).scl(baseForce * 0.5f * scale * scale)

                if (body.linearVelocity.len2() > 0.1f) {
                    body.setTransform(body.position, body.linearVelocity.angleRad())
                }
            }
            State.PURSUE -> {
                val playerWidth = 1.5f * player.scale
                val desiredDistance = min(weaponRange * 0.7f, playerWidth * 3f)

                if (distance > desiredDistance + 1f) {
                    force.set(toPlayer).scl(baseForce * 1.5f * scale * scale) // Faster pursuit
                } else if (distance < desiredDistance - 1f) {
                    force.set(toPlayer).scl(-baseForce * scale * scale)
                } else {
                    body.linearVelocity = body.linearVelocity.scl(0.95f)
                }
                body.setTransform(body.position, toPlayer.angleRad())

                if (distance < weaponRange) {
                    fireWeapons(toPlayer, onProjectileCreated, potentialTargets)
                }
            }
            State.RETREAT -> {
                val flockingForce = calculateFlocking(allies)
                val retreatDir = toPlayer.cpy().scl(-1f).add(flockingForce.scl(2f)).nor()
                force.set(retreatDir).scl(baseForce * 1.5f * scale * scale)

                body.setTransform(body.position, toPlayer.angleRad())
                if (distance < weaponRange) {
                    fireWeapons(toPlayer, onProjectileCreated, potentialTargets)
                }
            }
        }

        body.applyForceToCenter(force, true)
    }

    private fun calculateFlocking(allies: List<EnemyShip>): Vector2 {
        val force = Vector2()
        var count = 0
        val neighborhood = 15f * scale

        for (ally in allies) {
            if (ally == this) continue
            val dist = body.position.dst(ally.body.position)
            if (dist < neighborhood) {
                force.add(ally.body.linearVelocity)
                force.add(ally.body.position.cpy().sub(body.position).nor().scl(0.8f))
                count++
            }
        }

        if (count > 0) {
            force.scl(1f / count).nor()
        }
        return force
    }
}
