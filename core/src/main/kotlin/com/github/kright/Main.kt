package com.github.kright

import com.badlogic.gdx.ApplicationAdapter

class Main : ApplicationAdapter() {
    private val lazyInitializedAdapter by lazy {
        GLTFQuickStartExample()
    }

    override fun create() {
        lazyInitializedAdapter.create()
    }

    override fun render() {
        lazyInitializedAdapter.render()
    }

    override fun resize(width: Int, height: Int) {
        lazyInitializedAdapter.resize(width, height)
    }

    override fun dispose() {
        lazyInitializedAdapter.dispose()
    }
}
