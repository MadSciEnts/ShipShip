package com.github.ships.core.entities

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.Body
import com.badlogic.gdx.physics.box2d.World
import com.github.ships.core.utils.MotionTrail
import com.github.ships.core.utils.ProceduralTextureGenerator
import com.github.ships.core.utils.VentingAtmos
import com.github.ships.core.weapons.ProjectileWeapon
import com.github.ships.core.weapons.Rarity
import com.github.ships.core.weapons.Weapon

abstract class Ship(val world: World, val x: Float, val y: Float, val shipColor: Color) {
    var health: Float = 100f
    var maxHealth: Float = 100f
    var shield: Float = 50f
    var maxShield: Float = 50f
    var level: Int = 1

    lateinit var body: Body
    // MANAGED TEXTURE: Keep pixmap to allow context recovery on Android
    protected var pixmap: Pixmap = ProceduralTextureGenerator.createShipPixmap(64, 64, shipColor, 1)
    protected var texture: Texture = Texture(pixmap)
    val trail = MotionTrail(if (this is PlayerShip) Color.CYAN.cpy() else Color(1f, 0.2f, 0.2f, 1f))

    // Public for projectile access
    val ventingAtmos = VentingAtmos()
    protected val ventingEffects = mutableListOf<VentingAtmos>()

    var scale: Float = 1.0f
    var baseMaxSpeed: Float = 6f

    protected var fireSequenceIndex = 0
    protected var shipFireTimer = 0f

    var chargeTime = 0f
    var isCharging = false

    // Task: Shield recharges in 5s at level 50, 10s at level 100.
    // Time = max(5, Level / 10). Rate = maxShield / Time.
    open val shieldRechargeRate: Float
        get() {
            val rechargeTime = Math.max(5.0f, level / 10.0f)
            return maxShield / rechargeTime
        }

    val shieldDrainRate: Float get() = maxShield / 5.0f

    val activeBeams = mutableListOf<BeamData>()
    protected var candleFlickerTimer = 0f
    protected var currentFlickerColor = Color.RED

    // Pulse logic for signaling
    protected var signalPulseTimer = 0f
    var isSignalingRepair = false
    var isRepairingAlly = false

    protected class DamagePoint(val localPos: Vector2, val baseSize: Float, val timerOffset: Float, val popInterval: Float, var ventingAtmos: VentingAtmos? = null)
    protected val damageImpacts = mutableListOf<DamagePoint>()

    protected class DebrisParticle(var localPos: Vector2, val velocity: Vector2, var life: Float, val size: Float)
    protected val activeDebris = mutableListOf<DebrisParticle>()
    protected var debrisTimer = 0f

    class BeamData(val start: Vector2, val end: Vector2, val color: Color, var life: Float, var maxLife: Float, val baseWidth: Float)

    open val maxSpeed: Float
        get() = baseMaxSpeed * scale

    val weaponPorts = mutableListOf<Weapon>()
    var maxPorts = 1

    abstract fun createBody()

    init {
        texture.draw(pixmap, 0, 0)
    }

