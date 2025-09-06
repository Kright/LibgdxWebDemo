package com.github.kright

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Cubemap
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.PerspectiveCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector3
import ktx.assets.DisposableContainer
import ktx.assets.DisposableRegistry

import net.mgsx.gltf.loaders.gltf.GLTFLoader;
import net.mgsx.gltf.scene3d.attributes.PBRCubemapAttribute;
import net.mgsx.gltf.scene3d.attributes.PBRTextureAttribute;
import net.mgsx.gltf.scene3d.lights.DirectionalLightEx;
import net.mgsx.gltf.scene3d.lights.DirectionalShadowLight
import net.mgsx.gltf.scene3d.scene.Scene;
import net.mgsx.gltf.scene3d.scene.SceneAsset;
import net.mgsx.gltf.scene3d.scene.SceneManager;
import net.mgsx.gltf.scene3d.scene.SceneSkybox;
import net.mgsx.gltf.scene3d.utils.IBLBuilder;


class GLTFQuickStartExample(private val disposableContainer: DisposableContainer = DisposableContainer()): ApplicationAdapter(), DisposableRegistry by disposableContainer {

    private lateinit var sceneManager: SceneManager
    private var camera: PerspectiveCamera? = null
    private var time = 0f

    override fun create() {
        // create scene

        val sceneAsset = GLTFLoader().load(Gdx.files.internal("neighbourhood_city_modular_lowpoly/scene.gltf")).alsoRegister()
        val scene = Scene(sceneAsset.scene)

        sceneManager = SceneManager().alsoRegister()
        sceneManager.addScene(scene)

        // setup camera (The BoomBox model is very small so you may need to adapt camera settings for your scene)
        camera = PerspectiveCamera(60f, Gdx.graphics.getWidth().toFloat(), Gdx.graphics.getHeight().toFloat())
        val d = 1000f
        camera!!.near = d / 1000f
        camera!!.far = d * 4
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
    }

    override fun render() {
        val deltaTime = Gdx.graphics.getDeltaTime()
        time += deltaTime

        // animate camera
        val v = camera!!.position.cpy().setFromSpherical(time * .3f, MathUtils.PI / 2 * 0.8f).scl(30f)
        camera!!.position.set(v.x, v.z, v.y)
        camera!!.up.set(Vector3.Y)
        camera!!.lookAt(Vector3.Zero)
        camera!!.update()

        // render
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)
        sceneManager.update(deltaTime)
        sceneManager.render()
    }

    override fun dispose() {
        disposableContainer.dispose()
    }
}