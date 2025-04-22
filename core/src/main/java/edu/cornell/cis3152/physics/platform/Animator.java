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

    private int startFrame;
    private int endFrame;

    private int rows;
    private int cols;

    public Animator(Texture spriteSheet, int rows, int cols, float frameDuration, int frameCount, int startFrame, int endFrame) {
        this(spriteSheet,rows,cols,frameDuration,frameCount,startFrame, endFrame, true);
    }

    public Animator(Texture spriteSheet, int rows, int cols, float frameDuration, int frameCount, int startFrame, int endFrame, boolean looping) {
        this.spriteSheet = new SpriteSheet(spriteSheet, rows, cols);
        this.rows = rows;
        this.cols = cols;
        stateTime = 0f;
        this.frameDuration = frameDuration;
        this.frameCount = frameCount;
        this.looping = looping;
        this.startFrame = startFrame;
        this.endFrame = endFrame;
    }

    public TextureRegion getCurrentFrame(float delta) {
        stateTime += delta;
        // Calculate frame index based on elapsed time and frame duration
        int frameIndex;
        if (looping) {
            frameIndex = startFrame + (int)(stateTime / frameDuration) % (endFrame - startFrame + 1);

        } else {
            frameIndex = startFrame + (int)(stateTime / frameDuration);
            if (frameIndex > endFrame) {
                frameIndex = endFrame - 1;
            }

        }
        // Set the active frame in the sprite sheet
        spriteSheet.setFrame(frameIndex);
        return spriteSheet;
    }

    public void reset() {
        stateTime = 0f;
    }

    public boolean isAnimationFinished() {
        if (looping) {
            return false;
        }
        // total frames in this segment
        int totalFrames = endFrame - startFrame + 1;
        // total animation length
        float animationLength = totalFrames * frameDuration;
        return stateTime >= animationLength;
    }

    public TextureRegion getKeyFrame(int i) {
        int actualIndex = startFrame + i;
        if (actualIndex > endFrame) actualIndex = endFrame;
        spriteSheet.setFrame(actualIndex);
        return spriteSheet;
    }
}
