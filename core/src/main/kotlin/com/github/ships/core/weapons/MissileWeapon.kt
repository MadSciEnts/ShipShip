package com.github.ships.core.weapons

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.World
import com.github.ships.core.entities.Missile
import com.github.ships.core.entities.Projectile
import com.github.ships.core.entities.Ship

class MissileWeapon(rarity: Rarity) : Weapon("Homing Missile", rarity, 1.5f) {
    override fun fire(world: World, owner: Ship, origin: Vector2, direction: Vector2, onProjectileCreated: (Projectile) -> Unit) {
        if (!canFire()) return

        // Find a target (simple nearest enemy check would be better, for now just pass null or find first)
        // This is a placeholder for target selection logic
        // val missile = Missile(world, origin.x, origin.y, null, 8f, 50f, rarity.color)
        // Since Missile is not a Projectile, we need to handle it separately or make them share a base

        timer = cooldown
    }
}
