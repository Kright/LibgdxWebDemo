package com.github.kright

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.math.Intersector
import com.badlogic.gdx.math.Plane
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.Ray
import ktx.math.minus
import ktx.math.plus

sealed interface ModelGrag {
    fun updateDragging()

    companion object {
        fun create(
            camera: Camera,
            draggedModel: ModelInstance,
            modelSelection: ModelsSelection,
            draggedAxes: MovementAxes
        ): ModelGrag? {
            val bodyCenter = draggedModel.transform.getTranslation(Vector3())

            return when (draggedAxes) {
                MovementAxes.NOTHING -> null

                MovementAxes.X -> ModelDraggingAlongAxis(camera, modelSelection, draggedModel, Vector3.X)
                MovementAxes.Y -> ModelDraggingAlongAxis(camera, modelSelection, draggedModel, Vector3.Y)
                MovementAxes.Z -> ModelDraggingAlongAxis(camera, modelSelection, draggedModel, Vector3.Z)

                MovementAxes.XY -> ModelDraggingInPlane(
                    camera,
                    modelSelection,
                    draggedModel,
                    Plane(Vector3.Z, bodyCenter)
                )

                MovementAxes.XZ -> ModelDraggingInPlane(
                    camera,
                    modelSelection,
                    draggedModel,
                    Plane(Vector3.Y, bodyCenter)
                )

                MovementAxes.YZ -> ModelDraggingInPlane(
                    camera,
                    modelSelection,
                    draggedModel,
                    Plane(Vector3.X, bodyCenter)
                )

                MovementAxes.XYZ -> ModelDraggingInPlane(
                    camera,
                    modelSelection,
                    draggedModel,
                    Plane(camera.direction, bodyCenter)
                )
            }
        }
    }
}

private fun getMousePos(): Vector2 {
    val x = Gdx.input.x
    val y = Gdx.input.y
    return Vector2(x.toFloat(), y.toFloat())
}

private fun mousePosToRay(camera: Camera, mousePos: Vector2): Ray {
    val onNearPlane = camera.unproject(Vector3(mousePos.x, mousePos.y, 0f))
    val onFarPlane = camera.unproject(Vector3(mousePos.x, mousePos.y, 1f))
    return Ray(onNearPlane, onFarPlane.sub(onNearPlane))
}


private class ModelDraggingInPlane(
    private val camera: Camera,
    private val modelSelection: ModelsSelection,
    private val draggedModel: ModelInstance,
    private val plane: Plane,
) : ModelGrag {

    private val initialMousePos = getMousePos()
    private val initialModelsPositions: Map<ModelInstance, Vector3> =
        modelSelection.selected.associateWith { it.transform.getTranslation(Vector3()) }

    private val initialIntersection = findIntersection(initialMousePos)

    override fun updateDragging() {
        if (initialIntersection == null) return

        val current = getMousePos()

        val currentIntersection = findIntersection(current)

        if (currentIntersection == null) return

        val shift = currentIntersection - initialIntersection

        initialModelsPositions.forEach { (model, initialPos) ->
            model.transform.setTranslation(initialPos + shift)
        }
    }

    private fun findIntersection(mousePos: Vector2): Vector3? {
        val ray = mousePosToRay(camera, mousePos)

        val intersection = Vector3()
        if (Intersector.intersectRayPlane(ray, plane, intersection)) {
            return intersection
        } else {
            return null
        }
    }
}

private class ModelDraggingAlongAxis(
    private val camera: Camera,
    private val modelSelection: ModelsSelection,
    private val draggedModel: ModelInstance,
    private val axis: Vector3,
) : ModelGrag {

    private val initialMousePos = getMousePos()
    private val initialModelsPositions: Map<ModelInstance, Vector3> =
        modelSelection.selected.associateWith { it.transform.getTranslation(Vector3()) }

    private val planeWithAxis: Plane = run {
        val normal: Vector3 =
            camera.up.cpy().crs(axis).takeIf { normal -> normal.len() > 0.5 }
                ?: camera.right.cpy().crs(axis)

        Plane(normal, draggedModel.transform.getTranslation(Vector3()))
    }

    private val initialIntersection = findIntersection(initialMousePos)

    override fun updateDragging() {
        if (initialIntersection == null) return

        val current = getMousePos()
        val currentIntersection = findIntersection(current)

        if (currentIntersection == null) return

        val shift = currentIntersection - initialIntersection

        val realShift = axis.cpy().scl(shift.dot(axis))
        initialModelsPositions.forEach { (model, initialPos) ->
            model.transform.setTranslation(initialPos + realShift)
        }
    }

    private fun findIntersection(mousePos: Vector2): Vector3? {
        val ray = mousePosToRay(camera, mousePos)

        val intersection = Vector3()
        if (Intersector.intersectRayPlane(ray, planeWithAxis, intersection)) {
            return intersection
        } else {
            return null
        }
    }
}