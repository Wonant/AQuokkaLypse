package edu.cornell.cis3152.physics.platform;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.ParserUtils;
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.physics2.ObstacleSprite;
import edu.cornell.gdiac.physics2.PolygonObstacle;
import edu.cornell.gdiac.math.Poly2;
import com.badlogic.gdx.graphics.Texture;
import edu.cornell.gdiac.physics2.WheelObstacle;

import static edu.cornell.cis3152.physics.platform.CollisionFiltering.*;

public class Spear extends ObstacleSprite {

    private boolean filterActivated = false;
    private float timeAlive = 0f;
    private float delayTimer = 0f;

    private final float launchDelay = 0.25f; // Delay before launch
    private final float maxAge = 2.0f;

    private boolean dead = false;
    private boolean notLaunched = true;

    private Vector2 angle;
    private float speed;
    private float direction; // Optional usage if you want to mirror things

    private TextureRegion currentFrame;
    private Animator travelSprite;
    private Animator endAnimator;

    public Spear(float units, JsonValue settings, Vector2 pos, Vector2 angle, Texture travelTex, Texture endTex) {
        float s = settings.getFloat("size");
        float radius = s * units / 2.0f;
        this.angle = angle;

        // Create a circular physics obstacle
        obstacle = new WheelObstacle(pos.x + angle.x * 3, pos.y + angle.y * 3, s / 8);
        obstacle.setDensity(0);
        obstacle.setFriction(0);
        obstacle.setRestitution(0);
        obstacle.setPhysicsUnits(units);
        obstacle.setBullet(true);
        obstacle.setGravityScale(0);
        obstacle.setUserData(this);
        obstacle.setName("spear");

        // Start with zero velocity
        obstacle.setVX(0);
        obstacle.setVY(0);

        debug = ParserUtils.parseColor(settings.get("debug"), Color.WHITE);
        mesh.set(-radius, -radius, 2 * radius, 2 * radius);

        travelSprite = new Animator(travelTex, 1, 5, 0.15f, 5, 0, 4, true);
        endAnimator = new Animator(endTex, 1, 5, 0.15f, 5, 0, 4, false);

        speed = 15;
    }

    public void update(float dt) {
        if (dead) return;

        if (timeAlive > maxAge) {
            dead = true;
            return;
        }

        if (!filterActivated && obstacle.getBody() != null) {
            setFilter();
        }

        if (filterActivated) {
            timeAlive += dt;
            if (notLaunched) {
                delayTimer += dt;
                obstacle.setVX(0);
                obstacle.setVY(0);

                if (delayTimer >= launchDelay) {
                    notLaunched = false;
                    float vx = speed * angle.x;
                    float vy = speed * angle.y;
                    obstacle.setVY(vy);
                    obstacle.setVX(vx);

                }
            } else {

            }

            // Animation frame update
            if (timeAlive < maxAge / 2f) {
                currentFrame = travelSprite.getCurrentFrame(dt);
            } else {
                currentFrame = endAnimator.isAnimationFinished() ? endAnimator.getLastFrame() : endAnimator.getCurrentFrame(dt);
            }
        }
    }

    public void setFilter() {
        for (Fixture fixture : obstacle.getBody().getFixtureList()) {
            Object ud = fixture.getUserData();
            if (ud != null && ud.equals("player_sensor")) continue;

            Filter filter = fixture.getFilterData();
            filter.categoryBits = CATEGORY_ENEMY_PROJECTILE;
            filter.maskBits = CATEGORY_PLAYER;
            fixture.setFilterData(filter);
        }
        filterActivated = true;
    }

    public boolean isDead() {
        return dead;
    }

    @Override
    public void draw(SpriteBatch batch) {

        TextureRegion frame = currentFrame;

        float u = obstacle.getPhysicsUnits();
        float posX = obstacle.getX() * u;
        float posY = obstacle.getY() * u;
        float drawWidth = frame.getRegionWidth()/2f;
        float drawHeight = frame.getRegionHeight()/2f;
        frame.flip(true,false);

        float originX = drawWidth / 1.3f;
        float originY = drawHeight / 2f;

        batch.draw(frame,
            posX - originX, // lower-left x position
            posY - originY, // lower-left y position
            posX,        // originX used for scaling and rotation
            posY,        // originY
            drawWidth,      // width
            drawHeight,     // height
            1f,             // scaleX
            1f,             // scaleY
            (float) Math.toDegrees(Math.atan2(angle.y, angle.x))
        );
    }
}



