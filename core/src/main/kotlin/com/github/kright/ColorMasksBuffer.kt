package com.github.kright

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.ScreenUtils

/**
 * A class that handles rendering to an offscreen texture (G-buffer).
 * This allows for deferred rendering techniques or post-processing effects.
 */
class ColorMasksBuffer<T>(
    width: Int = Gdx.graphics.width,
    height: Int = Gdx.graphics.height,
    private val format: Pixmap.Format = Pixmap.Format.RGBA8888,
    private val hasDepth: Boolean = true,
) : Disposable {
    private val coloredIndices: ColorsTable<T> = ColorsTable()
    private var frameBuffer: FrameBuffer = FrameBuffer(format, width, height, hasDepth)

    val colorTexture: Texture
        get() = frameBuffer.colorBufferTexture

    fun begin(
        clearColor: Boolean = true,
        clearDepth: Boolean = true,
        color: Color = Color.BLACK,
    ) {
        frameBuffer.begin()

        if (clearColor) {
            ScreenUtils.clear(color)
            coloredIndices.clear()
        }

        if (clearDepth) {
            Gdx.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT)
        }
    }

    fun reserveColor(value: T): Color =
        coloredIndices.reserveColor(value)

    fun useAndGetPixelOrNull(x: Int, y: Int, swapY: Boolean = false): T? {
        this.use(clearColor = false, clearDepth = false) {
            return getPixelOrNull(x, y, swapY)
        }
    }

    fun getPixelOrNull(x: Int, y: Int, swapY: Boolean = false): T? {
        if (swapY) {
            return getPixelOrNull(x, frameBuffer.height - y - 1, swapY = false)
        }

        val buffer = ScreenUtils.getFrameBufferPixels(x, y, 1, 1, false)

        val color = Color(
            positiveByteValue(buffer[0]) / 255f,
            positiveByteValue(buffer[1]) / 255f,
            positiveByteValue(buffer[2]) / 255f,
            positiveByteValue(buffer[3]) / 255f,
        )

        return coloredIndices.getOrNull(color)
    }

    private fun positiveByteValue(value: Byte): Int {
        return (value.toInt() and 0xFF)
    }

    fun end() {
        frameBuffer.end()
    }

    override fun dispose() {
        frameBuffer.dispose()
    }

    fun resize(width: Int, height: Int) {
        frameBuffer.dispose()
        frameBuffer = FrameBuffer(format, width, height, hasDepth)
        coloredIndices.clear()
    }

    inline fun <U> use(
        clearColor: Boolean = true,
        clearDepth: Boolean = true,
        color: Color = Color.BLACK,
        block: ColorMasksBuffer<T>.() -> U
    ): U {
        try {
            begin(clearColor, clearDepth, color)
            return block()
        } finally {
            end()
        }
    }
}
