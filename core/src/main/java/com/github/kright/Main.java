package com.github.kright;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.loader.ObjLoader;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.ScreenUtils;

import java.util.ArrayList;

/**
 * {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms.
 */
public class Main extends ApplicationAdapter {

    private static final int objectsX = 10;
    private static final int objectsZ = 10;

    private SpriteBatch batch;
    private Texture image;
    private BitmapFont font;

    // 3D rendering components
    private PerspectiveCamera camera;
    private ModelBatch modelBatch;
    private Model model;
    private Environment environment;
    private final ArrayList<ModelInstance> instances = new ArrayList<ModelInstance>();

    @Override
    public void create() {
        batch = new SpriteBatch();
        image = new Texture("libgdx.png");
        font = new BitmapFont();

        // Initialize 3D components
        modelBatch = new ModelBatch();

        // Set up camera
        camera = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        float cameraDist = Math.max(objectsX, objectsX) * 1.5f;
        Vector3 cameraPos = new Vector3(5f, 5f, 2f).setLength(cameraDist);
        camera.position.set(cameraPos);
        camera.lookAt(0, 0, 0);
        camera.near = 0.1f;
        camera.far = 100f;
        camera.update();

        // Set up lighting environment
        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

        // Load the sphere model
        ObjLoader loader = new ObjLoader();
        model = loader.loadModel(Gdx.files.internal("Sphere128x64.obj"));

        for (int z = 0; z < objectsZ; z++) {
            for (int x = 0; x < objectsX; x++) {
                ModelInstance instance = new ModelInstance(model);
                instance.transform.translate((x - objectsX * 0.5f) * 2f, 0, (z - objectsZ * 0.5f) * 2f);
                instances.add(instance);
            }
        }
    }

    @Override
    public void render() {
        // Clear the screen and depth buffer
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        // Update camera
        camera.update();

        // Render 3D model
        modelBatch.begin(camera);

        for (ModelInstance instance : instances) {
            instance.transform.rotate(0, 1, 0, 0.5f);
            modelBatch.render(instance, environment);
        }

        modelBatch.end();

        // Render 2D sprite on top if needed
        batch.begin();
        batch.draw(image, 140, 210);

        // Display FPS counter in the top left corner
        font.draw(batch, "FPS: " + Gdx.graphics.getFramesPerSecond(), 10, Gdx.graphics.getHeight() - font.getLineHeight());
        font.draw(batch, "triangles count: " + 16128 * objectsX * objectsZ, 10, Gdx.graphics.getHeight() - font.getLineHeight() * 2);

        batch.end();
    }

    @Override
    public void dispose() {
        // Dispose 2D resources
        batch.dispose();
        image.dispose();
        font.dispose();

        // Dispose 3D resources
        modelBatch.dispose();
        model.dispose();
    }
}
