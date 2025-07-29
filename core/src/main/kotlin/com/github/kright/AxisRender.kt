package com.github.kright

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g3d.ModelInstance
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

    fun renderAxesAndPlanes(transform: Matrix4, camera: Camera, shapeRenderer: ShapeRenderer) {
        require(shapeRenderer.currentType == ShapeRenderer.ShapeType.Line)

        val size = getSize(transform, camera)

        shapeRenderer.transformMatrix = transform
        renderAxes(center, size, shapeRenderer)
        renderPlanes(size * 0.4f, size * 0.6f, shapeRenderer)
    }

    fun renderAxesForSelection(
        transform: Matrix4,
        camera: Camera,
        shapeRenderer: ShapeRenderer,
        maskBuffer: ColorMasksBuffer<ClickedObject>,
        model: ModelInstance,
    ) {
        require(shapeRenderer.currentType == ShapeRenderer.ShapeType.Filled)

        val size = getSize(transform, camera)

        shapeRenderer.transformMatrix = transform

        val width: Float = size * 0.1f
        val centerBoxSize: Float = size * 0.3f

        shapeRenderer.color = maskBuffer.reserveColor(ClickedObject.ModelMovement(model, MovementAxes.X))
        shapeRenderer.box(center + Vector3(size * 0.5f, 0f, 0f), Vector3(size, width, width))

        shapeRenderer.color = maskBuffer.reserveColor(ClickedObject.ModelMovement(model, MovementAxes.Y))
        shapeRenderer.box(center + Vector3(0f, size * 0.5f, 0f), Vector3(width, size, width))

        shapeRenderer.color = maskBuffer.reserveColor(ClickedObject.ModelMovement(model, MovementAxes.Z))
        shapeRenderer.box(center + Vector3(0f, 0f, size * 0.5f), Vector3(width, width, size))

        val rectSize = size * 0.5f
        val rectWidth = size * 0.05f

        shapeRenderer.color = maskBuffer.reserveColor(ClickedObject.ModelMovement(model, MovementAxes.XY))
        shapeRenderer.box(
            Vector3(center) + xAxis * size * 0.5f + yAxis * size * 0.5f,
            Vector3(rectSize, rectSize, rectWidth)
        )

        shapeRenderer.color = maskBuffer.reserveColor(ClickedObject.ModelMovement(model, MovementAxes.XZ))
        shapeRenderer.box(
            Vector3(center) + xAxis * size * 0.5f + zAxis * size * 0.5f,
            Vector3(rectSize, rectWidth, rectSize)
        )

        shapeRenderer.color = maskBuffer.reserveColor(ClickedObject.ModelMovement(model, MovementAxes.YZ))
        shapeRenderer.box(
            Vector3(center) + yAxis * size * 0.5f + zAxis * size * 0.5f,
            Vector3(rectWidth, rectSize, rectSize)
        )

        shapeRenderer.color = maskBuffer.reserveColor(ClickedObject.ModelMovement(model, MovementAxes.XYZ))
        shapeRenderer.box(center, Vector3(centerBoxSize, centerBoxSize, centerBoxSize))
    }

    private fun getSize(transform: Matrix4, camera: Camera): Float {
        val vec: Vector3 = transform.getTranslation(Vector3())
        val p = camera.project(vec.cpy())
        val realDepth = (camera.near + (camera.far - camera.near) * 0.5f * (p.z + 1f) / camera.far)
        val scale = Math.abs(realDepth) * 2f
        return scale
    }

    private fun renderAxes(center: Vector3, size: Float, shapeRenderer: ShapeRenderer) {
        require(shapeRenderer.currentType == ShapeRenderer.ShapeType.Line)
        shapeRenderer.color = xColor
        shapeRenderer.line(center, center + xAxis * size)
        shapeRenderer.color = yColor
        shapeRenderer.line(center, center + yAxis * size)
        shapeRenderer.color = zColor
        shapeRenderer.line(center, center + zAxis * size)
    }

    private fun renderPlanes(
        minSize: Float,
        maxSize: Float,
        shapeRenderer: ShapeRenderer,
    ) {
        shapeRenderer.color = xColor
        shapeRenderer.rectWireframe(center, yAxis, zAxis, minSize, maxSize, minSize, maxSize)

        shapeRenderer.color = yColor
        shapeRenderer.rectWireframe(center, xAxis, zAxis, minSize, maxSize, minSize, maxSize)

        shapeRenderer.color = zColor
        shapeRenderer.rectWireframe(center, xAxis, yAxis, minSize, maxSize, minSize, maxSize)
    }
}


private fun ShapeRenderer.rectWireframe(
    center: Vector3, vx: Vector3, vy: Vector3,
    minX: Float, maxX: Float, minY: Float, maxY: Float,
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