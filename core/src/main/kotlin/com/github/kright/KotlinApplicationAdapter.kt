package com.github.kright

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g3d.*
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.g3d.loader.ObjLoader
import com.badlogic.gdx.graphics.g3d.utils.ShaderProvider
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.ScreenUtils
import ktx.assets.DisposableContainer
import ktx.assets.DisposableRegistry
import ktx.graphics.use
import kotlin.math.max

class KotlinApplicationAdapter(
    private val disposableContainer: DisposableContainer = DisposableContainer()
) : ApplicationAdapter(), DisposableRegistry by disposableContainer {
    private val batch: SpriteBatch = SpriteBatch().alsoRegister()
    private val image: Texture = Texture("libgdx.png").alsoRegister()
    private val font: BitmapFont = BitmapFont().alsoRegister()

    // 3D rendering components
    private val camera: PerspectiveCamera =
        PerspectiveCamera(
            67f,
            Gdx.graphics.getWidth().toFloat(),
            Gdx.graphics.getHeight().toFloat()
        ).apply {
            val cameraDist: Float = max(objectsX, objectsX) * 1.8f
            val cameraPos = Vector3(5f, 5f, 2f).setLength(cameraDist)
            position.set(cameraPos)
            lookAt(0f, 0f, 0f)
            near = 1f
            far = 200f
            update()
        }

    private val modelBatch: ModelBatch = ModelBatch().alsoRegister()
    private val shapeRenderer: ShapeRenderer = ShapeRenderer().alsoRegister()

    private val gBufferModelBatch: ModelBatch = run {
        val fixedColorShaderProvider = object : ShaderProvider {
            override fun getShader(renderable: Renderable): Shader {
                return fixedColorShader
            }

            override fun dispose() {}
        }

        ModelBatch(fixedColorShaderProvider)
    }.alsoRegister()

    private val model: Model = ObjLoader().loadModel(Gdx.files.internal("Sphere128x64.obj")).alsoRegister()

    private val environment: Environment = Environment().apply {
        set(ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f))
        add(DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f))
    }
    private val instances = ArrayList<ModelInstance>()

    // G-buffer for offscreen rendering
    private val gBuffer: GBuffer = GBuffer(Gdx.graphics.width, Gdx.graphics.height).alsoRegister()
    private val fixedColorShader: FixedColorShader = FixedColorShader(ShaderCode.load("fixedColor")).alsoRegister()
    private val modelSelection: ModelsSelection = ModelsSelection()

    override fun create() {
        for (z in 0..<objectsZ) {
            for (x in 0..<objectsX) {
                val instance = ModelInstance(model)
                instance.transform.translate((x - objectsX * 0.5f) * 2f, 0f, (z - objectsZ * 0.5f) * 2f)
                instances.add(instance)
            }
        }
    }


    override fun render() {
        // Update camera
        camera.update()

        gBuffer.use(color = Color.WHITE) { // Automatically handles begin/end with exception safety
            for ((index, instance) in instances.withIndex()) {
                fixedColorShader.setColor(IndexAsColor.color(index))

                // ineffective, but I don't want to debug `effective` way now
                gBufferModelBatch.use(camera) {
                    render(instance)
                }
            }
        }

        // Step 2: Render G-buffer texture to screen
        // Clear the screen
        ScreenUtils.clear(1f, 0f, 1f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)

        modelBatch.use(camera) {
            for (instance in instances) {
                render(instance, environment)
            }
        }

        shapeRenderer.use(ShapeRenderer.ShapeType.Line, camera) {
            for (instance in modelSelection.selected) {
                AxisRender.renderAxesAndPlanes(instance.transform, camera, it)
            }
        }

        // Render the G-buffer texture to the screen
        batch.use {
            batch.draw(
                gBuffer.colorTexture,
                Gdx.graphics.width.toFloat() * 0.75f, Gdx.graphics.height.toFloat() * 0.75f,
                Gdx.graphics.width.toFloat() * 0.25f, Gdx.graphics.height.toFloat() * 0.25f,
                0, 0,
                gBuffer.colorTexture.width, gBuffer.colorTexture.height,
                false, true // Flip vertically because FrameBuffer textures are Y-flipped
            )

            // Step 3: Render 2D UI elements directly to the screen
            batch.draw(image, 140f, 210f)

            // Display FPS counter in the top left corner
            font.draw(
                batch,
                "FPS: ${Gdx.graphics.getFramesPerSecond()}",
                10f,
                Gdx.graphics.getHeight() - font.getLineHeight()
            )
            font.draw(
                batch,
                "triangles count: ${16128 * objectsX * objectsZ}",
                10f,
                Gdx.graphics.getHeight() - font.getLineHeight() * 2
            )
        }

        handleMouseClick()
    }

    private fun handleMouseClick() {
        if (Gdx.input.justTouched()) {
            val screenX = Gdx.input.x
            val screenY = Gdx.input.y

            var pixel: Color? = null
            gBuffer.use(clearColor = false, clearDepth = false) {
                pixel = gBuffer.getPixel(screenX, screenY, swapY = true)
            }
            require(pixel != null)

            println("pixel = $pixel")
            println("index = ${IndexAsColor.index(pixel!!)}")

            val index = IndexAsColor.index(pixel)

            val isShiftPressed: Boolean =
                Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.SHIFT_LEFT) ||
                        Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.SHIFT_RIGHT)

            if (isShiftPressed) {
                if (index < instances.size) {
                    modelSelection.selected.add(instances[index])
                }
            } else {
                modelSelection.selected.clear()

                if (index < instances.size) {
                    modelSelection.selected.add(instances[index])
                }
            }
        }
    }

    override fun resize(width: Int, height: Int) {
        Gdx.gl.glViewport(0, 0, width, height)

        // Update camera aspect ratio
        camera.viewportWidth = width.toFloat()
        camera.viewportHeight = height.toFloat()
        camera.update()

        // Resize G-buffer to match new screen dimensions
        gBuffer.resize(width, height)

        // Update SpriteBatch projection matrix
        val cam2d = com.badlogic.gdx.graphics.OrthographicCamera(width.toFloat(), height.toFloat())
        cam2d.setToOrtho(false) // Origin bottom-left, y-up
        batch.projectionMatrix = cam2d.combined
    }

    override fun dispose() {
        disposableContainer.dispose()
    }

    companion object {
        private const val objectsX = 20
        private const val objectsZ = 20
    }
}
