package edu.cornell.cis3152.physics.platform;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.ParserUtils;
import edu.cornell.gdiac.physics2.ObstacleSprite;
import edu.cornell.gdiac.physics2.PolygonObstacle;
import edu.cornell.gdiac.math.Poly2;

public class ShieldWall extends ObstacleSprite {

    public ShieldWall(float units, JsonValue settings, Vector2 pos, float direction) {
        float offset = settings.getFloat( "offset", 0 );
        float s = settings.getFloat( "size" );
        float radius = s * units / 2.0f;

        // Create a rectangular obstacle
        Poly2 p = new Poly2(-units*s/32, -units*s/4, units*s/16, units*s/2);
        obstacle = new PolygonObstacle(p, pos.x + direction * 1, pos.y + 1);
        obstacle.setDensity(100);
        obstacle.setPhysicsUnits( units );
        obstacle.setBullet( true );
        obstacle.setGravityScale( 0 );
        obstacle.setUserData( this );
        obstacle.setName( "shield_wall" );
        obstacle.setFixedRotation(true);
        float speed = settings.getFloat( "speed", 0 );
        obstacle.setVX(speed*direction);
        obstacle.setVY(0);
        debug = ParserUtils.parseColor( settings.get( "debug" ), Color.PURPLE);

        // While the bullet is a circle, we want to create a rectangular mesh.
        // That is because the image is a rectangle. The width/height of the
        // rectangle should be the same as the diameter of the circle (adjusted
        // by the physics units). Note that radius has ALREADY been multiplied
        // by the physics units. In addition, for all meshes attached to a
        // physics body, we want (0,0) to be in the center of the mesh. So
        // the method call below is (x,y,w,h) where x, y is the bottom left.
        mesh.set( -radius, 20*-radius, 5 * radius, 40 * radius );
    }
    public void update(){
        obstacle.setVX(obstacle.getVX()*0.97f);
    }
    public float getV(){
        return obstacle.getVX();
    }

}
