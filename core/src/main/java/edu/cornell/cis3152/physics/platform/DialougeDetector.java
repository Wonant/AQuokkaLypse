package edu.cornell.cis3152.physics.platform;

import edu.cornell.gdiac.physics2.ObstacleSprite;

public class DialougeDetector extends ObstacleSprite {

    private String message;
    private float x;
    private float y;
    /** Whether the player is currently in front of the dialouge */
    private boolean active;

    public DialougeDetector(float x, float y, String text){
        message = text;
        this.x = x;
        this.y = y;
    }

    /** returns true if the player is in front of the dialouge */
    public boolean isActive(){
        return active;
    }
    /** set active to true */
    public void setActive(){
        active = true;
    }
    /** set active to false*/
    public void setInactive(){
        active = false;
    }

    public String getMessage(){return message;}
}
