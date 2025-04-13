package edu.cornell.cis3152.physics.platform;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.assets.ParserUtils;
import edu.cornell.gdiac.physics2.BoxObstacle;
import edu.cornell.gdiac.physics2.ObstacleSprite;

import static edu.cornell.cis3152.physics.platform.CollisionFiltering.*;

public class Spear extends ObstacleSprite {

    public Spear(float units, JsonValue settings, Vector2 pos, Vector2 angle, AssetDirectory directory) {
        float offset = settings.getFloat("offset", 0);
        Vector2 v_offset = angle.cpy().scl(offset);
        float width = settings.getFloat("width", 0.5f);
        float height = settings.getFloat("height", 0.1f);

        obstacle = new BoxObstacle(pos.x + v_offset.x, pos.y + v_offset.y, width, height);
        obstacle.setDensity(settings.getFloat("density", 1.0f));
        obstacle.setPhysicsUnits(units);
        obstacle.setBullet(true);
        obstacle.setGravityScale(0);
        obstacle.setUserData(this);
        obstacle.setName("spear");

        float speed = settings.getFloat("speed", 0);
        obstacle.setVX(speed * angle.x);
        obstacle.setVY(speed * angle.y);
        debug = ParserUtils.parseColor(settings.get("debug"), Color.WHITE);

        float pxWidth = width * units;
        float pxHeight = height * units;
        mesh.set(-pxWidth / 2, -pxHeight / 2, pxWidth, pxHeight);

        // ✅ Add texture
        setTexture(directory.getEntry("spear-texture", Texture.class));
    }

    public void setFilter() {
        for (Fixture spearFixture : getObstacle().getBody().getFixtureList()) {
            Filter spearFilter = spearFixture.getFilterData();
            spearFilter.categoryBits = CATEGORY_ENEMY_PROJECTILE;
            spearFilter.maskBits = CATEGORY_SCENERY | CATEGORY_PLAYER;
            spearFixture.setFilterData(spearFilter);
        }
    }
}
