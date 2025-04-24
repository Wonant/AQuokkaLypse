package edu.cornell.cis3152.physics;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Affine2;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.ScreenUtils;
import edu.cornell.gdiac.assets.*;
import edu.cornell.gdiac.audio.AudioEngine;
import edu.cornell.gdiac.audio.AudioSource;
import edu.cornell.gdiac.audio.MusicQueue;
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.util.*;
import com.badlogic.gdx.graphics.Texture;

/**
 * Class that provides a loading screen for the state of the game.
 *
 * This is a fairly generic loading screen that shows the GDIAC logo and a
 * progress bar. Once all assets are loaded, the progress bar is replaced
 * by a play button. You are free to adopt this to your needs.
 */
public class MainMenuScene implements Screen, InputProcessor {

    /** The background texture for the menu */
    private Texture background;
    /** The texture for the title */
    private Texture titleTexture;
    /** The textures for the menu buttons */
    private Texture[] buttonTextures;
    /** The textures for the menu buttons when hovered */
    private Texture[] buttonHoverTextures;

    // There are TWO asset managers.
    // One to load the loading screen. The other to load the assets
    /** The actual assets to be loaded */
    private AssetDirectory assets;

    /** The drawing camera for this scene */
    private OrthographicCamera camera;
    /** Reference to sprite batch created by the root */
    private SpriteBatch batch;
    /** Affine transform for displaying images */
    private Affine2 affine;
    /** Listener that will update the player mode when we are done */
    private ScreenListener listener;
    /** Viewport for maintaining aspect ratio */
    //private Viewport viewport;
    /** Button rectangles for hit detection */
    private Rectangle[] buttons;
    /** Current button being hovered */
    private int hoverButton = -1;

    /** The width of this scene */
    private int width;
    /** The height of this scene */
    private int height;

    /** The constants for arranging images on the screen */
    JsonValue constants;

    /** Scaling factor for when the student changes the resolution. */
    private float scale;
    /** Current progress (0 to 1) of the asset manager */
    private float progress;
    /** The current state of the play button */
    private int   pressState;

    /** Whether or not this player mode is still active */
    private boolean active;

    /** Exit code for starting the game */
    public static final int EXIT_PLAY = 0;
    /** Exit code for level select screen */
    public static final int EXIT_SETTINGS = 1;
    /** Exit code for options screen */
    public static final int EXIT_CREDITS = 2;
    /** Exit code for quitting the game */
    public static final int EXIT_EXIT = 3;

    /** Names of menu options */
    private final String[] MENU_OPTIONS = {"Start", "Settings", "Credits", "Exit"};

    /** The button currently being pressed down */
    private int pressedButton = -1;

    /** List of music to play */
    AudioSource samples[];
    /** The current music sample to play */
    int currentSample = 0;

    /** A queue to play music */
    MusicQueue music;


    /**
     * Returns the asset directory produced by this loading screen
     *
     * This asset loader is NOT owned by this loading scene, so it persists even
     * after the scene is disposed. It is your responsbility to unload the
     * assets in this directory.
     *
     * @return the asset directory produced by this loading screen
     */
    public AssetDirectory getAssets() {
        return assets;
    }

    /**
     * Creates a LoadingMode with the default size and position.
     *
     * The budget is the number of milliseconds to spend loading assets each animation
     * frame. This allows you to do something other than load assets. An animation
     * frame is ~16 milliseconds. So if the budget is 10, you have 6 milliseconds to
     * do something else. This is how game companies animate their loading screens.
     *
     *
     */
    public MainMenuScene(AssetDirectory assets, SpriteBatch batch) {
        this.batch  = batch;
        this.assets = assets;

        camera = new OrthographicCamera();
        //viewport = new FitViewport(1420, 799, camera);

        active = false;

        // Initialize buttons
        buttons = new Rectangle[MENU_OPTIONS.length];
        for (int i = 0; i < buttons.length; i++) {
            buttons[i] = new Rectangle();
        }

        System.out.println("Available assets keys: " + assets.getAssetNames());

        // Load assets
        background = assets.getEntry("background", Texture.class);
        //titleTexture = assets.getEntry("title", Texture.class);

        buttonTextures = new Texture[MENU_OPTIONS.length];
        buttonHoverTextures = new Texture[MENU_OPTIONS.length];
        for (int i = 0; i < MENU_OPTIONS.length; i++) {
            try {
                buttonTextures[i] = assets.getEntry("button" + i, Texture.class);
                buttonHoverTextures[i] = assets.getEntry("buttonhover" + i, Texture.class);
            } catch (Exception e) {
                // Fall back to default buttons if specific ones aren't available
                buttonTextures[i] = assets.getEntry("button", Texture.class);
                buttonHoverTextures[i] = assets.getEntry("buttonhover", Texture.class);
            }
        }

        samples = new AudioSource[1];
        samples[0] = assets.getEntry( "theme", AudioSource.class );
        currentSample = 0;

        AudioEngine engine = (AudioEngine)Gdx.audio;
        music = engine.newMusicQueue( false, 44100 );
        music.addSource( samples[0] );
        music.setLooping(true);

        //VOLUME CONTROL SHOULD PROBABLY BE IN A SETTINGS SCREEN
        if (music != null) {
            music.setVolume(1.0f);
        }

        Gdx.input.setInputProcessor(this);
    }

