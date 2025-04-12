package edu.cornell.cis3152.physics.platform;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import edu.cornell.gdiac.graphics.SpriteSheet;

public class Animator {
    public Animation<TextureRegion> animation;
    private SpriteSheet spriteSheet;
    private float frameDuration;
    private float stateTime;
    private int frameCount;
    private boolean looping;

    private int rows;
    private int cols;

    public Animator(Texture spriteSheet, int rows, int cols, float frameDuration, int frameCount) {
        this(spriteSheet,rows,cols,frameDuration,frameCount,true);
    }

    public Animator(Texture spriteSheet, int rows, int cols, float frameDuration, int frameCount, boolean looping) {
        this.spriteSheet = new SpriteSheet(spriteSheet, rows, cols);
        this.rows = rows;
        this.cols = cols;
        stateTime = 0f;
        this.frameDuration = frameDuration;
        this.frameCount = frameCount;
        this.looping = looping;
    }

    public TextureRegion getCurrentFrame(float delta) {
        stateTime += delta;
        // Calculate frame index based on elapsed time and frame duration
        int frameIndex;
        if (looping) {
            frameIndex = (int)(stateTime / frameDuration) % frameCount;
        } else {
            frameIndex = (int)(stateTime / frameDuration);
            if (frameIndex >= frameCount) {
                frameIndex = frameCount - 1;
            }
        }
        // Set the active frame in the sprite sheet
        spriteSheet.setFrame(frameIndex);
        return spriteSheet;
    }

    public void reset() {
        stateTime = 1.3f; // IT SHOULD BE 0f just rn cause the only use is for jump sprite i change it
    }

    public TextureRegion getKeyFrame(int i) {
        spriteSheet.setFrame(i);
        return spriteSheet;
    }
}
