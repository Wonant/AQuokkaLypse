package edu.cornell.cis3152.physics.platform;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.physics2.ObstacleSprite;
import edu.cornell.gdiac.physics2.PolygonObstacle;
import edu.cornell.gdiac.math.Poly2;

import com.badlogic.gdx.graphics.Texture;

import static edu.cornell.cis3152.physics.platform.CollisionFiltering.*;

public class Spear extends ObstacleSprite {

    private boolean filterActivated;
    private float timeAlive;
    private float maxAge = 2.0f; //
    private boolean dead = false;
    private float direction; // -1 for left, 1 for right
    private float speed = 22.5f;
    private TextureRegion currentFrame;
    private Animator travelAnimator;
    private Animator endAnimator;


    public Spear(float units, JsonValue settings, Vector2 pos, Vector2 velocity, Texture travelTex, Texture endTex) {
        timeAlive = 0f;
        this.direction = velocity.x >= 0 ? 1 : -1;

        float width = 1.0f;
        float height = 0.2f;

        if (direction > 0) {
            width = 0.7f;
            height = 0.15f;
        }

        float halfW = units * width / 10;
        float halfH = units * height / 25;

        Poly2 p;
        float offset = 0f;

        if (direction > 0) {
            offset = halfW * 1.2f;
            p = new Poly2(-halfW + offset, -halfH, halfW + offset, halfH);
        } else {
            p = new Poly2(-halfW, -halfH, halfW, halfH);
        }

        obstacle = new PolygonObstacle(p, pos.x, pos.y);
        obstacle.setDensity(8.0f);
        obstacle.setPhysicsUnits(units);
        obstacle.setBullet(true);
        obstacle.setGravityScale(0);
        obstacle.setUserData(this);
        obstacle.setName("spear");
        obstacle.setFixedRotation(true);

        obstacle.setVX(velocity.x);
        obstacle.setVY(velocity.y);

        if (direction > 0) {
            mesh.set((-halfW + offset) * 32, -halfH * 28, 2 * halfW * 16, 2 * halfH * 16);
        } else {
            mesh.set(-halfW * 32, -halfH * 28, 2 * halfW * 16, 2 * halfH * 16);
        }

        travelAnimator = new Animator(travelTex, 1, 5, 0.2f, 5, 0, 4, true);
        endAnimator = new Animator(endTex, 1, 5, 0.2f, 5, 0, 4, false);
    }

    public void update(float dt) {
        if (timeAlive > maxAge) {
            dead = true;
        }

        if (filterActivated) {
            timeAlive += dt;
            obstacle.setVX(obstacle.getVX() * 0.99f);
            if (timeAlive < maxAge / 2f) {
                currentFrame = travelAnimator.getCurrentFrame(dt);
            } else {
                if (endAnimator.isAnimationFinished()) {
                    currentFrame = endAnimator.getLastFrame();
                } else {
                    currentFrame = endAnimator.getCurrentFrame(dt);
                }
            }
        } else if (obstacle.getBody() != null) {
            setFilter();
        }
        Vector2 vel = new Vector2(obstacle.getVX(), obstacle.getVY());
        if (vel.len2() > 0.01f) {
            float angle = (vel.angleDeg() + 180f) % 360f; // 取得当前速度向量的角度
            obstacle.setAngle((float) Math.toRadians(angle));
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
            filter.categoryBits = CATEGORY_BULLET;
            filter.maskBits = CATEGORY_PLAYER | CATEGORY_SCENERY;
            fixture.setFilterData(filter);
        }
    }

    public boolean isDead() {
        return dead;
    }

    public void drawOwnAnimation(edu.cornell.gdiac.graphics.SpriteBatch batch) {
        if (currentFrame == null) return;

        float u = obstacle.getPhysicsUnits();
        float drawX = obstacle.getX() * u;
        float drawY = obstacle.getY() * u;
        float scale = 0.25f;

        float width = currentFrame.getRegionWidth() * scale;
        float height = currentFrame.getRegionHeight() * scale;

        if (direction > 0 && !currentFrame.isFlipX()) {
            currentFrame.flip(true, false);
        } else if (direction < 0 && currentFrame.isFlipX()) {
            currentFrame.flip(true, false);
        }

        float drawOffset = 0f;
        if (direction > 0) {
            drawOffset = width * 0.3f;
        }

        batch.draw(currentFrame,
            drawX - width / 2 + drawOffset,
            drawY - height / 2,
            width,
            height
        );
    }
}
