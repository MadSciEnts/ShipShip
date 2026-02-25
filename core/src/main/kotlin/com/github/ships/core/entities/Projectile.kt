package com.github.ships.core.entities

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.physics.box2d.*
import com.github.ships.core.utils.ProceduralTextureGenerator

class Projectile(
    val world: World,
    x: Float,
    y: Float,
    val direction: Vector2,
    val baseScreenSpeed: Float,
    var damage: Float,
    val color: Color,
    val owner: Ship,
    val chargeMultiplier: Float = 1.0f
) {
    var body: Body
    val isEnemy = owner is EnemyShip

    val texture: Texture = if (isEnemy) {
        ProceduralTextureGenerator.getRectangleTexture(20, 20)
    } else {
        // Player projectiles: reduced width from 12 to 6 to match 1/2 width request
        ProceduralTextureGenerator.getRectangleTexture(6, 40)
    }

    var active = true
    var lifeTime = if (isEnemy) 4f else 15f
    private var rotationTimer = 0f

    private val damageScale = (1.5f + (damage - 1f) * 0.1f) * chargeMultiplier

    // Player width reduced from 0.3f to 0.15f (1/2 as wide)
    private val baseWidth = (if (isEnemy) 0.5f else 0.15f) * damageScale
    private val baseHeight = (if (isEnemy) 0.5f else 1.0f) * damageScale

    init {
        // Calculate dynamic offset so the projectile spawns in front of the mount point
        // instead of centered on it. We shift the starting position forward by half its length.
        val spawnX = x + direction.x * (baseHeight / 2f)
        val spawnY = y + direction.y * (baseHeight / 2f)

        val bodyDef = BodyDef().apply {
            type = BodyDef.BodyType.DynamicBody
            position.set(spawnX, spawnY)
            bullet = true
            angle = direction.angleRad() - (Math.PI.toFloat() / 2f)
        }
        body = world.createBody(bodyDef)
        body.userData = this

        val shape = PolygonShape()
        shape.setAsBox(baseWidth / 2f, baseHeight / 2f)
        val fixtureDef = FixtureDef().apply {
            this.shape = shape
            isSensor = true
        }
        body.createFixture(fixtureDef)
        shape.dispose()
    }

    fun update(dt: Float, cameraPos: Vector3, zoom: Float, worldWidth: Float, worldHeight: Float) {
        lifeTime -= dt

        val worldVisibleW = worldWidth * zoom
        val worldVisibleH = worldHeight * zoom
        val left = cameraPos.x - worldVisibleW / 2f
        val right = cameraPos.x + worldVisibleW / 2f
        val bottom = cameraPos.y - worldVisibleH / 2f
        val top = cameraPos.y + worldVisibleH / 2f

        val isOffScreen = body.position.x < left || body.position.x > right ||
                          body.position.y < bottom || body.position.y > top

        if (isEnemy) {
            if (lifeTime <= 0) active = false
            rotationTimer += dt * 500f
        } else {
            if (isOffScreen) active = false
            if (lifeTime <= 0) active = false
        }

        val worldSpeed = baseScreenSpeed * worldVisibleW
        body.setLinearVelocity(direction.x * worldSpeed, direction.y * worldSpeed)
    }

    /**
     * NOTE FOR FUTURE SELF / AGENTS:
     * The "Black Box" bug was fixed by refactoring the ProceduralTextureGenerator to create
     * pure WHITE base textures. We then apply the projectile's specific color using
     * batch.setColor(renderColor) during this render pass.
     *
     * WHY THIS WORKS:
     * Previously, colors were baked into the texture pixels. Tinting a colored texture with
     * another color resulted in "Color Multiplication" (e.g., Red * Red = Dark Red), which
     * often collapsed into black on certain mobile GPU drivers. Using a White base ensures
     * the mathematical identity (White * Color = Color), preserving full vibrance.
     *
     * We also explicitly reset batch.setColor(Color.WHITE) at the end of this function to
     * prevent "Color Leaks" where the projectile color would stain subsequent draw calls (like ships).
     */
    fun render(batch: SpriteBatch, zoom: Float, worldWidth: Float) {
        val angle = if (isEnemy) rotationTimer else body.angle * 57.295776f

        val unitsPerPixel = (worldWidth * zoom) / Gdx.graphics.width.toFloat()
        val minWorldSize = 5.0f * unitsPerPixel

        val renderWidth = Math.max(baseWidth, minWorldSize)
        val renderHeight = Math.max(baseHeight, minWorldSize * (baseHeight / baseWidth))

        val renderColor = color.cpy()
        if (renderColor.a < 0.5f) renderColor.a = 0.5f

        batch.setColor(renderColor)
        batch.draw(texture, body.position.x - renderWidth / 2f, body.position.y - renderHeight / 2f,
            renderWidth / 2f, renderHeight / 2f,
            renderWidth, renderHeight,
            1f, 1f, angle, 0, 0, texture.width, texture.height, false, false)
        batch.setColor(Color.WHITE)
    }

    fun dispose() {
    }
}
