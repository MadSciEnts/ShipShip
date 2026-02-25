package com.github.ships.core.entities

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.*
import com.badlogic.gdx.physics.box2d.World
import kotlin.math.max
import kotlin.math.pow

class PlayerShip(world: World, x: Float, y: Float) : Ship(world, x, y, Color.GRAY) {
    var experience: Float = 0f
    private val baseXPThreshold = 100f
    private var pendingEvolution = false

    // Warp mechanics
    var warpCharge = 0f
    var isWarping = false
    private var warpTimer = 0f
    private val maxWarpDuration = 2.0f
    private val warpSpeedMultiplier = 5.0f

    init {
        level = 1
        updateStats()
        createBody()
        baseMaxSpeed = 15f
    }

    override fun createBody() {
        val bodyDef = BodyDef().apply {
            type = BodyDef.BodyType.DynamicBody
            position.set(x, y)
            // Task: Maintain momentum much more (lower damping) as size increases
            linearDamping = 1.0f / scale
            angularDamping = 1.0f
        }
        body = world.createBody(bodyDef)
        body.userData = this

        val shape = CircleShape()
        shape.radius = 0.6f * scale
        val fixtureDef = FixtureDef().apply {
            this.shape = shape
            density = 1f
            friction = 0.2f
            restitution = 0.1f
        }
        body.createFixture(fixtureDef)
        shape.dispose()
    }

    override fun update(dt: Float) {
        super.update(dt)

        // Task Fix: Warp drive pushes in the direction the ship is facing (even during turns)
        if (isWarping) {
            warpTimer -= dt
            if (warpTimer <= 0) {
                isWarping = false
            } else {
                // Facing vector: 0 radians is Right (X+), so forward is cos/sin
                val forward = Vector2(MathUtils.cos(body.angle), MathUtils.sin(body.angle))
                val warpVel = forward.scl(maxSpeed * warpSpeedMultiplier)
                body.linearVelocity = warpVel
            }
        }
    }

    fun applyInput(moveX: Float, moveY: Float, dt: Float) {
        if (isWarping) return // Lock normal movement during warp

        val forceMultiplier = 30f * scale
        val force = Vector2(moveX, moveY).scl(forceMultiplier)
        body.applyForceToCenter(force, true)

        if (force.len2() > 0.01f) {
            val targetAngle = force.angleRad()

            // Task: Turn slower as level increases (more mass/inertia)
            val rotationSpeedFactor = 1.0f / (1.0f + (level - 1) * 0.15f)
            val lerpFactor = 0.15f * rotationSpeedFactor

            var currentAngle = body.angle
            while (currentAngle < -MathUtils.PI) currentAngle += MathUtils.PI * 2f
            while (currentAngle > MathUtils.PI) currentAngle -= MathUtils.PI * 2f

            val newAngle = MathUtils.lerpAngle(currentAngle, targetAngle, lerpFactor)
            body.setTransform(body.position, newAngle)
        }
    }

    fun activateWarp(chargeRatio: Float) {
        if (chargeRatio < 0.1f) return
        isWarping = true
        warpTimer = chargeRatio * maxWarpDuration
    }

    fun addExperience(amount: Float) {
        experience += amount
        if (experience >= getExperienceThreshold()) {
            levelUp()
        }
    }

    fun getExperienceThreshold(): Float = baseXPThreshold * 2.0f.pow(level - 1)

    private fun levelUp() {
        level++
        experience = 0f
        scale *= 1.025f
        updateStats()
        pendingEvolution = true
    }

    fun testLevelUp() {
        levelUp()
    }

    fun testLevelDown() {
        if (level > 1) {
            level--
            experience = 0f
            scale /= 1.025f
            updateStats()
            pendingEvolution = true
        }
    }

    private fun updateStats() {
        maxHealth = level * 10f
        maxShield = level * 10f
        health = maxHealth
        shield = maxShield

        // Task: Overall increase in max speed as ships evolve
        // Level 1: 1.0x, Level 10: ~1.1x, Level 100: 2.0x
        val speedMultiplier = 1.0f + (level - 1) / 99.0f
        baseMaxSpeed = 12f * speedMultiplier
    }

    fun checkEvolution(): Boolean {
        val evolve = pendingEvolution
        if (pendingEvolution) {
            updateFixtureScale()
            evolveShip()
            pendingEvolution = false
        }
        return evolve
    }

    private fun updateFixtureScale() {
        val fixtures = com.badlogic.gdx.utils.Array<Fixture>()
        for (f in body.fixtureList) {
            fixtures.add(f)
        }
        for (f in fixtures) {
            body.destroyFixture(f)
        }

        val shape = CircleShape()
        shape.radius = 0.6f * scale
        body.createFixture(shape, 1f)
        shape.dispose()

        // Update damping based on new scale: larger = maintains momentum more
        body.linearDamping = 1.0f / scale
    }
}
