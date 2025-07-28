package com.github.kright

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3
import ktx.math.plus
import ktx.math.times

object AxisRender {
    private val center = Vector3(0f, 0f, 0f)
    private val xAxis = Vector3(1f, 0f, 0f)
    private val yAxis = Vector3(0f, 1f, 0f)
    private val zAxis = Vector3(0f, 0f, 1f)

    private val xColor = Color.RED
    private val yColor = Color.GREEN
    private val zColor = Color.BLUE

    fun renderAxesAndPlanes(transform: Matrix4, camera: Camera, shapeRenderer: ShapeRenderer): Unit {
        val vec: Vector3 = transform.getTranslation(Vector3())

        val p = camera.project(vec.cpy())
        val realDepth = (camera.near + (camera.far - camera.near) * 0.5f * (p.z + 1f) / camera.far)

        val scale = Math.abs(realDepth) * 2f

        shapeRenderer.transformMatrix = transform

        shapeRenderer.color = xColor
        shapeRenderer.line(center, xAxis * scale)
        shapeRenderer.color = yColor
        shapeRenderer.line(center, yAxis * scale)
        shapeRenderer.color = zColor
        shapeRenderer.line(center, zAxis * scale)

        val minX = 0.4f
        val maxX = 0.6f
        val minY = 0.4f
        val maxY = 0.6f

        shapeRenderer.color = xColor
        shapeRenderer.rect3d(center, yAxis * scale, zAxis * scale, minX, maxX, minY, maxY)

        shapeRenderer.color = yColor
        shapeRenderer.rect3d(center, xAxis * scale, zAxis * scale, minX, maxX, minY, maxY)

        shapeRenderer.color = zColor
        shapeRenderer.rect3d(center, xAxis * scale, yAxis * scale, minX, maxX, minY, maxY)
    }

    private fun ShapeRenderer.rect3d(
        center: Vector3, vx: Vector3, vy: Vector3,
        minX: Float, maxX: Float, minY: Float, maxY: Float
    ) {
        val vxMin = vx * minX
        val vxMax = vx * maxX
        val vyMin = vy * minY
        val vyMax = vy * maxY

        val v0 = center + vxMin + vyMin
        val v1 = center + vxMin + vyMax
        val v2 = center + vxMax + vyMin
        val v3 = center + vxMax + vyMax

        line(v0, v1)
        line(v0, v2)
        line(v3, v1)
        line(v3, v2)
    }

}