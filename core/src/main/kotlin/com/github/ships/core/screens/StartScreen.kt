package com.github.ships.core.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.github.ships.core.ShipShipGame
import com.github.ships.core.utils.Starfield

class StartScreen(val game: ShipShipGame) : Screen {
    private val batch = SpriteBatch()
    private val camera = OrthographicCamera()
    private val viewport = ExtendViewport(30f, 30f, camera)
    private val starfield = Starfield()
    private val font = BitmapFont()
    private val layout = GlyphLayout()

    private var time = 0f
    private val camPos = Vector2(0f, 0f)
    private var fade = 1f
    private var starting = false

    override fun render(delta: Float) {
        time += delta

        camPos.x += delta * 2f
        camPos.y += delta * 1f

        if (starting) {
            fade = MathUtils.clamp(fade - delta * 2f, 0f, 1f)
            if (fade <= 0f) {
                game.screen = GameScreen(game)
                return
            }
        }

        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        camera.position.set(camPos.x, camPos.y, 0f)
        camera.update()

        batch.projectionMatrix = camera.combined
        batch.begin()
        starfield.render(batch, camPos, 1f, viewport.worldWidth, viewport.worldHeight, delta)
        batch.end()

        val uiCam = OrthographicCamera(Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        uiCam.position.set(uiCam.viewportWidth / 2, uiCam.viewportHeight / 2, 0f)
        uiCam.update()
        batch.projectionMatrix = uiCam.combined

        batch.begin()

        // STATIC Main Title
        font.data.setScale(5f)
        font.setColor(1f, 1f, 1f, fade)
        layout.setText(font, "ShipShip")
        font.draw(batch, "ShipShip", (uiCam.viewportWidth - layout.width) / 2f, uiCam.viewportHeight / 2f + 100f)

        // PULSING Subtitle
        font.data.setScale(2f)
        val alpha = (MathUtils.sin(time * 4f) + 1f) / 2f
        font.setColor(1f, 1f, 1f, alpha * fade)
        layout.setText(font, "Tap to Begin")
        font.draw(batch, "Tap to Begin", (uiCam.viewportWidth - layout.width) / 2f, uiCam.viewportHeight / 2f - 50f)

        batch.end()

        if (Gdx.input.justTouched()) {
            starting = true
        }
    }

    override fun resize(width: Int, height: Int) {
        viewport.update(width, height)
    }

    override fun show() {}
    override fun hide() {}
    override fun pause() {}
    override fun resume() {}
    override fun dispose() {
        batch.dispose()
        starfield.dispose()
        font.dispose()
    }
}
