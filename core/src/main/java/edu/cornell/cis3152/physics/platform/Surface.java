/*
 * Surface.java
 *
 * This class is a ObstacleSprite referencing either a wall or a platform. All
 * it does is override the constructor. We do this for organizational purposes.
 * Otherwise we have to put a lot of initialization code in the scene, and that
 * just makes the scene too long and unreadable.
 *
 * Note that we have similar classes in the other scenes (rocket and ragdoll).
 * We do this because we want to keep each mini-game self-contained.
 *
 * Based on the original PhysicsDemo Lab by Don Holden, 2007
 *
 * Author:  Walker M. White
 * Version: 2/8/2025
 */
 package edu.cornell.cis3152.physics.platform;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.ParserUtils;
import edu.cornell.gdiac.math.Poly2;
import edu.cornell.gdiac.math.PolyTriangulator;
import edu.cornell.gdiac.physics2.ObstacleSprite;
import edu.cornell.gdiac.physics2.PolygonObstacle;

/**
 * A class representing a tiled surface (wall or platform)
 *
 * An ObstacleSprite is a sprite (specifically a textured mesh) that is
 * connected to a obstacle. It is designed to be the same size as the
 * physics object, and it tracks the physics object, matching its position
 * and angle at all times.
 *
 * This class demonstrates WHY we use meshes, even though we did not use them
 * in earlier labs. For a surface, we do not want to draw a simple rectangular
 * image. This time we want to tile a texture on a polygonal shape. Creating
 * such tiles is something the designers had to do in Lab 2. The nice thing
 * about ObstacleSprite, is that you can have a mesh with the exact same shape
 * as the physics body (adjusted for physics units), and then apply a texture
 * to that shape.
 */
public class Surface extends ObstacleSprite {

    private boolean shadowed;

    private float width;

    public boolean isShadowed() {
        return shadowed;
    }

    public void setShadowed(boolean value) {
        shadowed = value;
    }

    public float getWidth(){
        return width;
    }

    /**
     * Creates a surface from the given set of points and physics units
     *
     * The points are in box2d space, not drawing space. They will be scaled
     * by the physics units when draw. The points define the outline of the
     * shape. To work correctly, the points must be specified in counterclockwise
     * order, and the line segments may not cross.
     *
     * @param points    The outline of the shape as x,y pairs
     * @param units     The physics units
     * @param data      The physics constants for this rope bridge
     */
    public Surface(float[] points, float units, JsonValue settings, boolean shadowed) {
        super();

        float tile = settings.getFloat( "tile" );

        // Construct a Poly2 object, breaking it into triangles
        Poly2 poly = new Poly2();
        PolyTriangulator triangulator = new PolyTriangulator();
        triangulator.set(points);
        triangulator.calculate();
        triangulator.getPolygon(poly);

        obstacle = new PolygonObstacle(points);
        obstacle.setBodyType( BodyDef.BodyType.StaticBody );
        obstacle.setDensity( settings.getFloat( "density", 0 ) );
        obstacle.setFriction( settings.getFloat( "friction", 0 ) );
        obstacle.setRestitution( settings.getFloat( "restitution", 0 ) );
        obstacle.setPhysicsUnits( units );
        obstacle.setUserData( this );

        debug = ParserUtils.parseColor( settings.get("debug"),  Color.WHITE);

        this.shadowed = shadowed;
        width = points[1] - points[0];

        // Create a polygon mesh matching the physics body, adjusted by the
        // physics units. We take the save polygon we used to create the
        // physics obstacle and scale it up. We then use that to set the
        // mesh. The attribute tile is used to define how we scale/stretch
        // the texture to fit to the polygon. Try experimenting with this in
        // the JSON to see what happens.
        poly.scl( units );
        mesh.set(poly,tile,tile);
    }

}