    /**
     * Called when this screen should release all resources.
     */
    public void dispose() {
        if (music != null) {
            music.stop();
        }

        for (AudioSource sample : samples) {
             if (sample != null) {
                 sample.dispose();
             }
        }
    }

    /**
     * Updates the status of this scene
     *
     * We prefer to separate update and draw from one another as separate
     * methods, instead of using the single render() method that LibGDX does.
     * We will talk about why we prefer this in lecture.
     *
     * @param delta Number of seconds since last animation frame
     */
    private void update(float delta) {

        if (music.getPosition() + 10 > music.getDuration()){
            music.stop();
            music.removeSource(0);
            music.addSource( samples[0] );
        }
    }

    /**
     * Draws the status of this player mode.
     *
     * We prefer to separate update and draw from one another as separate
     * methods, instead of using the single render() method that LibGDX does.
     * We will talk about why we prefer this in lecture.
     */
    private void draw() {
        ScreenUtils.clear(0.1f, 0.1f, 0.1f, 1);
        batch.begin(camera);

        // Draw background
        if (background != null) {
            batch.draw(background, 0, 0, width, height);
        } else {
            System.out.println("Background Drawing Failed");
        }

        // Draw title
        //if (titleTexture != null) {
        //    float titleX = width/2 - titleTexture.getWidth()/2;
        //    float titleY = height - titleTexture.getHeight() - 20;
        //    batch.draw(titleTexture, titleX, titleY);
        //}

        // Draw buttons
        float buttonSpacing = 20 * scale;
        float buttonWidth = 448 * scale;
        float buttonHeight = 60 * scale;
        float startY = height * 0.45f + ((buttonHeight + buttonSpacing) * (buttons.length-1))/2;

        for (int i = 0; i < buttons.length; i++) {
            float buttonX = width * 0.75f - buttonWidth/2;
            float buttonY = startY - (buttonHeight + buttonSpacing) * i;

            buttons[i].set(buttonX, buttonY, buttonWidth, buttonHeight);

            // Draw button texture or hover texture
            Texture buttonTex;
            if (i == pressedButton) {
                buttonTex = buttonHoverTextures[i];
                buttonX += 2;
                buttonY -= 2;
            } else if (i == hoverButton) {
                buttonTex = buttonHoverTextures[i];
            } else {
                buttonTex = buttonTextures[i];
            }

            if (buttonTex != null) {
                batch.draw(buttonTex, buttonX, buttonY, buttonWidth, buttonHeight);
            } else {
                // Draw fallback colored rectangle
                Color prevColor = batch.getColor();
                batch.setColor((i == hoverButton) ? Color.LIGHT_GRAY : Color.DARK_GRAY);
                batch.draw(assets.getEntry("white", Texture.class),
                    buttonX, buttonY, buttonWidth, buttonHeight);
                batch.setColor(prevColor);
            }

        }

        batch.end();
    }


    // ADDITIONAL SCREEN METHODS
    /**
     * Called when the Screen should render itself.
     *
     * We defer to the other methods update() and draw(). However, it is VERY
     * important that we only quit AFTER a draw.
     *
     * @param delta Number of seconds since last animation frame
     */
    public void render(float delta) {
        if (active) {
            update(delta);
            draw();
        }
    }

    /**
     * Called when the Screen is resized.
     *
     * This can happen at any point during a non-paused state but will never
     * happen before a call to show().
     *
     * @param width  The new width in pixels
     * @param height The new height in pixels
     */
    public void resize(int width, int height) {
        scale = ((float)height)/ 800.0f; //Assuming 800 height

        this.width  = width;
        this.height = height;
        if (camera == null) {
            camera = new OrthographicCamera(width,height);
        } else {
            camera.setToOrtho( false, width, height  );
        }
    }

    /**
     * Called when the Screen is paused.
     *
     * This is usually when it's not active or visible on screen. An Application
     * is also paused before it is destroyed.
     */
    public void pause() {
        // TODO Auto-generated method stub

    }

    /**
     * Called when the Screen is resumed from a paused state.
     *
     * This is usually when it regains focus.
     */
    public void resume() {
        // TODO Auto-generated method stub

    }

    /**
     * Called when this screen becomes the current screen for a Game.
     */
    public void show() {
        active = true;
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.input.setInputProcessor(this);

        if (music != null && !music.isPlaying()) {
            music.play();
        }
    }

