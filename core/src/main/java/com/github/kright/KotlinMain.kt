package com.github.kright

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.g3d.loader.ObjLoader
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.ScreenUtils
import kotlin.math.max

class KotlinMain : ApplicationAdapter() {
    private var batch: SpriteBatch? = null
    private var image: Texture? = null
    private var font: BitmapFont? = null

    // 3D rendering components
    private lateinit var camera: PerspectiveCamera
    private var modelBatch: ModelBatch? = null
    private var model: Model? = null
    private var environment: Environment? = null
    private val instances = ArrayList<ModelInstance>()

    override fun create() {
        batch = SpriteBatch()
        image = Texture("libgdx.png")
        font = BitmapFont()

        // Initialize 3D components
        modelBatch = ModelBatch()

        // Set up camera
        camera = PerspectiveCamera(67f, Gdx.graphics.getWidth().toFloat(), Gdx.graphics.getHeight().toFloat())
        val cameraDist: Float = max(objectsX, objectsX) * 1.8f
        val cameraPos = Vector3(5f, 5f, 2f).setLength(cameraDist)
        camera.position.set(cameraPos)
        camera.lookAt(0f, 0f, 0f)
        camera.near = 1f
        camera.far = 200f
        camera.update()

        // Set up lighting environment
        environment = Environment()
        environment!!.set(ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f))
        environment!!.add(DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f))

        // Load the sphere model
        val loader = ObjLoader()
        model = loader.loadModel(Gdx.files.internal("Sphere128x64.obj"))

        for (z in 0..<objectsZ) {
            for (x in 0..<objectsX) {
                val instance = ModelInstance(model)
                instance.transform.translate((x - objectsX * 0.5f) * 2f, 0f, (z - objectsZ * 0.5f) * 2f)
                instances.add(instance)
            }
        }
    }

    override fun render() {
        // Clear the screen and depth buffer
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        // Update camera
        camera.update()

        // Render 3D model
        modelBatch!!.begin(camera)

        for (instance in instances) {
            instance.transform.rotate(0f, 1f, 0f, 0.5f)
            modelBatch!!.render(instance, environment)
        }

        modelBatch!!.end()

        // Render 2D sprite on top if needed
        batch!!.begin()
        batch!!.draw(image, 140f, 210f)

        // Display FPS counter in the top left corner
        font!!.draw(
            batch,
            "FPS: ${Gdx.graphics.getFramesPerSecond()}",
            10f,
            Gdx.graphics.getHeight() - font!!.getLineHeight()
        )
        font!!.draw(
            batch,
            "triangles count: ${16128 * objectsX * objectsZ}",
            10f,
            Gdx.graphics.getHeight() - font!!.getLineHeight() * 2
        )

        batch!!.end()
    }

    override fun dispose() {
        // Dispose 2D resources
        batch?.dispose()
        image?.dispose()
        font?.dispose()

        // Dispose 3D resources
        modelBatch?.dispose()
        model?.dispose()
    }

    companion object {
        private const val objectsX = 20
        private const val objectsZ = 20
    }
}
