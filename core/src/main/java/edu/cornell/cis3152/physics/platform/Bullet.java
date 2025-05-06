/*
 * Bullet.java
 *
 * This class is a ObstacleSprite referencing a bullet. All it does is override
 * the constructor. We do this for organizational purposes. Otherwise we have
 * to put a lot of initialization code in the scene, and that just makes the
 * scene too long and unreadable.
 *
 * Based on the original PhysicsDemo Lab by Don Holden, 2007
 *
 * Author:  Walker M. White
 * Version: 2/8/2025
 */
 package edu.cornell.cis3152.physics.platform;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.ParserUtils;
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.physics2.Obstacle;
import edu.cornell.gdiac.physics2.ObstacleSprite;
import edu.cornell.gdiac.physics2.WheelObstacle;

import static edu.cornell.cis3152.physics.platform.CollisionFiltering.*;


public class Bullet extends ObstacleSprite {

    private Animator bulletSprite;
    private Animator bulletEndSprite;
    public Vector2 angle;
    private float timeAlive;
    private float speed;
    private Vector2 direction;
    private float width, height;

    /**
     * Creates a bullet with the given physics units and settings
     *
     * The physics units are used to size the mesh relative to the physics
     * body. The other attributes (pos, right) are used to position to bullet
     * relative to Traci.
     *
     * @param units     The physics units
     * @param settings  The bullet physics constants
     * @param pos       Traci's position
     */
    public Bullet(float units, JsonValue settings, Vector2 pos, Vector2 angle, Texture animation, Texture endAnimation) {
        float offset = settings.getFloat( "offset", 0 );
        Vector2 v_offset = angle.scl(offset);
        float s = settings.getFloat( "size" );
        float radius = s * units / 2.0f;
        this.angle = angle;

        // Create a circular obstacle
        obstacle = new WheelObstacle( pos.x + v_offset.x, pos.y + v_offset.y, s/2 );
        obstacle.setDensity(0);
        obstacle.setPhysicsUnits( units );
        obstacle.setBullet( true );
        obstacle.setGravityScale( 0 );
        obstacle.setUserData( this );
        obstacle.setName( "bullet" );

        speed = settings.getFloat( "speed", 0 );
        float vx = speed * angle.x;
        float vy = speed * angle.y;

        obstacle.setVX( vx );
        obstacle.setVY( vy );
        debug = ParserUtils.parseColor( settings.get( "debug" ), Color.WHITE );

        // While the bullet is a circle, we want to create a rectangular mesh.
        // That is because the image is a rectangle. The width/height of the
        // rectangle should be the same as the diameter of the circle (adjusted
        // by the physics units). Note that radius has ALREADY been multiplied
        // by the physics units. In addition, for all meshes attached to a
        // physics body, we want (0,0) to be in the center of the mesh. So
        // the method call below is (x,y,w,h) where x, y is the bottom left.
        mesh.set( -radius, -radius, 2 * radius, 2 * radius );
        width = radius;
        height = radius;
        bulletSprite = new Animator(animation, 1, 5, 0.066f, 5, 0, 4);
        bulletEndSprite = new Animator(endAnimation, 1, 3, 0.066f, 3, 0, 2, false);
        timeAlive = 0;
        this.direction   = angle.cpy();
    }

    public float getTimeAlive() {
        return timeAlive;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }



    @Override
    public void update(float dt) {
        timeAlive += dt;

        super.update(dt);
    }

    @Override
    public void draw(SpriteBatch batch) {

        TextureRegion frame = new TextureRegion();
        frame = bulletSprite.getCurrentFrame(Gdx.graphics.getDeltaTime());

        float u = obstacle.getPhysicsUnits();
        float posX = obstacle.getX() * u;
        float posY = obstacle.getY() * u;
        float drawWidth = frame.getRegionWidth()/10f;
        float drawHeight = frame.getRegionHeight()/10f;

        float originX = drawWidth/1.3f;
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

    public void setFilter() {
        for(Fixture bulletFixture : getObstacle().getBody().getFixtureList()) {
            Filter bulletFilter = bulletFixture.getFilterData();
            bulletFilter.categoryBits = CATEGORY_BULLET;
            bulletFilter.maskBits = CATEGORY_SCENERY | CATEGORY_ENEMY; // Collide with walls/scenery
            bulletFixture.setFilterData(bulletFilter);
        }

    }

}
