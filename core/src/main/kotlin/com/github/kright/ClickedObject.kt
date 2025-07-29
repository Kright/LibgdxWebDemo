package com.github.kright

import com.badlogic.gdx.graphics.g3d.ModelInstance

sealed interface ClickedObject {
    data class Model(
        val model: ModelInstance
    ) : ClickedObject

    data class ModelMovement(
        val model: ModelInstance,
        val axes: MovementAxes
    ) : ClickedObject
}
