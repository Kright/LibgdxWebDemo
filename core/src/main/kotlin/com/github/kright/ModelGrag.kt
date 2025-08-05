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
import ktx.math.times

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

                MovementAxes.X -> ModelDraggingInPlane.createForAxis(camera, modelSelection, draggedModel, Vector3.X.cpy())
                MovementAxes.Y -> ModelDraggingInPlane.createForAxis(camera, modelSelection, draggedModel, Vector3.Y.cpy())
                MovementAxes.Z -> ModelDraggingInPlane.createForAxis(camera, modelSelection, draggedModel, Vector3.Z.cpy())

                MovementAxes.XY -> ModelDraggingInPlane.createForPlane(
                    camera,
                    modelSelection,
                    draggedModel,
                    Plane(Vector3.Z, bodyCenter)
                )

                MovementAxes.XZ -> ModelDraggingInPlane.createForPlane(
                    camera,
                    modelSelection,
                    draggedModel,
                    Plane(Vector3.Y, bodyCenter)
                )

                MovementAxes.YZ -> ModelDraggingInPlane.createForPlane(
                    camera,
                    modelSelection,
                    draggedModel,
                    Plane(Vector3.X, bodyCenter)
                )

                MovementAxes.XYZ -> ModelDraggingInPlane.createForPlane(
                    camera,
                    modelSelection,
                    draggedModel,
                    Plane( (bodyCenter - camera.position).nor(), bodyCenter)
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
    private val initialIntersection: Vector3,
    private val castAxis: Vector3? = null,
) : ModelGrag {

    private val initialModelsPositions: Map<ModelInstance, Vector3> =
        modelSelection.selected.associateWith { it.transform.getTranslation(Vector3()) }

    override fun updateDragging() {
        val current = getMousePos()
        val currentIntersection = findIntersection(camera, plane, current) ?: return

        val shift = currentIntersection - initialIntersection

        val castShift =
            if (castAxis == null) shift
            else castAxis * shift.dot(castAxis)

        initialModelsPositions.forEach { (model, initialPos) ->
            model.transform.setTranslation(initialPos + castShift)
        }
    }


    companion object {
        fun createForPlane(
            camera: Camera,
            modelSelection: ModelsSelection,
            draggedModel: ModelInstance,
            plane: Plane
        ): ModelGrag? {
            val initialMousePos = getMousePos()
            val initialIntersection = findIntersection(camera, plane, initialMousePos) ?: return null

            return ModelDraggingInPlane(
                camera,
                modelSelection,
                draggedModel,
                plane,
                initialIntersection,
                castAxis = null
            )
        }

        fun createForAxis(
            camera: Camera,
            modelSelection: ModelsSelection,
            draggedModel: ModelInstance,
            axis: Vector3
        ): ModelGrag? {
            val initialMousePos = getMousePos()
            val plane = run {
                val bodyCenter = draggedModel.transform.getTranslation(Vector3())
                val camToBody = (bodyCenter - camera.position).nor()
                val normal = camToBody - (axis * camToBody.dot(axis))
                if (normal.len() < 0.05) return null
                normal.nor()

                Plane(normal, bodyCenter)
            }

            val initialIntersection = findIntersection(camera, plane, initialMousePos) ?: return null

            return ModelDraggingInPlane(
                camera,
                modelSelection,
                draggedModel,
                plane,
                initialIntersection,
                castAxis = axis,
            )
        }

        private fun findIntersection(camera: Camera, plane: Plane, mousePos: Vector2): Vector3? {
            val ray = mousePosToRay(camera, mousePos)

            val intersection = Vector3()
            if (Intersector.intersectRayPlane(ray, plane, intersection)) {
                return intersection
            } else {
                return null
            }
        }
    }
}