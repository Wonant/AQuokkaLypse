/*
 * Door.java
 *
 * This class is a ObstacleSprite referencing the "win door". All it does is
 * override the constructor. We do this for organizational purposes. Otherwise
 * we have to put a lot of initialization code in the scene, and that just makes
 * the scene too long and unreadable.
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
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.ParserUtils;
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.physics2.BoxObstacle;
import edu.cornell.gdiac.physics2.ObstacleSprite;

/**
 * The win door.
 *
 * An ObstacleSprite is a sprite (specifically a textured mesh) that is
 * connected to a obstacle. It is designed to be the same size as the
 * physics object, and it tracks the physics object, matching its position
 * and angle at all times.
 *
 * The reason we use a textured mesh instead of a image is because it allows
 * us more control over the size and shape of the image. We will talk about
 * how to use these later in class. For now, just notice how we create meshes.
 *
 * The associated obstacle is a sensor. That means that collisions will be
 * detected, but nothing happens to the game physics. Instead, we decide the
 * result of the collision.
 */
public class Shard extends ObstacleSprite {
    private boolean pickedUp;
    public int id;

    private Animator sprite;
    private TextureRegion currentFrame;
    private float timeAlive;


    /**
     * Creates a door with the given physics units and settings
     *
     * The physics units are used to size the mesh relative to the physics
     * body. All other attributes are defined by the JSON file
     *
     * @param units     The physics units
     * @param settings  The door physics constants
     */
    public Shard(float units, JsonValue settings, float x, float y, int id, Texture sprite) {
        super();

        this.id = id;

        float s = settings.getFloat( "size" );
        float size = s*units;

        obstacle = new BoxObstacle(x, y, s*2, s*2);
        obstacle.setDensity(settings.getFloat("density", 0));
        obstacle.setFriction(settings.getFloat("friction", 0));
        obstacle.setRestitution(settings.getFloat("restitution", 0));
        obstacle.setPhysicsUnits(units);
        obstacle.setBodyType(BodyDef.BodyType.StaticBody);
        obstacle.setSensor(true);
        obstacle.setUserData(this);
        obstacle.setName("goal");


        debug = ParserUtils.parseColor( settings.get("debug"),  Color.WHITE);

        // Create a rectangular mesh the same size as the door, adjusted by
        // the physics units. For all meshes attached to a physics body, we
        // want (0,0) to be in the center of the mesh. So the method call below
        // is (x,y,w,h) where x, y is the bottom left.
        mesh.set(-size/2.0f,-size/2.0f,size,size);
        this.sprite = new Animator(sprite, 1, 1, 0.1f, 1, 0, 0);
        timeAlive = 0;
    }

    public void setFilter() {
        for (Fixture fixture : getObstacle().getBody().getFixtureList()) {
            // Because it's a sensor, we only want to detect collisions with the Player
            Filter filter = fixture.getFilterData();
            // Treat this shard as SCENERY for collision category
            filter.categoryBits = CollisionFiltering.CATEGORY_SCENERY;
            // Only collide with the player
            filter.maskBits = CollisionFiltering.CATEGORY_PLAYER | CollisionFiltering.CATEGORY_ENEMY;
            fixture.setFilterData(filter);
        }
    }

    @Override
    public void update(float dt) {
        timeAlive += Gdx.graphics.getDeltaTime();

        obstacle.setPosition(obstacle.getX(), obstacle.getY() + 0.01f* (float) Math.sin(timeAlive*4));
        super.update(dt);
    }

    @Override
    public void draw(SpriteBatch batch) {
        TextureRegion frame = sprite.getCurrentFrame(Gdx.graphics.getDeltaTime());

        float u = obstacle.getPhysicsUnits();
        float posX = obstacle.getX() * u;
        float posY = obstacle.getY() * u;
        float drawWidth = frame.getRegionWidth()/2.5f;
        float drawHeight = frame.getRegionHeight()/2.5f;

        float originX = drawWidth/ 2f;
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
            0
        );
    }
}
