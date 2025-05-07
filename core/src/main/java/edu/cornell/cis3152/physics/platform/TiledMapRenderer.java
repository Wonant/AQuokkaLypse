package edu.cornell.cis3152.physics.platform;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.MapGroupLayer;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.tiles.StaticTiledMapTile;
import edu.cornell.gdiac.graphics.SpriteBatch;

public class TiledMapRenderer {
    private final TiledMap map;
    private final SpriteBatch batch;
    private final float unitScale;

    /**
     * @param map       the loaded TiledMap
     * @param batch     the SpriteBatch *you* want to use
     * @param unitScale how many world units per pixel (e.g. 1/32f if your tiles are 32px and your physics world is in meters)
     */
    public TiledMapRenderer(TiledMap map, SpriteBatch batch, float unitScale) {
        this.map       = map;
        this.batch     = batch;
        this.unitScale = unitScale;
    }

    private static float getParallax(MapLayer layer, String key){
        String raw = layer.getProperties().get(key, String.class);
        return raw != null ? Float.parseFloat(raw) : 1f;
    }

    private void renderLayersHelper(MapLayer layer, OrthographicCamera camera, float PX, float PY) {

        float layerPx = layer.getParallaxX();
        float layerPy = layer.getParallaxY();
        PY *= layerPy;
        PX *= layerPx;

        if (layer instanceof MapGroupLayer) {
            for (MapLayer l : ((MapGroupLayer) layer).getLayers()) {
                renderLayersHelper(l, camera, PX, PY);
            }
            return;
        }

        if (!(layer instanceof TiledMapTileLayer)) {
            return;
        }


        TiledMapTileLayer tileLayer = (TiledMapTileLayer)layer;
        float tileW = tileLayer.getTileWidth(); // * unitScale;
        float tileH = tileLayer.getTileHeight();// * unitScale;

        float offX = camera.position.x - camera.viewportWidth  * 0.5f;
        float offY = camera.position.y - camera.viewportHeight * 0.5f;
        offX *= (1f - PX);
        offY *= (1f - PY);


        for (int y = 0; y < tileLayer.getHeight(); y++) {
            for (int x = 0; x < tileLayer.getWidth(); x++) {
                TiledMapTileLayer.Cell cell = tileLayer.getCell(x, y);
                if (cell == null) continue;

                TiledMapTile tile = cell.getTile();
                if (tile == null) continue;

                TextureRegion region = new TextureRegion(tile.getTextureRegion());

                if (cell.getFlipHorizontally()) region.flip(true, false);
                if (cell.getFlipVertically()) region.flip(false, true);
                int   rotSteps = cell.getRotation();          // 0..3
                float degrees  = rotSteps * 90f;


                float worldX = x * tileW + offX;
                float worldY = y * tileH + offY;

                batch.draw(
                    region,
                    worldX, worldY,      // bottom‐left corner
                    worldX + tileW * 0.5f,        // originX (center)
                    worldY + tileH * 0.5f,        // originY (center)
                    tileW,               // width
                    tileH,               // height
                    1f,                  // scaleX
                    1f,                  // scaleY
                    degrees
                );
            }
        }
    }

    /**
     * Draws *all* TileLayers in the order they appear in the map.
     */
    public void renderAllLayers(OrthographicCamera camera) {
        // tell the batch to use the camera’s combined matrix
        batch.setProjectionMatrix(camera.combined);
        batch.begin();


        for (MapLayer layer : map.getLayers()) {
            renderLayersHelper(layer, camera,1f, 1f);
        }

        batch.end();
    }
}