    open fun update(dt: Float) {
        maxPorts = 1 + (level - 1) / 5
        val baseCooldown = if (this is PlayerShip) 0.4f else 1.2f

        while (weaponPorts.size < maxPorts) {
            weaponPorts.add(ProjectileWeapon(Rarity.COMMON, baseCooldown))
        }
        while (weaponPorts.size > maxPorts) {
            weaponPorts.removeAt(weaponPorts.size - 1)
        }

        for (i in 0 until weaponPorts.size) {
            weaponPorts[i].update(dt)
        }

        if (shipFireTimer > 0) shipFireTimer -= dt

        if (isCharging && shield > 0) {
            val drain = shieldDrainRate * dt
            shield = Math.max(0f, shield - drain)
            chargeTime += dt
        } else {
            isCharging = false
            if (shield < maxShield) {
                shield = MathUtils.clamp(shield + shieldRechargeRate * dt, 0f, maxShield)
            }
        }

        // Velocity Enforcement: Slower when damaged (proportional to health %)
        val healthPercent = health / maxHealth
        // Task: Enemy ships 1/2 as fast as base (handled in EnemyShip.init)
        // Here we apply the damage penalty: speed ranges from 20% to 100% of max
        val speedPenalty = 0.2f + 0.8f * healthPercent
        val currentMax = maxSpeed * speedPenalty

        val vel = body.linearVelocity
        if (vel.len2() > currentMax * currentMax) {
            body.linearVelocity = vel.nor().scl(currentMax)
        }

        val progress = MathUtils.clamp((level - 1) / 49f, 0f, 1f)
        val lengthScale = 1f + progress * 2f
        val shipH = 1.5f * scale * lengthScale
        val rearOffset = Vector2(-shipH * 0.45f, 0f).rotateRad(body.angle)

        // Motion Trail Stutter logic: update only if health allows
        // Healthy = update every frame. Damaged = skip frames based on health %
        val stutterChance = healthPercent * 0.8f + 0.2f
        if (MathUtils.random() < stutterChance) {
            trail.update(dt, body.position.cpy().add(rearOffset))
        }

        // Idle update for projectile atmos
        ventingAtmos.update(dt, body.position)

        // Update individual Venting Atmosphere effects from damage
        val visualAngle = body.angle - (90f * MathUtils.degreesToRadians)
        val vIter = ventingEffects.iterator()
        while(vIter.hasNext()) {
            if (vIter.next().isFinished()) vIter.remove()
        }

        for (impact in damageImpacts) {
            val vAtmos = impact.ventingAtmos
            if (vAtmos != null) {
                val centeredPos = impact.localPos.cpy().scl(0.8f)
                val worldImpactPos = body.position.cpy().add(centeredPos.rotateRad(visualAngle))
                val ventDir = centeredPos.cpy().rotateRad(visualAngle).nor()
                val ventVelocity = ventDir.scl(5f)
                vAtmos.update(dt, worldImpactPos, ventVelocity)
                if (vAtmos.isFinished()) {
                    impact.ventingAtmos = null
                }
            }
        }

        val iter = activeBeams.iterator()
        while (iter.hasNext()) {
            val b = iter.next()
            b.life -= dt
            if (b.life <= 0) iter.remove()
        }

        if (damageImpacts.isNotEmpty()) {
            debrisTimer += dt
            if (debrisTimer >= 0.6f) { // ~1.6 per second
                debrisTimer = 0f
                val point = damageImpacts[MathUtils.random(damageImpacts.size - 1)]
                activeDebris.add(DebrisParticle(
                    point.localPos.cpy(),
                    Vector2(MathUtils.random(-0.5f, 0.5f), MathUtils.random(-0.5f, 0.5f)),
                    MathUtils.random(1f, 3f),
                    MathUtils.random(2f, 5f)
                ))
            }
        }

        val dIter = activeDebris.iterator()
        while (dIter.hasNext()) {
            val d = dIter.next()
            d.life -= dt
            d.localPos.add(d.velocity.x * dt, d.velocity.y * dt)
            if (d.life <= 0) dIter.remove()
        }

        candleFlickerTimer += dt
        if (candleFlickerTimer >= 0.1f) {
            candleFlickerTimer = 0f
            currentFlickerColor = when(MathUtils.random(2)) {
                0 -> Color.RED
                1 -> Color(0.8f, 0.8f, 0f, 0.8f) // Yellow
                else -> Color(0.8f, 0.5f, 0.2f, 1f) // Molten Orange
            }
        }

        // Signal pulse update
        signalPulseTimer += dt
        if (signalPulseTimer > 1.0f) signalPulseTimer = 0f
    }

    fun receiveHeal(amount: Float) {
        val oldHealth = health
        health = MathUtils.clamp(health + amount, 0f, maxHealth)
        val healPercent = (health - oldHealth) / maxHealth

        if (healPercent > 0 && damageImpacts.isNotEmpty()) {
            val numToRemove = MathUtils.ceil(damageImpacts.size * healPercent)
            for (i in 0 until numToRemove) {
                if (damageImpacts.isNotEmpty()) damageImpacts.removeAt(0)
            }
        }
        if (healPercent > 0 && activeDebris.isNotEmpty()) {
            val numToRemove = MathUtils.ceil(activeDebris.size * healPercent)
            for (i in 0 until numToRemove) {
                if (activeDebris.isNotEmpty()) activeDebris.removeAt(0)
            }
        }
    }

    open fun render(batch: SpriteBatch) {
        val pos = body.position
        val angleDeg = body.angle * MathUtils.radiansToDegrees - 90f

        val progress = MathUtils.clamp((level - 1) / 49f, 0f, 1f)
        val lengthScale = 1f + progress * 2f

        val width = 1.5f * scale
        val height = 1.5f * scale * lengthScale

        batch.draw(texture, pos.x - width / 2, pos.y - height / 2,
            width / 2, height / 2,
            width, height,
            1f, 1f, angleDeg, 0, 0, texture.width, texture.height, false, false)

        renderWeaponPods(batch, width, height, angleDeg)
    }

