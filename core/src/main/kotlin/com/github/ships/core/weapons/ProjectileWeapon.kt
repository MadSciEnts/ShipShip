package com.github.ships.core.weapons

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.World
import com.github.ships.core.entities.EnemyShip
import com.github.ships.core.entities.PlayerShip
import com.github.ships.core.entities.Projectile
import com.github.ships.core.entities.Ship
import com.github.ships.core.utils.ProceduralTextureGenerator

class ProjectileWeapon(rarity: Rarity, cooldown: Float = 0.2f) : Weapon("Auto-Cannon", rarity, cooldown) {
    override fun fire(world: World, owner: Ship, origin: Vector2, direction: Vector2, onProjectileCreated: (Projectile) -> Unit) {
        if (!canFire()) return

        // Task: Projectile color now matches the ship's evolution level color
        val projectileColor = ProceduralTextureGenerator.getEvolutionColor(owner.level)
        val damage = owner.level.toFloat()
        val screenSpeed: Float
        var lengthMult = 1.0f

        if (owner is PlayerShip) {
            screenSpeed = (0.8f + (damage * 0.05f)) * 12f
        } else if (owner is EnemyShip) {
            screenSpeed = 0.7f
            lengthMult = 0.5f
        } else {
            screenSpeed = 0.5f
        }

        val p = Projectile(world, origin.x, origin.y, direction, screenSpeed, damage, projectileColor, owner, lengthMult)
        onProjectileCreated(p)
        timer = cooldown
    }
}
