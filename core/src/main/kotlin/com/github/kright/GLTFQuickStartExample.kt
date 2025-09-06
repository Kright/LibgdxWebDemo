package com.github.kright

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.Renderable
import com.badlogic.gdx.graphics.g3d.RenderableProvider
import com.badlogic.gdx.graphics.g3d.Shader
import com.badlogic.gdx.graphics.g3d.utils.ShaderProvider
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import ktx.assets.DisposableContainer
import ktx.assets.DisposableRegistry
import ktx.graphics.use

import net.mgsx.gltf.loaders.gltf.GLTFLoader;
import net.mgsx.gltf.scene3d.attributes.PBRCubemapAttribute;
import net.mgsx.gltf.scene3d.attributes.PBRTextureAttribute;
import net.mgsx.gltf.scene3d.lights.DirectionalShadowLight
import net.mgsx.gltf.scene3d.scene.Scene;
import net.mgsx.gltf.scene3d.scene.SceneManager;
import net.mgsx.gltf.scene3d.scene.SceneSkybox;
import net.mgsx.gltf.scene3d.utils.IBLBuilder;
import java.lang.Math.pow


class GLTFQuickStartExample(private val disposableContainer: DisposableContainer = DisposableContainer()): ApplicationAdapter(), DisposableRegistry by disposableContainer {

    companion object {
        var cameraDistance: Float = 100f

        private const val objectsX = 3
        private const val objectsZ = 3
    }

    private val sceneManager: SceneManager = SceneManager().alsoRegister()

    private val camera: PerspectiveCamera = PerspectiveCamera(
        60f,
        Gdx.graphics.getWidth().toFloat(),
        Gdx.graphics.getHeight().toFloat()
    ).also { camera ->
        val d = 1000f
        camera.near = d / 1000f
        camera.far = d * 4
        val v = Vector3().setFromSpherical(0f, MathUtils.PI / 2 * 0.8f).scl(cameraDistance)
        camera.position.set(v.x, v.z, v.y) // swap y and z
        camera.update()
    }

    private val spriteBatch: SpriteBatch = SpriteBatch().alsoRegister()
    private val font: BitmapFont = BitmapFont().alsoRegister()

    // G-buffer for offscreen rendering
    private val maskBuffer: ColorMasksBuffer<RenderableProvider> =
        ColorMasksBuffer<RenderableProvider>(Gdx.graphics.width, Gdx.graphics.height).alsoRegister()

    private val fixedColorShader: FixedColorShader = FixedColorShader(ShaderCode.load("fixedColor")).alsoRegister()
    private val modelSelection: ModelsSelection = ModelsSelection()
    private var modelDrag: ModelGrag? = null

    private var maskSize = 0.25f

    private val maskBufferModelBatch: ModelBatch = run {
        val fixedColorShaderProvider = object : ShaderProvider {
            override fun getShader(renderable: Renderable): Shader {
                return fixedColorShader
            }

            override fun dispose() {}
        }

        ModelBatch(fixedColorShaderProvider)
    }.alsoRegister()