    fun renderDamageArtifacts(shapeRenderer: ShapeRenderer, unitsPerPixel: Float) {
        if (damageImpacts.isEmpty() && activeDebris.isEmpty()) return

        val stateTime = Gdx.graphics.frameId * 0.016f
        val visualAngle = body.angle - (90f * MathUtils.degreesToRadians)

        for (impact in damageImpacts) {
            val time = (stateTime + impact.timerOffset)
            val progress = (time % impact.popInterval) / impact.popInterval
            val dynamicScale = 0.25f + progress * 0.75f
            val currentSize = impact.baseSize * dynamicScale

            val centeredPos = impact.localPos.cpy().scl(0.8f)
            val worldPos = body.position.cpy().add(centeredPos.rotateRad(visualAngle))

            val flickerColor = if (progress < 0.5f) {
                Color.WHITE.cpy().lerp(Color.YELLOW, progress * 2f)
            } else {
                Color.YELLOW.cpy().lerp(Color.RED, (progress - 0.5f) * 2f)
            }

            shapeRenderer.setColor(flickerColor)
            shapeRenderer.circle(worldPos.x, worldPos.y, currentSize, 8)
        }

        shapeRenderer.setColor(Color.GRAY)
        for (d in activeDebris) {
            val centeredPos = d.localPos.cpy().scl(0.8f)
            val worldPos = body.position.cpy().add(centeredPos.rotateRad(visualAngle))
            val renderSize = d.size * unitsPerPixel
            shapeRenderer.rect(worldPos.x - renderSize/2, worldPos.y - renderSize/2, renderSize, renderSize)
        }
    }

    fun renderBeams(shapeRenderer: ShapeRenderer) {
        if (activeBeams.isEmpty()) return
        for (beam in activeBeams) {
            val progress = 1.0f - (beam.life / beam.maxLife)
            val dynamicWidth = beam.baseWidth * MathUtils.sin(progress * MathUtils.PI)
            val alpha = beam.life / beam.maxLife
            shapeRenderer.setColor(beam.color.r, beam.color.g, beam.color.b, beam.color.a * alpha)
            shapeRenderer.rectLine(beam.start.x, beam.start.y, beam.end.x, beam.end.y, dynamicWidth)
            shapeRenderer.setColor(Color.WHITE.r, Color.WHITE.g, Color.WHITE.b, alpha)
            shapeRenderer.rectLine(beam.start.x, beam.start.y, beam.end.x, beam.end.y, dynamicWidth * 0.3f)
        }
    }

    fun renderSignalPulses(shapeRenderer: ShapeRenderer) {
        if (!isSignalingRepair && !isRepairingAlly) return

        val pos = body.position
        val baseRadius = 2.0f * scale
        val pulseRadius = baseRadius + (signalPulseTimer * 3.0f * scale)
        val alpha = 1.0f - signalPulseTimer

        if (isSignalingRepair) {
            shapeRenderer.setColor(1f, 0f, 0f, alpha * 0.6f)
            shapeRenderer.circle(pos.x, pos.y, pulseRadius, 30)
        }

        if (isRepairingAlly) {
            shapeRenderer.setColor(0f, 0.6f, 1f, alpha * 0.6f)
            shapeRenderer.circle(pos.x, pos.y, pulseRadius, 30)
        }
    }

    protected fun getPodWorldPosition(index: Int, shipW: Float, shipH: Float, bodyAngleRad: Float): Vector2 {
        val hasNosePort = maxPorts % 2 != 0
        val localForward: Float
        val localSide: Float
        if (hasNosePort && index == 0) {
            localForward = shipH * 0.45f
            localSide = 0f
        } else {
            val actualIndex = if (hasNosePort) index - 1 else index
            val podsPerSide = (if (hasNosePort) maxPorts - 1 else maxPorts) / 2
            val sideSign = if (actualIndex % 2 == 0) 1f else -1f
            val sideIndex = actualIndex / 2
            val progress = MathUtils.clamp((level - 1) / 49f, 0f, 1f)
            val t = if (podsPerSide > 1) sideIndex.toFloat() / (podsPerSide - 1).toFloat() else 0.5f
            val yFactor = (t - 0.5f) * 0.8f
            localForward = shipH * yFactor
            val triangleWidthFactor = (0.5f - yFactor)
            localSide = (shipW / 2f) * sideSign * MathUtils.lerp(triangleWidthFactor, 1f, progress)
        }
        val worldOffset = Vector2(localForward, -localSide).rotateRad(bodyAngleRad)
        return body.position.cpy().add(worldOffset)
    }

