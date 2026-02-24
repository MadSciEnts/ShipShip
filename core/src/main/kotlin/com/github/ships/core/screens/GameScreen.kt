package com.github.ships.core.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.physics.box2d.*
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.ui.Touchpad
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.github.ships.core.ShipShipGame
import com.github.ships.core.entities.EnemyShip
import com.github.ships.core.entities.PlayerShip
import com.github.ships.core.entities.Projectile
import com.github.ships.core.entities.Ship
import com.github.ships.core.utils.EffectManager
import com.github.ships.core.utils.ProceduralTextureGenerator
import com.github.ships.core.utils.Starfield
import com.github.ships.core.weapons.ProjectileWeapon
import com.github.ships.core.weapons.Rarity

class GameScreen(val game: ShipShipGame) : Screen {
    private val batch = SpriteBatch()
    private val shapeRenderer = ShapeRenderer()
    private val world = World(Vector2(0f, 0f), true)
    private val camera = OrthographicCamera()
    private val viewport = ExtendViewport(30f, 30f, camera)

    private val player = PlayerShip(world, 0f, 0f)
    private val enemies = Array<EnemyShip>()
    private val projectiles = Array<Projectile>()
    private val starfield = Starfield(300)
    private val effectManager = EffectManager()

    private val uiStage = Stage(ScreenViewport())
    private lateinit var touchpad: Touchpad
    private lateinit var fireButton: TextButton
    private lateinit var chargeButton: TextButton
    private lateinit var levelUpBtn: TextButton
    private lateinit var levelDownBtn: TextButton
    private lateinit var posLabel: Label
    private var isFiring = false
    private var isCharging = false

    private val bodiesToRemove = Array<Body>()

    init {
        setupUI()
        setupWorld()
        Gdx.input.inputProcessor = uiStage
        player.weaponPorts.add(ProjectileWeapon(Rarity.COMMON, 0.4f))
    }

    private fun setupWorld() {
        world.setContactListener(object : ContactListener {
            override fun beginContact(contact: Contact) {
                val a = contact.fixtureA.body.userData
                val b = contact.fixtureB.body.userData

                if (a is Projectile && b is Ship) handleCollision(a, b)
                else if (b is Projectile && a is Ship) handleCollision(b, a)
            }
            override fun endContact(contact: Contact) {}
            override fun preSolve(contact: Contact, oldManifold: Manifold) {}
            override fun postSolve(contact: Contact, impulse: ContactImpulse) {}
        })
    }

    private fun handleCollision(projectile: Projectile, ship: Ship) {
        if (projectile.active && projectile.owner != ship) {
            val impactPos = projectile.body.position.cpy()

            if (ship.shield > 0) {
                effectManager.spawnShieldHit(impactPos.x, impactPos.y, 0.8f * ship.scale)
            }

            ship.takeDamage(projectile.damage, impactPos)
            projectile.active = false

            // Scaled debris based on ship size (1.5 is base size)
            val shipWidth = 1.5f * ship.scale
            effectManager.spawnImpact(impactPos.x, impactPos.y, shipWidth)

            if (ship is EnemyShip && ship.health <= 0) {
                player.addExperience(50f * ship.level)
            }
        }
    }

