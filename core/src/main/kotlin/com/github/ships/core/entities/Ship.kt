package com.github.ships.core.entities

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
    protected var pixmap: Pixmap = ProceduralTextureGenerator.createShipPixmap(64, 64, shipColor, 1)
    protected var texture: Texture = Texture(pixmap)
    val trail = MotionTrail(shipColor)

    var scale: Float = 1.0f
    var baseMaxSpeed: Float = 6f

    protected var fireSequenceIndex = 0
    protected var shipFireTimer = 0f

    var chargeTime = 0f
    var isCharging = false
    val shieldRechargeRate = 2f

    val shieldDrainRate: Float
        get() = maxShield / 5.0f

    private val activeBeams = mutableListOf<BeamData>()
    private var candleFlickerTimer = 0f
    private var currentFlickerColor = Color.RED

    private class BeamData(val start: Vector2, val end: Vector2, val color: Color, var life: Float, var maxLife: Float, val baseWidth: Float)

    open val maxSpeed: Float
        get() = baseMaxSpeed * scale

    val weaponPorts = mutableListOf<Weapon>()
    var maxPorts = 1

    abstract fun createBody()

    open fun update(dt: Float) {
        maxPorts = 1 + (level - 1) / 5
        val baseCooldown = if (this is PlayerShip) 0.4f else 1.0f

        while (weaponPorts.size < maxPorts) {
            weaponPorts.add(ProjectileWeapon(Rarity.COMMON, baseCooldown))
        }
        while (weaponPorts.size > maxPorts) {
            weaponPorts.removeAt(weaponPorts.size - 1)
        }

        weaponPorts.forEach { it.update(dt) }

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

        val vel = body.linearVelocity
        val currentMax = maxSpeed
        if (vel.len2() > currentMax * currentMax) {
            body.linearVelocity = vel.nor().scl(currentMax)
        }

        val progress = MathUtils.clamp((level - 1) / 49f, 0f, 1f)
        val lengthScale = 1f + progress * 2f
        val shipH = 1.5f * scale * lengthScale
        val rearOffset = Vector2(-shipH * 0.45f, 0f).rotateRad(body.angle)
        trail.update(dt, body.position.cpy().add(rearOffset))

        val iter = activeBeams.iterator()
        while (iter.hasNext()) {
            val b = iter.next()
            b.life -= dt
            if (b.life <= 0) iter.remove()
        }

        candleFlickerTimer += dt
        if (candleFlickerTimer >= 0.1f) {
            candleFlickerTimer = 0f
            currentFlickerColor = when(MathUtils.random(2)) {
                0 -> Color.RED
                1 -> Color.YELLOW
                else -> Color.WHITE
            }
            refreshDamageVisuals = true
        }
    }

    private var refreshDamageVisuals = false

    open fun render(batch: SpriteBatch) {
        if (refreshDamageVisuals) {
            updateDamageTextureColors()
            refreshDamageVisuals = false
        }

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

    private fun updateDamageTextureColors() {
        var changed = false
        val flickerColorInt = Color.rgba8888(currentFlickerColor)

        for (y in 0 until pixmap.height) {
            for (x in 0 until pixmap.width) {
                val pixel = pixmap.getPixel(x, y)
                if (isDamagePixel(pixel)) {
                    pixmap.drawPixel(x, y, flickerColorInt)
                    changed = true
                }
            }
        }
        if (changed) texture.draw(pixmap, 0, 0)
    }

    private fun isDamagePixel(rgba: Int): Boolean {
        val r = (rgba ushr 24) and 0xff
        val g = (rgba ushr 16) and 0xff
        val b = (rgba ushr 8) and 0xff
        val a = rgba and 0xff
        if (a == 0) return false
        return (r > 200 && g < 100 && b < 100) ||
               (r > 200 && g > 200 && b < 100) ||
               (r > 200 && g > 200 && b > 200)
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

            // Fixed: Ports now grow by 2x at max charge
            val podScale = 1.0f + chargeRatio * 1.0f
            val podW = 0.25f * scale * podScale
            val podH = 0.4f * scale * podScale

            // Fixed: Added mechanical "rattle" jitter that increases with charge
            val rattleAmount = 0.05f * scale * chargeRatio
            val rx = MathUtils.random(-rattleAmount, rattleAmount)
            val ry = MathUtils.random(-rattleAmount, rattleAmount)

            val levelHue = (level * 10f) % 360f
            val targetColor = Color().fromHsv(levelHue, 0.8f, 1f)
            val basePodColor = Color.GRAY.cpy().lerp(targetColor, chargeRatio)

            batch.setColor(basePodColor)
            batch.draw(white, podPos.x - podW/2 + rx, podPos.y - podH/2 + ry, podW/2, podH/2, podW, podH, 1f, 1f, angleDeg)
            batch.setColor(if (chargeRatio > 0.1f) Color.WHITE else Color.LIGHT_GRAY)
            batch.draw(white, podPos.x - podW/4 + rx, podPos.y - podH/4 + ry, podW/4, podH/4, podW/2, podH/2, 1f, 1f, angleDeg)
        }
        batch.setColor(Color.WHITE)
    }

    fun renderTrail(shapeRenderer: ShapeRenderer) {
        trail.render(shapeRenderer, 1.5f * scale)
    }

    fun fireWeapons(defaultTargetDir: Vector2, onProjectileCreated: (Projectile) -> Unit, potentialTargets: List<Ship>) {
        if (weaponPorts.isEmpty()) return
        val baseCooldown = if (this is PlayerShip) 0.4f else 1.0f
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
                val fireDir = findBestSideTarget(fireOrigin, podSide, body.angle, potentialTargets) ?: defaultTargetDir
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
            val fireDir = findBestSideTarget(fireOrigin, podSide, body.angle, potentialTargets) ?: Vector2(MathUtils.cos(body.angle), MathUtils.sin(body.angle))
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
                val isOnCorrectSide = if (side > 0) dot > 0 else dot < 0
                if (!isOnCorrectSide) continue
            }
            if (dist < minDist) {
                minDist = dist
                closest = target
            }
        }
        return closest?.let { it.body.position.cpy().sub(origin).nor() }
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
                damageTexture(impactPoint)
            }
        }
        if (health <= 0) die()
    }
    private fun damageTexture(impactWorldPos: Vector2) {
        val relativePos = impactWorldPos.cpy().sub(body.position)
        relativePos.rotateDeg(90f - body.angle * MathUtils.radiansToDegrees)
        val size = 1.5f * scale
        val px = ((relativePos.x / size + 0.5f) * pixmap.width).toInt()
        val py = ((0.5f - relativePos.y / size) * pixmap.height).toInt()
        pixmap.blending = Pixmap.Blending.None
        val damageSize = MathUtils.random(4, 8)
        val startX = px - damageSize / 2
        val startY = py - damageSize / 2
        for (dx in 0 until damageSize) {
            for (dy in 0 until damageSize) {
                val fx = startX + dx
                val fy = startY + dy
                if (fx >= 0 && fx < pixmap.width && fy >= 0 && fy < pixmap.height) {
                    val colorInt = pixmap.getPixel(fx, fy)
                    val alpha = (colorInt and 0x000000ff)
                    if (alpha > 0) {
                        pixmap.drawPixel(fx, fy, Color.rgba8888(currentFlickerColor))
                    }
                }
            }
        }
        texture.draw(pixmap, 0, 0)
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
    }
}
