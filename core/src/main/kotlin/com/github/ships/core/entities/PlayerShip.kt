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

    init {
        level = 1
        updateStats()
        createBody()
        baseMaxSpeed = 12f
    }

    override fun createBody() {
        val bodyDef = BodyDef().apply {
            type = BodyDef.BodyType.DynamicBody
            position.set(x, y)
            linearDamping = 1.0f
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
    }

    fun applyInput(moveX: Float, moveY: Float, dt: Float) {
        val forceMultiplier = 30f * scale
        val force = Vector2(moveX, moveY).scl(forceMultiplier)
        body.applyForceToCenter(force, true)

        if (force.len2() > 0.01f) {
            val targetAngle = force.angleRad()

            // Slow down rotation as level increases
            // Base lerp factor 0.15f, reduced as level grows
            val rotationSpeedFactor = 1.0f / (1.0f + (level - 1) * 0.05f)
            val lerpFactor = 0.15f * rotationSpeedFactor

            var currentAngle = body.angle
            // Normalize angle using MathUtils constants
            while (currentAngle < -MathUtils.PI) currentAngle += MathUtils.PI * 2f
            while (currentAngle > MathUtils.PI) currentAngle -= MathUtils.PI * 2f

            val newAngle = MathUtils.lerpAngle(currentAngle, targetAngle, lerpFactor)
            body.setTransform(body.position, newAngle)
        }
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
    }

    fun checkEvolution(): Boolean {
        val evolve = pendingEvolution
        if (pendingEvolution) {
            updateFixtureScale()
            evolveShip() // Procedural texture redraw
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
    }
}
