package edu.cornell.cis3152.physics.platform;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.ParserUtils;
import edu.cornell.gdiac.physics2.ObstacleSprite;
import edu.cornell.gdiac.physics2.PolygonObstacle;
import edu.cornell.gdiac.math.Poly2;

import com.badlogic.gdx.physics.box2d.Filter;

public class Spear extends ObstacleSprite {

    public Spear(float units, JsonValue settings, Vector2 pos, float direction) {
        float offset = settings.getFloat("offset", 0);
        float s = settings.getFloat("size");
        float width = units * s / 1.5f;
        float height = units * s / 15.0f;


        // Create a long rectangular polygon for the spear shape
        Poly2 p = new Poly2(-width/2, -height/2, width, height);
        obstacle = new PolygonObstacle(p, pos.x + direction * 1, pos.y + 1);
        obstacle.setDensity(50);
        obstacle.setPhysicsUnits(units);
        obstacle.setBullet(true);
        obstacle.setGravityScale(0);
        obstacle.setUserData(this);
        obstacle.setName("spear");
        obstacle.setFixedRotation(true);

        float speed = settings.getFloat("speed", 0);
        obstacle.setVX(speed * direction);
        obstacle.setVY(0);

        debug = ParserUtils.parseColor(settings.get("debug"), Color.GOLD);

        // Set the visual mesh for the spear
        float radius = height / 2;
        mesh.set(-width / 2, -radius, width, 2 * radius);
    }

    public void update() {
        obstacle.setVX(obstacle.getVX() * 0.98f);
    }

    public float getV() {
        return obstacle.getVX();
    }
}

