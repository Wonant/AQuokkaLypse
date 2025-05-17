package edu.cornell.cis3152.physics.platform;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Filter;
import com.badlogic.gdx.physics.box2d.Fixture;
import edu.cornell.gdiac.physics2.BoxObstacle;
import edu.cornell.gdiac.physics2.ObstacleSprite;

/**
 * A Gate behaves similarly to a Door but transports the player to a
 * specified exit location in world coordinates.
 */
public class Gate extends ObstacleSprite {

    /** Whether the player is currently in front of the gate */
    private boolean active;

    /** The exit location that the gate leads to */
    private Vector2 exitLocation;

    /**
     * Creates a gate obstacle.
     *
     * The gate is a rectangle obstacle with a fixed size (width x height)
     * that acts as a sensor. When the player enters, the gate becomes active,
     * and can transport the player to the exit location.
     *
     * @param units        The physics units for scaling.
     * @param x            The x-coordinate of the gate’s center.
     * @param y            The y-coordinate of the gate’s center.
     * @param width        The width of the gate in world units.
     * @param height       The height of the gate in world units.
     * @param exitLocation The world coordinate to which the gate exits.
     */
    public Gate(float units, float x, float y, float width, float height, Vector2 exitLocation) {
        super();
        this.active = false;
        this.exitLocation = exitLocation;

        obstacle = new BoxObstacle(x + width/2, y + height/2, width, height);
        obstacle.setPhysicsUnits(units);
        obstacle.setBodyType(BodyDef.BodyType.StaticBody);
        obstacle.setUserData(this);
        obstacle.setName("gate");
        obstacle.setSensor(true);

        debug = Color.WHITE;
        mesh.set(0, 0, 0, 0);
    }

    /** Returns the exit location of this gate */
    public Vector2 getExitLocation() {
        return exitLocation;
    }

    /** Returns true if the player is in front of the gate */
    public boolean isActive() {
        return active;
    }

    /** Marks the gate as active */
    public void setActive() {
        active = true;
    }

    /** Marks the gate as inactive */
    public void setInactive() {
        active = false;
    }

    /**
     * Configure collision filtering so that only the player interacts with the gate.
     */
    public void setFilter() {
        for (Fixture fixture : getObstacle().getBody().getFixtureList()) {
            Filter filter = fixture.getFilterData();
            filter.categoryBits = CollisionFiltering.CATEGORY_SCENERY;
            filter.maskBits = CollisionFiltering.CATEGORY_PLAYER;
            fixture.setFilterData(filter);
        }
    }
}
