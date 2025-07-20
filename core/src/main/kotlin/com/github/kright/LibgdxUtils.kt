package com.github.kright

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.g3d.ModelBatch

inline fun ModelBatch.use(camera: Camera, body: ModelBatch.() -> Unit) {
    try {
        this.begin(camera)
        body()
    } finally {
        this.end()
    }
}