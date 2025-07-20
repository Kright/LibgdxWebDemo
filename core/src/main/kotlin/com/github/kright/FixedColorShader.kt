package com.github.kright

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g3d.Renderable
import com.badlogic.gdx.graphics.g3d.Shader
import com.badlogic.gdx.graphics.g3d.utils.RenderContext
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector4

/**
 * A simple shader that renders objects with a fixed color.
 * Used for GBuffer rendering.
 */
class FixedColorShader(shaderCode: ShaderCode) : Shader {
    private val program: ShaderProgram = shaderCode.toShaderProgram()
    private val color = Vector4(1f, 1f, 1f, 1f)
    private var camera: Camera? = null
    private var context: RenderContext? = null
    private val tmpMatrix = Matrix4()

    override fun init() {
        program.bind()
    }

    override fun dispose() {
        program.dispose()
    }

    override fun begin(camera: Camera, context: RenderContext) {
        this.camera = camera
        this.context = context
        program.bind()

        context.setDepthTest(GL20.GL_LEQUAL)
        context.setCullFace(GL20.GL_BACK)

        program.setUniformMatrix("u_projViewTrans", camera.combined)
    }

    override fun end() {
        program.end()
    }

    fun setColor(newColor: Color) {
        this.color.set(newColor.r, newColor.g, newColor.b, newColor.a)
    }

    fun updateUniform() {
        program.setUniformf("u_specularColor", color)
    }

    override fun render(renderable: Renderable) {
        // Set the world transform
        tmpMatrix.set(renderable.worldTransform)
        program.setUniformMatrix("u_worldTrans", tmpMatrix)

        // Set the color
        program.setUniformf("u_specularColor", color)

        // Render the mesh
        renderable.meshPart.render(program)
    }

    override fun compareTo(other: Shader?): Int = 0

    override fun canRender(instance: Renderable?): Boolean = true
}