    /**
     * Called when this screen is no longer the current screen for a Game.
     */
    public void hide() {
        active = false;
        Gdx.input.setInputProcessor(null);

        if (music != null && music.isPlaying()) {
            music.pause();
        }
    }

    /**
     * Sets the ScreenListener for this mode
     *
     * The ScreenListener will respond to requests to quit.
     */
    public void setScreenListener(ScreenListener listener) {
        this.listener = listener;
    }

    // PROCESSING PLAYER INPUT
    /**
     * Called when the screen was touched or a mouse button was pressed.
     *
     * This method checks to see if the play button is available and if the click
     * is in the bounds of the play button. If so, it signals the that the button
     * has been pressed and is currently down. Any mouse button is accepted.
     *
     * @param screenX the x-coordinate of the mouse on the screen
     * @param screenY the y-coordinate of the mouse on the screen
     * @param pointer the button or touch finger number
     * @return whether to hand the event to other listeners.
     */
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        screenY = height - screenY;

        for (int i = 0; i < buttons.length; i++) {
            if (buttons[i].contains(screenX, screenY)) {
                pressedButton = i;
                hoverButton = i;
                return true;
            }
        }
        return false;
    }

    /**
     * Called when a finger was lifted or a mouse button was released.
     *
     * This method checks to see if the play button is currently pressed down.
     * If so, it signals the that the player is ready to go.
     *
     * @param screenX the x-coordinate of the mouse on the screen
     * @param screenY the y-coordinate of the mouse on the screen
     * @param pointer the button or touch finger number
     * @return whether to hand the event to other listeners.
     */
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        screenY = height - screenY;
        if (pressedButton >= 0) {
            if (buttons[pressedButton].contains(screenX, screenY)) {
                if (listener != null) {
                    listener.exitScreen(this, pressedButton);
                }
            }
            pressedButton = -1;
        }

        // Fixes the touchDown move off button touchUp bug
        boolean overAnyButton = false;
        for (int i = 0; i < buttons.length; i++) {
            if (buttons[i].contains(screenX, screenY)) {
                hoverButton = i;
                overAnyButton = true;
                break;
            }
        }
        // If not over any button, clear the hover state
        if (!overAnyButton) {
            hoverButton = -1;
        }

        return true;
    }

    /**
     * Called when a key is pressed (UNSUPPORTED)
     *
     * @param keycode the key pressed
     * @return whether to hand the event to other listeners.
     */
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.ESCAPE) {
            if (listener != null) {
                listener.exitScreen(this, EXIT_EXIT);
            }
            return true;
        }
        return false;
    }

    /**
     * Called when a key is typed (UNSUPPORTED)
     *
     *
     * @return whether to hand the event to other listeners.
     */
    public boolean keyTyped(char character) {
        return false;
    }

    /**
     * Called when a key is released (UNSUPPORTED)
     *
     * @param keycode the key released
     * @return whether to hand the event to other listeners.
     */
    public boolean keyUp(int keycode) {
        return false;
    }

    /**
     * Called when the mouse was moved without any buttons being pressed. (UNSUPPORTED)
     *
     * @param screenX the x-coordinate of the mouse on the screen
     * @param screenY the y-coordinate of the mouse on the screen
     * @return whether to hand the event to other listeners.
     */
    public boolean mouseMoved(int screenX, int screenY) {
        screenY = height - screenY;
        int prevHover = hoverButton;
        hoverButton = -1;

        for (int i = 0; i < buttons.length; i++) {
            if (buttons[i].contains(screenX, screenY)) {
                hoverButton = i;
                break;
            }
        }
        return prevHover != hoverButton;
    }

    /**
     * Called when the mouse wheel was scrolled. (UNSUPPORTED)
     *
     * @param dx the amount of horizontal scroll
     * @param dy the amount of vertical scroll
     *
     * @return whether to hand the event to other listeners.
     */
    public boolean scrolled(float dx, float dy) {
        return false;
    }

    /**
     * Called when the touch gesture is cancelled (UNSUPPORTED)
     *
     * Reason may be from OS interruption to touch becoming a large surface such
     * as the user cheek. Relevant on Android and iOS only. The button parameter
     * will be Input.Buttons.LEFT on iOS.
     *
     * @param screenX the x-coordinate of the mouse on the screen
     * @param screenY the y-coordinate of the mouse on the screen
     * @param pointer the button or touch finger number
     * @param button  the button
     * @return whether to hand the event to other listeners.
     */
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    /**
     * Called when the mouse or finger was dragged. (UNSUPPORTED)
     *
     * @param screenX the x-coordinate of the mouse on the screen
     * @param screenY the y-coordinate of the mouse on the screen
     * @param pointer the button or touch finger number
     * @return whether to hand the event to other listeners.
     */
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return false;
    }

}

