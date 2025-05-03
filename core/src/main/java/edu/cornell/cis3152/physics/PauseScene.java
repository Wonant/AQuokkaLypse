package edu.cornell.cis3152.physics;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.ScreenUtils;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.util.ScreenListener;

/**
 * A pause screen that overlays on top of the game.
 *
 * This screen displays buttons while keeping the game visually frozen in the background.
 * The game should not update while this screen is active.
 */
public class PauseScene implements Screen, InputProcessor {
    /** Exit code for resuming the game */
    public static final int EXIT_RESUME = 0;
    /** Exit code for returning to main menu */
    public static final int EXIT_RESTART = 1;
    /** Exit code for options */
    public static final int EXIT_LEVELSELECT = 2;
    /** Exit code for quitting the game */
    public static final int EXIT_SETTINGS = 3;
    public static final int EXIT_QUIT = 4;

    /** Names of pause menu options */
    private final String[] MENU_OPTIONS = {"Resume", "Restart", "LevelSelect", "Settings", "Quit"};

    /** The background transparency overlay */
    private Texture overlay;
    /** The textures for the menu buttons */
    private Texture[] buttonTextures;
    /** The textures for the menu buttons when hovered */
    private Texture[] buttonHoverTextures;

    /** The asset directory */
    private AssetDirectory assets;
    /** The camera for drawing the UI */
    private OrthographicCamera camera;
    /** Reference to sprite batch */
    private SpriteBatch batch;
    /** Listener for screen transitions */
    private ScreenListener listener;

    /** Button rectangles for hit detection */
    private Rectangle[] buttons;
    /** Current button being hovered */
    private int hoverButton = -1;
    /** The button currently being pressed down */
    private int pressedButton = -1;

    /** The width of this scene */
    private int width;
    /** The height of this scene */
    private int height;

    /** Scaling factor for UI elements */
    private float scale;

    /** Whether this screen is active */
    private boolean active;

    /** The screen to draw in the background (the paused game) */
    private Screen gameScreen;

    /**
     * Creates a new pause scene.
     *
     * @param assets The asset directory to use
     * @param batch The sprite batch for drawing
     * @param gameScreen The game screen to pause
     */
    public PauseScene(AssetDirectory assets, SpriteBatch batch, Screen gameScreen) {
        this.assets = assets;
        this.batch = batch;
        this.gameScreen = gameScreen;

        camera = new OrthographicCamera();
        active = false;

        // Initialize buttons
        buttons = new Rectangle[MENU_OPTIONS.length];
        for (int i = 0; i < buttons.length; i++) {
            buttons[i] = new Rectangle();
        }

        // Load button textures
        buttonTextures = new Texture[MENU_OPTIONS.length];
        buttonHoverTextures = new Texture[MENU_OPTIONS.length];

        try {
            // Try to load semi-transparent overlay texture
            overlay = assets.getEntry("overlay", Texture.class);
        } catch (Exception e) {
            // If overlay texture not found, we'll draw a semi-transparent black rectangle
            System.out.println("Overlay texture not found: " + e.getMessage());
        }

        // Load button textures
        for (int i = 0; i < MENU_OPTIONS.length; i++) {
            try {
                buttonTextures[i] = assets.getEntry("pause" + i, Texture.class);
                buttonHoverTextures[i] = assets.getEntry("pausehover" + i, Texture.class);
            } catch (Exception e) {
                // Fall back to default buttons
                try {
                    buttonTextures[i] = assets.getEntry("button", Texture.class);
                    buttonHoverTextures[i] = assets.getEntry("buttonhover", Texture.class);
                } catch (Exception ex) {
                    System.out.println("Button textures not found: " + ex.getMessage());
                }
            }
        }
    }

    /**
     * Called when this screen should release all resources.
     */
    @Override
    public void dispose() {
        // Resources are managed by GDXRoot
    }

    /**
     * Draws the pause menu.
     */
    private void draw() {
        // First draw the game screen in the background (without updating it)
        if (gameScreen != null) {
            gameScreen.render(0); // Pass 0 delta to prevent animation
        }

        // Then draw our pause menu
        batch.begin(camera);

        // Draw semi-transparent overlay
        if (overlay != null) {
            batch.draw(overlay, 0, 0, width, height);
        } else {
            System.out.println("DRAWING OVERLAY FAILED");
            // Draw semi-transparent black rectangle
            Color prevColor = batch.getColor();
            batch.setColor(0, 0, 0, 0.7f); // 70% opacity black
            batch.draw(assets.getEntry("white", Texture.class), 0, 0, width, height);
            batch.setColor(prevColor);
        }

        // Draw "PAUSED" text
        // (This would need a font to be rendered properly)

        // Draw buttons
        float buttonSpacing = 20 * scale;
        float buttonWidth = 300 * scale;
        float buttonHeight = 60 * scale;
        float startY = height * 0.5f + ((buttonHeight + buttonSpacing) * (buttons.length-1))/2;

        for (int i = 0; i < buttons.length; i++) {
            float buttonX = width * 0.5f - buttonWidth/2;
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
                try {
                    batch.draw(assets.getEntry("textures.white", Texture.class),
                        buttonX, buttonY, buttonWidth, buttonHeight);
                } catch (Exception e) {
                    System.out.println("White texture not found: " + e.getMessage());
                }
                batch.setColor(prevColor);
            }
        }

        batch.end();
    }

    @Override
    public void render(float delta) {
        if (active) {
            draw();
        }
    }

    @Override
    public void resize(int width, int height) {
        scale = ((float)height) / 800.0f;

        this.width = width;
        this.height = height;
        if (camera == null) {
            camera = new OrthographicCamera(width, height);
        } else {
            camera.setToOrtho(false, width, height);
        }
    }

    @Override
    public void show() {
        active = true;
        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.input.setInputProcessor(this);
    }

    @Override
    public void hide() {
        active = false;
        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void pause() {
        // No additional pause actions needed
    }

    @Override
    public void resume() {
        // No additional resume actions needed
    }

    /**
     * Sets the ScreenListener for this mode
     *
     * The ScreenListener will respond to requests to transition.
     */
    public void setScreenListener(ScreenListener listener) {
        this.listener = listener;
    }

    // Input handling methods

    @Override
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

    @Override
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

        // Update hover state
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

    @Override
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

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.ESCAPE) {
            // Resume the game when escape is pressed again
            if (listener != null) {
                listener.exitScreen(this, EXIT_RESUME);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return false;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        return false;
    }

    @Override
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
        return false;
    }
}
