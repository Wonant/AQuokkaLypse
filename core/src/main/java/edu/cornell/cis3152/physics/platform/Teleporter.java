package edu.cornell.cis3152.physics.platform;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.utils.JsonValue;
import edu.cornell.gdiac.assets.ParserUtils;
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.physics2.BoxObstacle;
import edu.cornell.gdiac.physics2.Obstacle;
import edu.cornell.gdiac.physics2.ObstacleSprite;
import edu.cornell.gdiac.physics2.WheelObstacle;

public class Teleporter extends ObstacleSprite{

    /** Teleporter this is connected to**/
    private Teleporter linkedTeleporter;

    private float x;
    private float y;

    public Teleporter(float units, JsonValue settings)
    {
        super();

        float x = settings.get("pos").getFloat(0);
        float y = settings.get("pos").getFloat(1);
        float s = settings.getFloat( "size" );
        float size = s*units;

        obstacle = new BoxObstacle(x, y, s, s);

        obstacle.setDensity( settings.getFloat( "density", 0 ) );
        obstacle.setFriction( settings.getFloat( "friction", 0 ) );
        obstacle.setRestitution( settings.getFloat( "restitution", 0 ) );
        obstacle.setPhysicsUnits( units );
        obstacle.setBodyType( BodyDef.BodyType.StaticBody);
        obstacle.setSensor(true);
        obstacle.setUserData( this );
        obstacle.setName("goal");

        debug = ParserUtils.parseColor( settings.get("debug"),  Color.WHITE);

        // Create a rectangular mesh the same size as the door, adjusted by
        // the physics units. For all meshes attached to a physics body, we
        // want (0,0) to be in the center of the mesh. So the method call below
        // is (x,y,w,h) where x, y is the bottom left.
        mesh.set(-size/2.0f,-size/2.0f,size,size);
    }

    public Teleporter (Float units, JsonValue settings, Vector2 position)
    {
        super();

        this.x = position.x;
        this.y = position.y;
        float s = 1;
        float size = s*units;

        //linkedTeleporter = other;

        obstacle = new BoxObstacle(x, y, s, s);

        obstacle.setDensity( settings.getFloat( "density", 0 ) );
        obstacle.setFriction( settings.getFloat( "friction", 0 ) );
        obstacle.setRestitution( settings.getFloat( "restitution", 0 ) );
        obstacle.setPhysicsUnits( units );
        obstacle.setBodyType( BodyDef.BodyType.StaticBody);
        obstacle.setSensor(true);
        obstacle.setUserData( this );
        obstacle.setName("goal");

        debug = ParserUtils.parseColor( settings.get("debug"),  Color.WHITE);

        // Create a rectangular mesh the same size as the door, adjusted by
        // the physics units. For all meshes attached to a physics body, we
        // want (0,0) to be in the center of the mesh. So the method call below
        // is (x,y,w,h) where x, y is the bottom left.
        mesh.set(-size/2.0f,-size/2.0f,size,size);


    }

    public Teleporter getLinkedTeleporter()
    {
        return linkedTeleporter;
    }

    public void setLinkedTeleporter(Teleporter other) {
        linkedTeleporter = other;
    }

    public Vector2 getPosition(){
        return new Vector2(x, y);
    }



}
