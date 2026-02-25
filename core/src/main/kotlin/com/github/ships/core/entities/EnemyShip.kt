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
    private enum class State { WANDER, PURSUE, RETREAT, DODGE, REPAIR_MODE }
    private var state = State.WANDER

    private val baseForce = 25f
    private val sensorRange = 50f
    private val baseAttackRange = 25f

    private var wanderTimer = 0f
    private val wanderDir = Vector2()
    private var isAggravated = false

    // Dodge mechanics
    private var dodgeTimer = 0f
    private val dodgeDir = Vector2()
    private var dodgeWobbleTimer = 0f
    private var nextDodgeTurnTimer = 0f

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
        baseMaxSpeed = 12f
        weaponPorts.add(ProjectileWeapon(Rarity.COMMON, 1.2f))
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
        isAggravated = true
        if (state != State.DODGE) {
            state = State.DODGE
            dodgeTimer = MathUtils.random(2f, 3f)
            nextDodgeTurnTimer = 0f
            dodgeWobbleTimer = 0f
        }
    }

    override fun die() {
        onDeath?.invoke(this)
    }

    fun updateAI(player: PlayerShip, dt: Float, onProjectileCreated: (Projectile) -> Unit, potentialTargets: List<Ship>, allies: List<EnemyShip>) {
        val playerPos = player.body.position
        val distance = playerPos.dst(body.position)
        val adjustedSensorRange = sensorRange * scale
        val weaponRange = baseAttackRange * scale

        val needsRepair = health < maxHealth * 0.4f
        isSignalingRepair = needsRepair

        // State logic
        if (dodgeTimer > 0) {
            dodgeTimer -= dt
            state = State.DODGE
        } else if (state == State.REPAIR_MODE) {
            // Repair mode logic handled in specialized method below
        } else if (distance < adjustedSensorRange || isAggravated) {
            state = if (needsRepair) State.RETREAT else State.PURSUE
        } else {
            state = State.WANDER
        }

        val forceVector = Vector2()
        val toPlayer = playerPos.cpy().sub(body.position).nor()

        when (state) {
            State.WANDER -> {
                wanderTimer -= dt
                if (wanderTimer <= 0) {
                    wanderTimer = MathUtils.random(2f, 5f)
                    wanderDir.set(MathUtils.random(-1f, 1f), MathUtils.random(-1f, 1f)).nor()
                }
                forceVector.set(wanderDir).scl(baseForce * 0.5f * scale)
            }
            State.DODGE -> {
                nextDodgeTurnTimer -= dt
                if (nextDodgeTurnTimer <= 0) {
                    nextDodgeTurnTimer = MathUtils.random(0.5f, 1.0f)
                    dodgeDir.set(MathUtils.random(-1f, 1f), MathUtils.random(-1f, 1f)).nor()
                }

                dodgeWobbleTimer += dt * 8f
                val wobble = MathUtils.sin(dodgeWobbleTimer) * 0.5f
                val sideDir = dodgeDir.cpy().rotate90(1)
                forceVector.set(dodgeDir).add(sideDir.scl(wobble)).nor().scl(baseForce * 1.5f * scale)
            }
            State.PURSUE -> {
                val desiredDistance = min(weaponRange * 0.7f, 5f * scale)
                if (distance > desiredDistance + 1f) {
                    forceVector.set(toPlayer).scl(baseForce * scale)
                } else if (distance < desiredDistance - 1f) {
                    forceVector.set(toPlayer).scl(-baseForce * scale)
                }
                if (distance < weaponRange) fireWeapons(toPlayer, onProjectileCreated, potentialTargets)
            }
            State.RETREAT -> {
                val repairStation = findNearbyHealthyAlly(allies)
                if (repairStation != null) {
                    val toStation = repairStation.body.position.cpy().sub(body.position).nor()
                    forceVector.set(toStation).scl(baseForce * 1.2f * scale)
                } else {
                    forceVector.set(toPlayer).scl(-baseForce * scale)
                }
            }
            State.REPAIR_MODE -> {
                isRepairingAlly = true
                val target = allies.firstOrNull { it.isSignalingRepair }
                if (target != null) {
                    val toTarget = target.body.position.cpy().sub(body.position).nor()
                    forceVector.set(toTarget).scl(baseForce * scale)
                    if (body.position.dst(target.body.position) < (scale + target.scale)) {
                        // Heal 10% every 2 seconds = 5% per second
                        val healAmt = target.maxHealth * 0.05f * dt
                        target.receiveHeal(healAmt)
                        activeBeams.add(BeamData(body.position.cpy(), target.body.position.cpy(), Color.CYAN, 0.1f, 0.1f, 0.1f * scale))
                    }
                } else {
                    state = State.WANDER
                    isRepairingAlly = false
                }
            }
        }

        // Transition Undamaged to Repair mode if someone signals
        if (health >= maxHealth && state != State.REPAIR_MODE) {
            val signal = allies.firstOrNull { it.isSignalingRepair && it.body.position.dst(body.position) < sensorRange * scale }
            if (signal != null) state = State.REPAIR_MODE
        }

        body.applyForceToCenter(forceVector, true)

        if (forceVector.len2() > 0.01f) {
            val targetAngle = forceVector.angleRad()
            body.setTransform(body.position, MathUtils.lerpAngle(body.angle, targetAngle, 0.15f))
        }
    }

    private fun findNearbyHealthyAlly(allies: List<EnemyShip>): EnemyShip? {
        return allies.firstOrNull { it != this && it.health >= it.maxHealth && it.body.position.dst(body.position) < sensorRange * scale }
    }
}
