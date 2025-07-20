package com.github.kright

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.glutils.ShaderProgram

data class ShaderCode(
    val vertexShader: String,
    val fragmentShader: String
) {

    fun toShaderProgram(): ShaderProgram =
        ShaderProgram(vertexShader, fragmentShader).also {
            require(it.isCompiled)
        }

    companion object {
        fun load(name: String): ShaderCode {
            val vertexShader = Gdx.files.internal("shader/${name}V.glsl").readString()
            val fragmentShader = Gdx.files.internal("shader/${name}F.glsl").readString()
            return ShaderCode(vertexShader, fragmentShader)
        }
    }
}