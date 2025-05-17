package edu.cornell.cis3152.physics.platform;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
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

import static edu.cornell.cis3152.physics.platform.CollisionFiltering.CATEGORY_PLAYER;
import static edu.cornell.cis3152.physics.platform.CollisionFiltering.CATEGORY_SCENERY;

public class ShieldWall extends ObstacleSprite {

    private boolean filterActivated;
    private float timeAlive;
    private float maxAge = 1.5f;
    private boolean dead = false;
    private TextureRegion currentFrame;
    Texture travel;
    Texture end;
    private Animator travelSprite;
    private Animator endSprite;

    public ShieldWall(float units, JsonValue settings, Vector2 pos, float direction, Texture travelTex, Texture endTex) {
        float s = settings.getFloat( "size" );
        float radius = s * units / 2.0f;
        timeAlive = 0;

        // Create a rectangular obstacle
        Poly2 p = new Poly2(-units*s/32, -units*s/4, units*s/16, units*s/2);
        obstacle = new PolygonObstacle(p, pos.x, pos.y + 1);
        obstacle.setDensity(100);
        obstacle.setPhysicsUnits( units );
        obstacle.setBullet( true );
        obstacle.setGravityScale( 0 );
        obstacle.setUserData( this );
        obstacle.setName( "shield_wall" );
        obstacle.setFixedRotation(true);
        float speed = 5;
        obstacle.setVX(speed*direction);
        obstacle.setVY(0);
        debug = ParserUtils.parseColor( settings.get( "debug" ), Color.PURPLE);

        mesh.set( -radius, 20*-radius, 5 * radius, 40 * radius );
        travelSprite = new Animator(travelTex, 1, 4, 0.25f, 4, 0, 3, true);
        endSprite = new Animator(endTex, 1, 4, 0.15f, 4, 0, 3, false);
    }
    public void update(float dt){
        if (timeAlive > maxAge){
            dead = true;
        }

        if (filterActivated) {
            timeAlive += dt;
            obstacle.setVX(obstacle.getVX()*1.02f);
        }
        else if (obstacle.getBody() != null) setFilter();
        if (timeAlive < 2 * maxAge / 3f) {
            currentFrame = travelSprite.getCurrentFrame(dt);
        } else {
            currentFrame = endSprite.isAnimationFinished() ? endSprite.getLastFrame() : endSprite.getCurrentFrame(dt);
        }
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

    public boolean isDead(){
        return dead;
    }

    @Override
    public void draw(SpriteBatch batch) {

        TextureRegion frame = currentFrame;

        float u = obstacle.getPhysicsUnits();
        float posX = obstacle.getX() * u;
        float posY = obstacle.getY() * u;
        float drawWidth = frame.getRegionWidth()/5;
        float drawHeight = frame.getRegionHeight()/5;

        float originX = drawWidth/1.1f;
        if(obstacle.getVX() > 0){
            frame.flip(true,false);
            originX = drawWidth/8;
        }
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
