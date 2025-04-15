package edu.cornell.cis3152.physics.platform;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.physics.box2d.Fixture;
import edu.cornell.gdiac.physics2.BoxObstacle;
import edu.cornell.gdiac.physics2.ObstacleSprite;

public class Door extends ObstacleSprite {

    /**
     * Creates a door obstacle.
     *
     * The door is a rectangle obstacle with a fixed size (2x2 world units)
     * that physically collides with objects. Its collision filtering is set so
     * that only the player collides with it.
     *
     * @param units  The physics units for scaling. (Typically 1 if world units are used directly.)
     * @param x      The x-coordinate of the door’s center.
     * @param y      The y-coordinate of the door’s center.
     */
    public Door(float units, float x, float y, float width, float height) {
        super();

        obstacle = new BoxObstacle(x + width/2, y + height/2, width, height);
        obstacle.setPhysicsUnits(units);
        obstacle.setBodyType(BodyDef.BodyType.StaticBody);
        // We are not marking the door as a sensor; it will physically interact.
        obstacle.setUserData(this);
        obstacle.setName("door");
        obstacle.setSensor(true);

        // Set a default debug color (can be adjusted later).
        debug = Color.WHITE;

        // Create a rectangular mesh; meshes attached to physics bodies
        // are centered at (0,0), so adjust accordingly.
//        mesh.set(-width * units / 2.0f, -height * units / 2.0f, width * units, height * units);
        mesh.set(0, 0, 0, 0);
    }

    /**
     * Sets the collision filtering so that the door only collides with
     * objects in the player category.
     */
    public void setFilter() {
        for (Fixture fixture : getObstacle().getBody().getFixtureList()) {
            Filter filter = fixture.getFilterData();
            // Treat the door as scenery
            filter.categoryBits = CollisionFiltering.CATEGORY_SCENERY;
            // Only allow collisions with the player.
            filter.maskBits = CollisionFiltering.CATEGORY_PLAYER;
            fixture.setFilterData(filter);
        }
    }

}
