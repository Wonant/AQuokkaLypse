package edu.cornell.cis3152.physics.platform;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import edu.cornell.gdiac.graphics.Shader;
import edu.cornell.gdiac.graphics.SpriteBatch;

import java.util.List;

public class MinimapRenderer {
    private final TiledMap       map;
    private final SpriteBatch    batch;
    private final float          unitScale;
    private boolean              active = true;
    private float mapWidth, mapHeight;
    private Shader inkShader;

    public MinimapRenderer(TiledMap map, SpriteBatch batch, float unitScale, float width, float height) {
        this.map       = map;
        this.batch     = batch;
        this.unitScale = unitScale;

        mapWidth  = width  * unitScale;
        mapHeight = height * unitScale;

        inkShader = new Shader(
            Gdx.files.internal("shaders/ink.vert"),
            Gdx.files.internal("shaders/ink.frag"));
    }

    public void setActive(boolean value) { active = value; }
    public boolean isActive()         { return active; }

    /**
     * Draws the map scaled down to a half-screen inset.
     * Just swaps cameras—no glViewport or FBO needed.
     */
    public void render(Vector2 playerPos, OrthographicCamera miniCam, float time) {
        if (!active) { return; }

        Matrix4 oldProj = batch.getProjectionMatrix().cpy();
        Matrix4 oldTrans  = batch.getTransformMatrix();
        ShaderProgram oldShader = batch.getShader();

        batch.setProjectionMatrix(miniCam.combined);

        batch.setShader(inkShader);
        inkShader.bind();
        inkShader.setUniformf("iTime", time);
        inkShader.setUniformf("iResolution",
            Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        inkShader.setUniformf("u_center",
            Gdx.graphics.getWidth()  * 0.50f,     // or wherever you draw it
            Gdx.graphics.getHeight() * 0.50f);


        batch.begin();
        Matrix4 centreShift = new Matrix4().setToTranslation(
            -mapWidth  * 0.25f,
            0,
            0f);
        batch.setTransformMatrix(centreShift);

        for (MapLayer layer : map.getLayers()) {
            if (layer instanceof TiledMapTileLayer) {
                renderTileLayer((TiledMapTileLayer)layer);
            }
        }
        batch.end();



        // 4) restore main matrix
        batch.setTransformMatrix(oldTrans);
        batch.setProjectionMatrix(oldProj);
        batch.setShader(oldShader);
    }

    /** Copy of your tile-drawing loop (no changes) */
    private void renderTileLayer(TiledMapTileLayer layer) {
        float tileW = layer.getTileWidth();
        float tileH = layer.getTileHeight();
        for (int y = 0; y < layer.getHeight(); y++) {
            for (int x = 0; x < layer.getWidth(); x++) {
                TiledMapTileLayer.Cell cell = layer.getCell(x, y);
                if (cell == null || cell.getTile() == null) continue;
                TextureRegion region = new TextureRegion(cell.getTile().getTextureRegion());
                if (cell.getFlipHorizontally()) region.flip(true, false);
                if (cell.getFlipVertically())   region.flip(false, true);
                float deg = cell.getRotation() * 90f;
                float wx  = x * tileW;
                float wy  = y * tileH;
                batch.draw(region,
                    wx, wy,
                    tileW, tileH,
                    tileW, tileH,
                    1f, 1f,
                    deg);
            }
        }
    }
}
