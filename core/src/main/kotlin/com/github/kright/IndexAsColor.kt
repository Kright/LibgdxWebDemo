package com.github.kright

import com.badlogic.gdx.graphics.Color

object IndexAsColor {
    private const val step = 32
    private const val maxIndieces = step * step * step


    fun color(index: Int): Color {
        require(index < maxIndieces)
        return Color(
            (index % step).toFloat() / step,
            (index / step % step).toFloat() / step,
            (index / step / step % step).toFloat() / step,
            1.0f
        )
    }

    fun index(color: Color): Int {
        return Math.round(color.r * step) + Math.round(color.g * step) * step + Math.round(color.b * step) * step * step
    }
}