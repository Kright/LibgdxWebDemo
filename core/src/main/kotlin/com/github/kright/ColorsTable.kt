package com.github.kright

import com.badlogic.gdx.graphics.Color

class ColorsTable<T> {
    private val values = mutableListOf<T>()

    fun reserveColor(value: T): Color {
        val index = values.size
        values.add(value)
        return IndexAsColor.color(index)
    }

    operator fun get(color: Color): T {
        val index = IndexAsColor.index(color)
        return values[index]
    }

    fun getOrNull(color: Color): T? {
        val index = IndexAsColor.index(color)
        return values.getOrNull(index)
    }

    fun clear() {
        values.clear()
    }
}