    private fun renderWeaponPods(batch: SpriteBatch, shipW: Float, shipH: Float, angleDeg: Float) {
        if (weaponPorts.isEmpty()) return
        val white = ProceduralTextureGenerator.createWhitePixel()
        val chargeRatio = MathUtils.clamp(chargeTime / 5.0f, 0f, 1f)
        for (i in 0 until weaponPorts.size) {
            val podPos = getPodWorldPosition(i, shipW, shipH, body.angle)
            val podScale = 1.0f + chargeRatio * 1.0f
            val podW = 0.25f * scale * podScale
            val podW_final = podW
            val podH = 0.4f * scale * podScale
            val rx = MathUtils.random(-0.05f, 0.05f) * scale * chargeRatio
            val ry = MathUtils.random(-0.05f, 0.05f) * scale * chargeRatio
            val levelHue = (level * 10f) % 360f
            val targetColor = Color().fromHsv(levelHue, 0.8f, 1f)
            val basePodColor = Color.GRAY.cpy().lerp(targetColor, chargeRatio)
            batch.setColor(basePodColor)
            batch.draw(white, podPos.x - podW_final/2 + rx, podPos.y - podH/2 + ry, podW_final/2, podH/2, podW_final, podH, 1f, 1f, angleDeg)
            batch.setColor(if (chargeRatio > 0.1f) Color.WHITE else Color.LIGHT_GRAY)
            batch.draw(white, podPos.x - podW_final/4 + rx, podPos.y - podH/4 + ry, podW_final/4, podH/4, podW_final/2, podH/2, 1f, 1f, angleDeg)
        }
        batch.setColor(Color.WHITE)
    }

    fun renderTrail(shapeRenderer: ShapeRenderer) {
        trail.render(shapeRenderer, 1.5f * scale)
        for (effect in ventingEffects) {
            effect.render(shapeRenderer, 1.5f * scale)
        }
    }

    fun fireWeapons(defaultTargetDir: Vector2, onProjectileCreated: (Projectile) -> Unit, potentialTargets: List<Ship>) {
        if (weaponPorts.isEmpty()) return
        val baseCooldown = if (this is PlayerShip) 0.4f else 1.2f
        val shotInterval = baseCooldown / weaponPorts.size
        if (shipFireTimer <= 0) {
            if (fireSequenceIndex >= weaponPorts.size) fireSequenceIndex = 0
            val weapon = weaponPorts[fireSequenceIndex]
            if (weapon.canFire()) {
                val progress = MathUtils.clamp((level - 1) / 49f, 0f, 1f)
                val lengthScale = 1f + progress * 2f
                val shipW = 1.5f * scale
                val shipH = 1.5f * scale * lengthScale
                val fireOrigin = getPodWorldPosition(fireSequenceIndex, shipW, shipH, body.angle)
                val hasNose = maxPorts % 2 != 0
                val podSide = if (hasNose && fireSequenceIndex == 0) 0f else if ((fireSequenceIndex - (if (hasNose) 1 else 0)) % 2 == 0) 1f else -1f
                val fireDir = findSemiRandomTarget(fireOrigin, podSide, body.angle, potentialTargets) ?: defaultTargetDir
                weapon.fire(world, this, fireOrigin, fireDir, onProjectileCreated)
                fireSequenceIndex++
                shipFireTimer = shotInterval
            }
        }
    }

    fun fireChargedAttack(onProjectileCreated: (Projectile) -> Unit, potentialTargets: List<Ship>) {
        if (chargeTime < 0.5f) {
            chargeTime = 0f
            isCharging = false
            return
        }
        val multiplier = chargeTime
        val progress = MathUtils.clamp((level - 1) / 49f, 0f, 1f)
        val lengthScale = 1f + progress * 2f
        val shipW = 1.5f * scale
        val shipH = 1.5f * scale * lengthScale
        val levelHue = (level * 10f) % 360f
        val saturation = 1.0f - (chargeTime / 5.0f) * 0.8f
        val beamColor = Color().fromHsv(levelHue, saturation.coerceIn(0.1f, 1.0f), 1f)
        beamColor.a = 0.8f
        for (i in 0 until weaponPorts.size) {
            val fireOrigin = getPodWorldPosition(i, shipW, shipH, body.angle)
            val hasNose = maxPorts % 2 != 0
            val podSide = if (hasNose && i == 0) 0f else if ((i - (if (hasNose) 1 else 0)) % 2 == 0) 1f else -1f
            val fireDir = findSemiRandomTarget(fireOrigin, podSide, body.angle, potentialTargets) ?: Vector2(MathUtils.cos(body.angle), MathUtils.sin(body.angle))
            val beamEnd = fireOrigin.cpy().add(fireDir.cpy().scl(30f * scale))
            var finalEnd = beamEnd.cpy()
            world.rayCast({ fixture, point, _, fraction ->
                val userData = fixture.body.userData
                if (userData is Ship && userData != this) {
                    userData.takeDamage(level.toFloat() * multiplier * 2f, point)
                    finalEnd.set(point)
                    return@rayCast fraction
                }
                -1f
            }, fireOrigin, beamEnd)
            activeBeams.add(BeamData(fireOrigin.cpy(), finalEnd, beamColor.cpy(), 0.5f, 0.5f, 0.25f * scale * (1f + multiplier * 0.2f)))
        }
        chargeTime = 0f
        isCharging = false
    }

