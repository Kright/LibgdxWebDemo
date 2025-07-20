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
class GBuffer(
    width: Int = Gdx.graphics.width,
    height: Int = Gdx.graphics.height,
    private val format: Pixmap.Format = Pixmap.Format.RGBA8888,
    private val hasDepth: Boolean = true
) : Disposable {

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
        }

        if (clearDepth) {
            Gdx.gl.glClear(GL20.GL_DEPTH_BUFFER_BIT)
        }
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
    }

    inline fun <T> use(
        clearColor: Boolean = true,
        clearDepth: Boolean = true,
        color: Color = Color.BLACK,
        block: GBuffer.() -> T
    ): T {
        try {
            begin(clearColor, clearDepth, color)
            return block()
        } finally {
            end()
        }
    }
}
