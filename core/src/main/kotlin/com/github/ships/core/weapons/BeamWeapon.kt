package com.github.ships.core.weapons

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.World
import com.github.ships.core.entities.Projectile
import com.github.ships.core.entities.Ship

class BeamWeapon(rarity: Rarity) : Weapon("Ion Beam", rarity, 0.1f) {
    var isFiring = false
    private val beamLength = 10f

    override fun fire(world: World, owner: Ship, origin: Vector2, direction: Vector2, onProjectileCreated: (Projectile) -> Unit) {
        if (!canFire()) return
        isFiring = true
        // Beam damage logic usually involves raycasting
        world.rayCast({ fixture, point, normal, fraction ->
            val userData = fixture.body.userData
            if (userData is Ship && userData != owner) {
                userData.takeDamage(1f) // Continuous small damage
                return@rayCast fraction
            }
            -1f
        }, origin, origin.cpy().add(direction.cpy().scl(beamLength)))

        timer = cooldown
    }
}
