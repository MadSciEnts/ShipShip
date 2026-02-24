package com.github.ships.core

import com.badlogic.gdx.Game
import com.github.ships.core.screens.GameScreen

class ShipShipGame : Game() {
    override fun create() {
        setScreen(GameScreen(this))
    }
}
