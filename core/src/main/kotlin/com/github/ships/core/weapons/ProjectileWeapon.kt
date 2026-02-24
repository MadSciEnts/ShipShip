package com.github.ships.core.weapons

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.World
import com.github.ships.core.entities.EnemyShip
import com.github.ships.core.entities.PlayerShip
import com.github.ships.core.entities.Projectile
import com.github.ships.core.entities.Ship

class ProjectileWeapon(rarity: Rarity, cooldown: Float = 0.2f) : Weapon("Auto-Cannon", rarity, cooldown) {
    override fun fire(world: World, owner: Ship, origin: Vector2, direction: Vector2, onProjectileCreated: (Projectile) -> Unit) {
        if (!canFire()) return

        val projectileColor = Color(1f, 1f, 1f, 1f)
        val damage = owner.level.toFloat()
        val screenSpeed: Float
        var lengthMult = 1.0f

        if (owner is PlayerShip) {
            val hue = (owner.level * 10f) % 360f
            val variationRange = ((owner.level - 1) % 100) / 100f
            val finalHue = (hue + MathUtils.random(-180f, 180f) * variationRange + 360f) % 360f
            projectileColor.fromHsv(finalHue, 0.3f, 1f)
            projectileColor.a = 1f
            // Doubled player projectile speed again as requested (now 12x base)
            screenSpeed = (0.8f + (damage * 0.05f)) * 12f
        } else if (owner is EnemyShip) {
            projectileColor.fromHsv(0f, 0.3f, 1f)
            projectileColor.a = 1f
            screenSpeed = 0.3f
            // Enemy projectiles are half as long
            lengthMult = 0.5f
        } else {
            projectileColor.set(rarity.color)
            screenSpeed = 0.5f
        }

        val p = Projectile(world, origin.x, origin.y, direction, screenSpeed, damage, projectileColor, owner, lengthMult)
        onProjectileCreated(p)
        timer = cooldown
    }
}