    override fun create() {
        // create scene

        val sceneAsset = GLTFLoader().load(Gdx.files.internal("neighbourhood_city_modular_lowpoly/scene.gltf")).alsoRegister()

        for (z in 0..<objectsZ) {
            for (x in 0..<objectsX) {
                val scene = Scene(sceneAsset.scene)
                scene.modelInstance.transform.translate((x - (objectsX - 1) * 0.5f) * 40f, 0f, (z - (objectsZ - 1) * 0.5f) * 40f)
                sceneManager.addScene(scene)
            }
        }

        sceneManager.setCamera(camera)


        // setup light
        val light = DirectionalShadowLight(2048, 2048).alsoRegister()
        light.direction.set(-1f, -2f, 1f).nor()
        light.color.set(Color.YELLOW)
        light.intensity = 5f
        sceneManager.environment.add(light)


        // setup quick IBL (image based lighting)
        val iblBuilder: IBLBuilder = IBLBuilder.createOutdoor(light)
        val environmentCubemap = iblBuilder.buildEnvMap(1024)
        val diffuseCubemap = iblBuilder.buildIrradianceMap(256)
        val specularCubemap = iblBuilder.buildRadianceMap(10)
        iblBuilder.dispose()

        // This texture is provided by the library, no need to have it in your assets.
        val brdfLUT = Texture(Gdx.files.classpath("net/mgsx/gltf/shaders/brdfLUT.png")).alsoRegister()

        sceneManager.setAmbientLight(0.3f)
        sceneManager.environment.set(PBRTextureAttribute(PBRTextureAttribute.BRDFLUTTexture, brdfLUT))
        sceneManager.environment.set(PBRCubemapAttribute.createSpecularEnv(specularCubemap))
        sceneManager.environment.set(PBRCubemapAttribute.createDiffuseEnv(diffuseCubemap))

        // setup skybox
        val skybox = SceneSkybox(environmentCubemap).alsoRegister()
        sceneManager.setSkyBox(skybox)
    }

    override fun resize(width: Int, height: Int) {
        sceneManager.updateViewport(width.toFloat(), height.toFloat())

        // Resize G-buffer to match new screen dimensions
        maskBuffer.resize(width, height)

        // Update SpriteBatch projection matrix
        val cam2d = com.badlogic.gdx.graphics.OrthographicCamera(width.toFloat(), height.toFloat())
        cam2d.setToOrtho(false) // Origin bottom-left, y-up
        spriteBatch.projectionMatrix = cam2d.combined
    }

    override fun render() {
        val deltaTime = Gdx.graphics.getDeltaTime()

        // animate camera
        if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.A)) {
            camera.position.rotate(Vector3.Y, -50f * deltaTime)
        }
        if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.D)) {
            camera.position.rotate(Vector3.Y, 50f * deltaTime)
        }

        val distV = 1f
        if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.W)) {
            cameraDistance *= pow(2.0, (-distV * deltaTime).toDouble()).toFloat()
        }
        if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.S)) {
            cameraDistance *= pow(2.0, (distV * deltaTime).toDouble()).toFloat()
        }


        camera.position.setLength(cameraDistance)
        camera.up.set(Vector3.Y)
        camera.lookAt(Vector3.Zero)
        camera.update()


        maskBuffer.use(color = Color.WHITE) { // Automatically handles begin/end with exception safety
            for (model in sceneManager.renderableProviders) {
                val color = maskBuffer.reserveColor(model)
                fixedColorShader.setColor(color)
                // ineffective, but I don't want to debug `effective` way now
                maskBufferModelBatch.use(camera) {
                    render(model)
                }
            }
        }

        // render
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)
        sceneManager.update(deltaTime)
        sceneManager.render()

        spriteBatch.use {
            if (Gdx.input.isKeyJustPressed(com.badlogic.gdx.Input.Keys.NUM_1)) {
                if (maskSize == 1f) {
                    maskSize = 0.25f
                } else {
                    maskSize = 1f
                }
            }

            spriteBatch.draw(
                maskBuffer.colorTexture,
                Gdx.graphics.width.toFloat() * (1f - maskSize), Gdx.graphics.height.toFloat() * (1f - maskSize),
                Gdx.graphics.width.toFloat() * maskSize, Gdx.graphics.height.toFloat() * maskSize,
                0, 0,
                maskBuffer.colorTexture.width, maskBuffer.colorTexture.height,
                false, true // Flip vertically because FrameBuffer textures are Y-flipped
            )

            // Display FPS counter in the top left corner
            font.draw(
                spriteBatch,
                "FPS: ${Gdx.graphics.getFramesPerSecond()}",
                10f,
                Gdx.graphics.getHeight() - font.getLineHeight()
            )
        }
    }

    override fun dispose() {
        disposableContainer.dispose()
    }
}