package com.github.ships.core

import com.badlogic.gdx.Game
import com.github.ships.core.screens.StartScreen

class ShipShipGame : Game() {
    override fun create() {
        setScreen(StartScreen(this))
    }
}
