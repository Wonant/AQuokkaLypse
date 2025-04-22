package edu.cornell.cis3152.physics.platform;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.physics2.ObstacleSprite;
import edu.cornell.gdiac.physics2.PolygonObstacle;
import edu.cornell.gdiac.math.Poly2;

import static edu.cornell.cis3152.physics.platform.CollisionFiltering.CATEGORY_PLAYER;
import static edu.cornell.cis3152.physics.platform.CollisionFiltering.CATEGORY_SCENERY;

public class Spear extends ObstacleSprite {

    private boolean filterActivated;
    private float timeAlive;
    private float maxAge = 5;
    private boolean dead = false;
    private float direction; // -1 for left, 1 for right

    public Spear(float units, JsonValue settings, Vector2 pos, float direction) {
        timeAlive = 0;
        this.direction = direction;

        float width = 1.0f;
        float height = 0.2f;
        float halfW = units * width / 15;
        float halfH = units * height / 25;

        Poly2 p = new Poly2(-halfW, -halfH, halfW, halfH);
        obstacle = new PolygonObstacle(p, pos.x, pos.y);
        obstacle.setDensity(8.0f);
        obstacle.setPhysicsUnits(units);
        obstacle.setBullet(true);
        obstacle.setGravityScale(0);
        obstacle.setUserData(this);
        obstacle.setName("spear");
        obstacle.setFixedRotation(true);

        float speed = 22.5f;
        obstacle.setVX(speed * direction);
        obstacle.setVY(0);

        mesh.set(-halfW*32, -halfH*28, 2 * halfW*16, 2 * halfH*16);

    }

    public void update(float dt) {
        if (timeAlive > maxAge) {
            dead = true;
        }

        if (filterActivated) {
            timeAlive += dt;
            obstacle.setVX(obstacle.getVX() * 0.99f);
        }
        else if (obstacle.getBody() != null) {
            setFilter();
        }
    }

    public float getDirection() {
        return direction;
    }

    public void setFilter() {
        for (Fixture fixture : obstacle.getBody().getFixtureList()) {
            filterActivated = true;
            Object ud = fixture.getUserData();
            if (ud != null && ud.equals("player_sensor")) {
                continue;
            }
            Filter filter = fixture.getFilterData();
            filter.categoryBits = CATEGORY_SCENERY;
            filter.maskBits = CATEGORY_PLAYER;
            fixture.setFilterData(filter);
        }
    }

    public boolean isDead() {
        return dead;
    }
}

