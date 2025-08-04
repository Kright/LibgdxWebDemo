package com.github.kright

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector3
import ktx.graphics.box
import ktx.math.minus
import ktx.math.times

inline fun ModelBatch.use(camera: Camera, body: ModelBatch.() -> Unit) {
    try {
        this.begin(camera)
        body()
    } finally {
        this.end()
    }
}


fun ShapeRenderer.box(
    center: Vector3,
    size: Vector3,
) {
    box(
        center - size * 0.5f,
        width = size.x,
        height = size.y,
        depth = -size.z // I don't know why libgdx inverts depth; it's awful logic
    )
}


val Camera.right: Vector3
    get() = Vector3(direction).crs(up).nor()