    private fun findBestSideTarget(origin: Vector2, side: Float, bodyAngleRad: Float, targets: List<Ship>): Vector2? {
        var closest: Ship? = null
        var minDist = Float.MAX_VALUE
        val forward = Vector2(MathUtils.cos(bodyAngleRad), MathUtils.sin(bodyAngleRad))
        val right = forward.cpy().rotate90(-1)
        for (target in targets) {
            if (target == this || target.health <= 0) continue
            val toTarget = target.body.position.cpy().sub(origin)
            val dist = toTarget.len2()
            if (side != 0f) {
                val dot = toTarget.dot(right)
                if ((side > 0 && dot <= 0) || (side < 0 && dot >= 0)) continue
            }
            if (dist < minDist) {
                minDist = dist
                closest = target
            }
        }
        return closest?.let { it.body.position.cpy().sub(origin).nor() }
    }

    private fun findSemiRandomTarget(origin: Vector2, side: Float, bodyAngleRad: Float, targets: List<Ship>): Vector2? {
        var closest: Ship? = null
        var minDist = Float.MAX_VALUE
        val forward = Vector2(MathUtils.cos(bodyAngleRad), MathUtils.sin(bodyAngleRad))
        val right = forward.cpy().rotate90(-1)
        for (target in targets) {
            if (target == this || target.health <= 0) continue
            val toTarget = target.body.position.cpy().sub(origin)
            val dist = toTarget.len2()
            if (side != 0f) {
                val dot = toTarget.dot(right)
                if ((side > 0 && dot <= 0) || (side < 0 && dot >= 0)) continue
            }
            if (dist < minDist) {
                minDist = dist
                closest = target
            }
        }

        return closest?.let { target ->
            val toTarget = target.body.position.cpy().sub(origin)
            val dist = toTarget.len()
            val targetDir = toTarget.scl(1f / dist)
            val halfShipSize = (1.5f * target.scale) / 2f
            val maxAngleFromDistance = MathUtils.atan2(halfShipSize, dist) * MathUtils.radiansToDegrees
            val maxSpread = Math.min(15f, maxAngleFromDistance)
            targetDir.rotateDeg(MathUtils.random(-maxSpread, maxSpread))
            targetDir.nor()
        }
    }

    open fun takeDamage(amount: Float, impactPoint: Vector2? = null) {
        val shieldHit = shield > 0
        if (shieldHit) {
            shield -= amount
            if (shield < 0) {
                health += shield
                shield = 0f
            }
        } else {
            health -= amount
            if (impactPoint != null) {
                val visualAngle = body.angle - (90f * MathUtils.degreesToRadians)
                val localImpact = impactPoint.cpy().sub(body.position).rotateRad(-visualAngle)

                val progress = MathUtils.clamp((level - 1) / 49f, 0f, 1f)
                val hullW = (1.5f * scale) / 2f
                val hullH = (1.5f * scale * (1f + progress * 2f)) / 2f

                localImpact.x = MathUtils.clamp(localImpact.x, -hullW, hullW)
                localImpact.y = MathUtils.clamp(localImpact.y, -hullH, hullH)

                damageImpacts.add(DamagePoint(localImpact, Math.max(0.15f, 0.2f * scale), MathUtils.random(100f), MathUtils.random(0.05f, 0.15f)))

                if (ventingEffects.size < 3) {
                    val atmos = VentingAtmos()
                    damageImpacts.last().ventingAtmos = atmos
                    ventingEffects.add(atmos)
                }
            }
        }
    }

    open fun die() {}
    fun dispose() {
        texture.dispose()
        pixmap.dispose()
    }
    fun evolveShip() {
        pixmap.dispose()
        texture.dispose()
        pixmap = ProceduralTextureGenerator.createShipPixmap(64, 64, shipColor, level)
        texture = Texture(pixmap)
        damageImpacts.clear()
        ventingEffects.clear()
    }
}
