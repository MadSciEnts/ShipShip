package com.github.ships.core.weapons

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.World
import com.github.ships.core.entities.Projectile
import com.github.ships.core.entities.Ship

enum class Rarity(val color: Color) {
    COMMON(Color.GRAY),
    RARE(Color.BLUE),
    EPIC(Color.PURPLE),
    LEGENDARY(Color.GOLD)
}

abstract class Weapon(
    val name: String,
    val rarity: Rarity,
    var cooldown: Float = 0.5f
) {
    var timer: Float = 0f

    fun update(dt: Float) {
        if (timer > 0) timer -= dt
    }

    fun canFire(): Boolean = timer <= 0

    abstract fun fire(world: World, owner: Ship, origin: Vector2, direction: Vector2, onProjectileCreated: (Projectile) -> Unit)
}
