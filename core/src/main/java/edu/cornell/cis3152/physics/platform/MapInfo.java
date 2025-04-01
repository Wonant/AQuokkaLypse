package edu.cornell.cis3152.physics.platform;


import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.tiled.*;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Vector2;
import edu.cornell.gdiac.assets.AssetDirectory;

public class MapInfo {
    private TiledMap map;
    private int levelID;
    private OrthogonalTiledMapRenderer mapRenderer;

    /** conversion constats */
    public static int PIXELS_PER_WORLD_METER = 32;
    public static Vector2 TILE_SIZE = new Vector2(PIXELS_PER_WORLD_METER, PIXELS_PER_WORLD_METER);

    public MapInfo(String fileName) {
        this.map = new TmxMapLoader().load(fileName);
        this.mapRenderer = new OrthogonalTiledMapRenderer(map);
    }

    public TiledMap getMap() {
        return map;
    }

    public void render(OrthographicCamera camera) {
        mapRenderer.setView(camera);
        mapRenderer.render();
    }


    public void disposeMap() {
        if (map != null) {
            map.dispose();
        }
    }
}