    private fun setupUI() {
        val skin = Skin()
        val whitePixel = ProceduralTextureGenerator.createWhitePixel()
        skin.add("white", whitePixel)

        val textButtonStyle = TextButton.TextButtonStyle()
        textButtonStyle.up = com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(ProceduralTextureGenerator.createSciFiButton(250, 250, false))
        textButtonStyle.down = com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(ProceduralTextureGenerator.createSciFiButton(250, 250, true))
        textButtonStyle.font = com.badlogic.gdx.graphics.g2d.BitmapFont()
        skin.add("default", textButtonStyle)

        val testButtonStyle = TextButton.TextButtonStyle()
        testButtonStyle.up = com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(ProceduralTextureGenerator.createSciFiButton(125, 125, false))
        testButtonStyle.down = com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(ProceduralTextureGenerator.createSciFiButton(125, 125, true))
        testButtonStyle.font = textButtonStyle.font
        skin.add("test", testButtonStyle)

        val touchpadStyle = Touchpad.TouchpadStyle()
        touchpadStyle.knob = com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(ProceduralTextureGenerator.createSciFiJoystickKnob(80))
        touchpadStyle.background = com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(ProceduralTextureGenerator.createSciFiJoystickBase(250))

        touchpad = Touchpad(10f, touchpadStyle)
        touchpad.setBounds(50f, 50f, 300f, 300f)
        uiStage.addActor(touchpad)

        fireButton = TextButton("FIRE", skin)
        fireButton.addListener(object : ClickListener() {
            override fun touchDown(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int): Boolean {
                isFiring = true
                return true
            }
            override fun touchUp(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int) {
                isFiring = false
            }
        })
        uiStage.addActor(fireButton)

        chargeButton = TextButton("CHARGE", skin)
        chargeButton.addListener(object : ClickListener() {
            override fun touchDown(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int): Boolean {
                isCharging = true
                player.isCharging = true
                return true
            }
            override fun touchUp(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int) {
                isCharging = false
                player.isCharging = false
                player.fireChargedAttack({ projectiles.add(it) }, enemies.toList())
            }
        })
        uiStage.addActor(chargeButton)

        levelUpBtn = TextButton("+", skin, "test")
        levelUpBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                player.testLevelUp()
            }
        })
        uiStage.addActor(levelUpBtn)

        levelDownBtn = TextButton("-", skin, "test")
        levelDownBtn.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                player.testLevelDown()
            }
        })
        uiStage.addActor(levelDownBtn)

        val labelStyle = Label.LabelStyle()
        labelStyle.font = com.badlogic.gdx.graphics.g2d.BitmapFont()
        labelStyle.fontColor = Color.WHITE
        posLabel = Label("( 0, 0 )", labelStyle)
        posLabel.setFontScale(2f)
        uiStage.addActor(posLabel)

        updateUIElements()
    }

    private fun updateUIElements() {
        val width = Gdx.graphics.width.toFloat()
        val height = Gdx.graphics.height.toFloat()
        val fireBtnSize = 250f
        val testBtnSize = fireBtnSize / 2f
        val margin = 50f

        if (width > height) {
            fireButton.setBounds(width - fireBtnSize - margin, margin, fireBtnSize, fireBtnSize)
            chargeButton.setBounds(width - fireBtnSize - margin, margin + fireBtnSize + 20f, fireBtnSize, fireBtnSize)
        } else {
            fireButton.setBounds(width / 2f + margin, margin, fireBtnSize, fireBtnSize)
            chargeButton.setBounds(width / 2f + margin, margin + fireBtnSize + 20f, fireBtnSize, fireBtnSize)
        }

        levelUpBtn.setBounds(20f, height / 2f + testBtnSize / 2f + 10f, testBtnSize, testBtnSize)
        levelDownBtn.setBounds(20f, height / 2f - testBtnSize / 2f - 10f, testBtnSize, testBtnSize)

        posLabel.setPosition(width / 2f - posLabel.prefWidth / 2f, 20f)
    }

    private fun spawnEnemy() {
        val angle = MathUtils.random(0f, MathUtils.PI2)
        val dist = 25f * player.scale
        val ex = player.body.position.x + MathUtils.cos(angle) * dist
        val ey = player.body.position.y + MathUtils.sin(angle) * dist

        val minLvl = Math.max(1, player.level - 2)
        val maxLvl = player.level + 2
        val spawnLvl = MathUtils.random(minLvl, maxLvl)

        val e = EnemyShip(world, ex, ey, spawnLvl)
        e.onDeath = { ship ->
            // Use the new spawnExplosion logic for ship destruction
            val progress = MathUtils.clamp((ship.level - 1) / 49f, 0f, 1f)
            val lengthScale = 1f + progress * 2f
            val shipW = 1.5f * ship.scale
            val shipH = 1.5f * ship.scale * lengthScale

            effectManager.spawnExplosion(ship.body.position.x, ship.body.position.y, shipW, shipH)
        }
        enemies.add(e)
    }

    private fun findClosestEnemy(): EnemyShip? {
        var closest: EnemyShip? = null
        var minDist = 1000f * player.scale * player.scale
        for (enemy in enemies) {
            val dist = enemy.body.position.dst2(player.body.position)
            if (dist < minDist) {
                minDist = dist
                closest = enemy
            }
        }
        return closest
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        player.checkEvolution()

        if (!world.isLocked) {
            val bodyIter = bodiesToRemove.iterator()
            while(bodyIter.hasNext()) {
                world.destroyBody(bodyIter.next())
                bodyIter.remove()
            }
        }

        if (player.health <= 0) {
            game.screen = GameOverScreen(game, player.level)
            return
        }

        world.step(1/60f, 6, 2)

        if (enemies.size < 5 + player.level) spawnEnemy()

        player.update(delta)
        player.applyInput(touchpad.knobPercentX, touchpad.knobPercentY, delta)

        if (isCharging && player.shield <= 0) {
            isCharging = false
            player.isCharging = false
            player.fireChargedAttack({ projectiles.add(it) }, enemies.toList())
        }

        if (isCharging) {
            val chargeRatio = MathUtils.clamp(player.chargeTime / 5.0f, 0f, 1f)
            chargeButton.color = Color(1f, 1f - chargeRatio, 1f - chargeRatio, 1f).lerp(Color.CYAN, chargeRatio)
        } else {
            chargeButton.color = Color.WHITE
        }

        val potentialTargets = Array<Ship>()
        enemies.forEach { potentialTargets.add(it) }

        if (isFiring) {
            val closest = findClosestEnemy()
            val fireDir = if (closest != null) {
                closest.body.position.cpy().sub(player.body.position).nor()
            } else {
                Vector2(MathUtils.cos(player.body.angle), MathUtils.sin(player.body.angle))
            }
            player.fireWeapons(fireDir, { projectiles.add(it) }, potentialTargets.toList())
        }

        val projIter = projectiles.iterator()
        while (projIter.hasNext()) {
            val p = projIter.next()
            p.update(delta, camera.zoom, viewport.worldWidth)
            if (!p.active) {
                bodiesToRemove.add(p.body)
                p.texture.dispose()
                projIter.remove()
            }
        }

        val enemyIter = enemies.iterator()
        while (enemyIter.hasNext()) {
            val e = enemyIter.next()
            val targetList = Array<Ship>()
            targetList.add(player)
            e.updateAI(player, delta, { projectiles.add(it) }, targetList.toList())
            if (e.health <= 0) {
                e.die()
                bodiesToRemove.add(e.body)
                enemyIter.remove()
            }
        }

        effectManager.update(delta)

        val lerpSpeed = 3f
        camera.zoom = MathUtils.lerp(camera.zoom, player.scale, delta * lerpSpeed)
        camera.position.set(player.body.position.x, player.body.position.y, 0f)
        camera.update()

        batch.projectionMatrix = camera.combined
        batch.begin()
        starfield.render(batch, player.body.position, camera.zoom, viewport.worldWidth, viewport.worldHeight, delta)
        batch.end()

        Gdx.gl.glEnable(GL20.GL_BLEND)
        shapeRenderer.projectionMatrix = camera.combined
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        player.renderTrail(shapeRenderer)
        enemies.forEach { it.renderTrail(shapeRenderer) }
        player.renderBeams(shapeRenderer)
        shapeRenderer.end()

        batch.begin()
        player.render(batch)
        enemies.forEach { it.render(batch) }
        projectiles.forEach { it.render(batch, camera.zoom, viewport.worldWidth) }
        batch.end()

        shapeRenderer.projectionMatrix = camera.combined
        effectManager.render(shapeRenderer)

        renderEnemyHealthBars()

        if (isCharging) {
            renderEdgeGlow()
        }

        val px = Math.round(player.body.position.x)
        val py = Math.round(player.body.position.y)
        posLabel.setText("( $px, $py )")
        posLabel.setX(Gdx.graphics.width / 2f - posLabel.prefWidth / 2f)

        uiStage.act(delta)
        uiStage.draw()

        renderHUD()
        renderRadar()
    }

    private fun renderEdgeGlow() {
        val chargeRatio = MathUtils.clamp(player.chargeTime / 5.0f, 0f, 1f)
        val levelHue = (player.level * 10f) % 360f
        val glowColor = Color().fromHsv(levelHue, 0.8f, 1f)
        glowColor.a = chargeRatio * 0.4f

        Gdx.gl.glEnable(GL20.GL_BLEND)
        shapeRenderer.projectionMatrix = uiStage.viewport.camera.combined
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.setColor(glowColor)

        val thickness = 10f
        val w = Gdx.graphics.width.toFloat()
        val h = Gdx.graphics.height.toFloat()

        shapeRenderer.rect(0f, 0f, w, thickness)
        shapeRenderer.rect(0f, h - thickness, w, thickness)
        shapeRenderer.rect(0f, 0f, thickness, h)
        shapeRenderer.rect(w - thickness, 0f, thickness, h)

        shapeRenderer.end()
    }

    private fun renderEnemyHealthBars() {
        shapeRenderer.projectionMatrix = camera.combined
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        for (enemy in enemies) {
            val pos = enemy.body.position
            val width = 1.5f * camera.zoom
            val height = 0.2f * camera.zoom
            shapeRenderer.setColor(Color.GRAY)
            shapeRenderer.rect(pos.x - width/2, pos.y + 1.2f * camera.zoom, width, height)
            shapeRenderer.setColor(Color.RED)
            shapeRenderer.rect(pos.x - width/2, pos.y + 1.2f * camera.zoom, (enemy.health / enemy.maxHealth) * width, height)
        }
        shapeRenderer.end()
    }

    private fun renderHUD() {
        shapeRenderer.projectionMatrix = uiStage.viewport.camera.combined
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.setColor(Color.RED)
        shapeRenderer.rect(20f, uiStage.height - 40f, (player.health / player.maxHealth) * 200f, 20f)
        shapeRenderer.setColor(Color.BLUE)
        shapeRenderer.rect(20f, uiStage.height - 70f, (player.shield / player.maxShield) * 200f, 20f)
        shapeRenderer.setColor(Color.GREEN)
        shapeRenderer.rect(20f, uiStage.height - 100f, (player.experience / player.getExperienceThreshold()) * 200f, 10f)
        shapeRenderer.end()
    }

    private fun renderRadar() {
        val radarSize = 250f
        val radarX = uiStage.width - radarSize - 20f
        val radarY = uiStage.height - radarSize - 20f
        val radarScale = radarSize / (80f * camera.zoom)

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        shapeRenderer.setColor(Color(0f, 1f, 0f, 0.5f))
        shapeRenderer.rect(radarX, radarY, radarSize, radarSize)
        shapeRenderer.end()

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.setColor(Color.CYAN)
        shapeRenderer.circle(radarX + radarSize / 2, radarY + radarSize / 2, 4f)

        shapeRenderer.setColor(Color.RED)
        for (enemy in enemies) {
            val dx = (enemy.body.position.x - player.body.position.x) * radarScale
            val dy = (enemy.body.position.y - player.body.position.y) * radarScale

            if (Math.abs(dx) < radarSize / 2 && Math.abs(dy) < radarSize / 2) {
                shapeRenderer.circle(radarX + radarSize / 2 + dx, radarY + radarSize / 2 + dy, 3f)
            }
        }
        shapeRenderer.end()
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height)
        uiStage.viewport.update(width, height, true)
        updateUIElements()
    }

    override fun show() {}
    override fun pause() {}
    override fun resume() {}
    override fun hide() {}
    override fun dispose() {
        batch.dispose()
        shapeRenderer.dispose()
        world.dispose()
        uiStage.dispose()
        player.dispose()
        enemies.forEach { it.dispose() }
        projectiles.forEach { it.dispose() }
        starfield.dispose()
    }
}
