package com.github.ships.core.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Skin
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.github.ships.core.ShipShipGame
import com.github.ships.core.utils.ProceduralTextureGenerator

class GameOverScreen(val game: ShipShipGame, val score: Int) : Screen {
    private val stage = Stage(ScreenViewport())

    init {
        val skin = Skin()
        val whitePixel = ProceduralTextureGenerator.createWhitePixel()
        skin.add("white", whitePixel)

        val labelStyle = Label.LabelStyle()
        labelStyle.font = com.badlogic.gdx.graphics.g2d.BitmapFont()
        labelStyle.fontColor = Color.WHITE

        val textButtonStyle = TextButton.TextButtonStyle()
        textButtonStyle.up = skin.newDrawable("white", Color.DARK_GRAY)
        textButtonStyle.down = skin.newDrawable("white", Color.GRAY)
        textButtonStyle.font = labelStyle.font

        val table = Table()
        table.setFillParent(true)
        stage.addActor(table)

        val gameOverLabel = Label("GAME OVER", labelStyle)
        gameOverLabel.setFontScale(3f)
        table.add(gameOverLabel).padBottom(20f).row()

        val scoreLabel = Label("Level Reached: $score", labelStyle)
        table.add(scoreLabel).padBottom(40f).row()

        val retryButton = TextButton("RETRY", textButtonStyle)
        retryButton.addListener(object : ClickListener() {
            override fun clicked(event: InputEvent?, x: Float, y: Float) {
                game.screen = GameScreen(game)
            }
        })
        table.add(retryButton).width(200f).height(60f)

        Gdx.input.inputProcessor = stage
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        stage.act(delta)
        stage.draw()
    }

    override fun resize(width: Int, height: Int) {
        stage.viewport.update(width, height, true)
    }

    override fun show() {}
    override fun pause() {}
    override fun resume() {}
    override fun hide() {}
    override fun dispose() {
        stage.dispose()
    }
}
