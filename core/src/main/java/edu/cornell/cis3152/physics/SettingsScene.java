package edu.cornell.cis3152.physics;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.audio.Sound;
import edu.cornell.gdiac.assets.AssetDirectory;
import edu.cornell.gdiac.audio.SoundEffect;
import edu.cornell.gdiac.graphics.SpriteBatch;
import edu.cornell.gdiac.util.ScreenListener;

/**
 * A settings screen that allows adjusting game options like volume.
 *
 * This screen displays sliders for volume control and a back button.
 */
public class SettingsScene implements Screen, InputProcessor {
    /** Exit code for returning to previous screen */
    public static final int EXIT_BACK = 0;

    /** The background transparency overlay */
    private Texture overlay;
    /** The texture for the slider background */
    private Texture sliderBg;
    /** The texture for the slider knob */
    private Texture sliderKnob;
    /** The texture for the back button */
    private Texture backButton;
    /** The texture for the back button when hovered */
    private Texture backButtonHover;

    /** The asset directory */
    private AssetDirectory assets;
    /** The camera for drawing the UI */
    private OrthographicCamera camera;
    /** Reference to sprite batch */
    private SpriteBatch batch;
    /** Listener for screen transitions */
    private ScreenListener listener;

    /** Back button rectangle for hit detection */
    private Rectangle backButtonRect;
    /** Master volume slider rectangle */
    private Rectangle masterSliderRect;
    /** Music volume slider rectangle */
    private Rectangle musicSliderRect;
    /** SFX volume slider rectangle */
    private Rectangle sfxSliderRect;
    /** Currently active slider (-1 for none) */
    private int activeSlider = -1;
    /** Whether the back button is being hovered */
    private boolean hoverBack = false;
    /** Whether the back button is being pressed */
    private boolean pressedBack = false;
    /** Test sound button rectangle */
    private Rectangle testSoundRect;
    /** Whether the test sound button is being hovered */
    private boolean hoverTestSound = false;
    /** Whether the test sound button is being pressed */
    private boolean pressedTestSound = false;
    /** Sound effect for testing volume */
    private Sound testSound;

    /** Reference to the AudioManager */
    private AudioManager audioManager;

    /** Width of the screen */
    private int width;
    /** Height of the screen */
    private int height;
    /** Scaling factor for UI elements */
    private float scale;
    /** Whether this screen is active */
    private boolean active;

    /** The screen to draw in the background */
    private Screen backgroundScreen;

    /**
     * Creates a new settings scene.
     *
     * @param assets The asset directory to use
     * @param batch The sprite batch for drawing
     * @param backgroundScreen The screen to draw in the background
     */
    public SettingsScene(AssetDirectory assets, SpriteBatch batch, Screen backgroundScreen) {
        this.assets = assets;
        this.batch = batch;
        this.backgroundScreen = backgroundScreen;
        this.audioManager = AudioManager.getInstance();

        camera = new OrthographicCamera();
        active = false;

        backButtonRect = new Rectangle();
        masterSliderRect = new Rectangle();
        musicSliderRect = new Rectangle();
        sfxSliderRect = new Rectangle();
        testSoundRect = new Rectangle();

        try {
            overlay = assets.getEntry("black_bg", Texture.class);
        } catch (Exception e) {
            System.out.println("Overlay texture not found: " + e.getMessage());
        }

        try {
            sliderBg = assets.getEntry("sliderBg", Texture.class);
            sliderKnob = assets.getEntry("sliderKnob", Texture.class);
        } catch (Exception e) {
            System.out.println("Slider textures not found: " + e.getMessage());
        }

        try {
            backButton = assets.getEntry("backButton", Texture.class);
            backButtonHover = assets.getEntry("backButton", Texture.class);
        } catch (Exception e) {
            try {
                backButton = assets.getEntry("button", Texture.class);
                backButtonHover = assets.getEntry("buttonhover", Texture.class);
            } catch (Exception ex) {
                System.out.println("Button textures not found: " + ex.getMessage());
            }
        }

        try {
            testSound = assets.getEntry("platform-pew", SoundEffect.class);
        } catch (Exception e) {
            System.out.println("Test sound not found: " + e.getMessage());
        }
    }

    /**
     * Called when this screen should release all resources.
     */
    @Override
    public void dispose() {
    }

    public void resetScreen() {
        ScreenUtils.clear(0, 0, 0, 0);

        if (batch != null) {
            batch.setColor(1, 1, 1, 1);
        }

        if (camera != null) {
            camera.update();
        }
    }

    /**
     * Gets the value of a slider based on its position.
     *
     * @param slider The slider rectangle
     * @param x The x position to check
     * @return A value between 0.0 and 1.0
     */
    private float getSliderValue(Rectangle slider, float x) {
        float value = (x - slider.x) / slider.width;
        return Math.max(0, Math.min(1, value));
    }

    /**
     * Gets the position of a slider knob based on its value.
     *
     * @param slider The slider rectangle
     * @param value The value between 0.0 and 1.0
     * @return The x position of the knob
     */
    private float getSliderKnobX(Rectangle slider, float value) {
        return slider.x + (slider.width * value);
    }

    /**
     * Updates the appropriate volume based on the active slider.
     *
     * @param x The x position to update to
     */
    private void updateSliderValue(float x) {
        if (activeSlider == 0) {
            audioManager.setMasterVolume(getSliderValue(masterSliderRect, x));
        } else if (activeSlider == 1) {
            audioManager.setMusicVolume(getSliderValue(musicSliderRect, x));
        } else if (activeSlider == 2) {
            audioManager.setSfxVolume(getSliderValue(sfxSliderRect, x));
        }
    }

    /**
     * Draws the settings menu.
     */
    private void draw() {
        if (backgroundScreen != null) {
            backgroundScreen.render(0);
        }

        batch.begin(camera);

        if (overlay != null) {
            batch.setColor(1, 1, 1, 1f);
            batch.draw(overlay, 0, 0, width, height);
        } else {
            batch.setColor(0, 0, 0, 1f);
            try {
                batch.draw(assets.getEntry("white", Texture.class), 0, 0, width, height);
            } catch (Exception e) {
                System.out.println("White texture not found: " + e.getMessage());
            }
        }

        batch.setColor(1, 1, 1, 1f);

        float sliderWidth = 300 * scale;
        float sliderHeight = 30 * scale;
        float knobSize = 40 * scale;
        float labelWidth = 150 * scale;
        float spacing = 60 * scale;
        float startY = height * 0.6f;

        float testButtonWidth = 120 * scale;
        float testButtonSpacing = 20 * scale;
        float totalElementWidth = labelWidth + sliderWidth + testButtonWidth + testButtonSpacing;

        float startX = width * 0.5f - totalElementWidth * 0.5f;
        float sliderStartX = startX + labelWidth;

        masterSliderRect.set(sliderStartX, startY, sliderWidth, sliderHeight);
        musicSliderRect.set(sliderStartX, startY - spacing, sliderWidth, sliderHeight);
        sfxSliderRect.set(sliderStartX, startY - spacing * 2, sliderWidth, sliderHeight);

        // Draw master volume slider
        drawSlider(sliderStartX, startY, sliderWidth, sliderHeight, knobSize, audioManager.getMasterVolume(), "Master Volume", startX);

        // Draw music volume slider
        drawSlider(sliderStartX, startY - spacing, sliderWidth, sliderHeight, knobSize, audioManager.getMusicVolume(), "Music Volume", startX);

        // Draw SFX volume slider
        drawSlider(sliderStartX, startY - spacing * 2, sliderWidth, sliderHeight, knobSize, audioManager.getSfxVolume(), "SFX Volume", startX);

        float testButtonHeight = 40 * scale;
        float testButtonX = sliderStartX + sliderWidth + testButtonSpacing;
        float testButtonY = startY - spacing * 2 + (sliderHeight - testButtonHeight) / 2;

        testSoundRect.set(testButtonX, testButtonY, testButtonWidth, testButtonHeight);

        Color prevColor = batch.getColor();
        batch.setColor(pressedTestSound ? new Color(0.9f, 0.9f, 0.9f, 1) : (hoverTestSound ? new Color(0.95f, 0.95f, 0.95f, 1) : Color.WHITE));
        try {
            batch.draw(assets.getEntry("testButton", Texture.class),
                testButtonX, testButtonY, testButtonWidth, testButtonHeight);
        } catch (Exception e) {
            try {
                batch.draw(assets.getEntry("white", Texture.class),
                    testButtonX, testButtonY, testButtonWidth, testButtonHeight);
            } catch (Exception ex) {
                System.out.println("White texture not found: " + ex.getMessage());
            }
        }
        batch.setColor(prevColor);

        float buttonWidth = 200 * scale;
        float buttonHeight = 60 * scale;
        float buttonX = width * 0.5f - buttonWidth * 0.5f;
        float buttonY = startY - spacing * 4;

        backButtonRect.set(buttonX, buttonY, buttonWidth, buttonHeight);

        Texture buttonTex;
        if (pressedBack) {
            buttonTex = backButtonHover;
            buttonX += 2;
            buttonY -= 2;
        } else if (hoverBack) {
            buttonTex = backButtonHover;
        } else {
            buttonTex = backButton;
        }

        if (buttonTex != null) {
            Color prevButtonColor = batch.getColor();
            batch.setColor(pressedBack ? new Color(0.9f, 0.9f, 0.9f, 1) : (hoverBack ? new Color(0.95f, 0.95f, 0.95f, 1) : Color.WHITE));
            batch.draw(buttonTex, buttonX, buttonY, buttonWidth, buttonHeight);
            batch.setColor(prevButtonColor);
        } else {
            Color prevColor2 = batch.getColor();
            batch.setColor(pressedBack ? new Color(0.9f, 0.9f, 0.9f, 1) : (hoverBack ? new Color(0.95f, 0.95f, 0.95f, 1) : Color.WHITE));
            try {
                batch.draw(assets.getEntry("white", Texture.class),
                    buttonX, buttonY, buttonWidth, buttonHeight);
            } catch (Exception e) {
                System.out.println("White texture not found: " + e.getMessage());
            }
            batch.setColor(prevColor2);
        }

        batch.end();
    }

    /**
     * Helper method to draw a slider with label.
     */
    private void drawSlider(float x, float y, float width, float height, float knobSize, float value, String label, float labelX) {

        if (sliderBg != null) {
            batch.draw(sliderBg, x, y, width, height);
        } else {
            Color prevColor = batch.getColor();
            batch.setColor(Color.GRAY);
            try {
                batch.draw(assets.getEntry("white", Texture.class), x, y, width, height);
            } catch (Exception e) {
                System.out.println("White texture not found: " + e.getMessage());
            }
            batch.setColor(prevColor);
        }

        float knobX = x + (width * value) - (knobSize * 0.5f);
        float knobY = y + (height * 0.5f) - (knobSize * 0.5f);

        if (sliderKnob != null) {
            batch.draw(sliderKnob, knobX, knobY, knobSize, knobSize);
        } else {
            Color prevColor = batch.getColor();
            batch.setColor(Color.WHITE);
            try {
                batch.draw(assets.getEntry("white", Texture.class), knobX, knobY, knobSize, knobSize);
            } catch (Exception e) {
                System.out.println("White texture not found: " + e.getMessage());
            }
            batch.setColor(prevColor);
        }

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

        audioManager.saveSettings();
        ScreenUtils.clear(0, 0, 0, 0);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
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

        if (backButtonRect.contains(screenX, screenY)) {
            pressedBack = true;
            hoverBack = true;
            return true;
        }

        if (testSoundRect.contains(screenX, screenY)) {
            pressedTestSound = true;
            hoverTestSound = true;
            if (testSound != null) {
                audioManager.playSound(testSound);
            }
            return true;
        }

        if (masterSliderRect.contains(screenX, screenY)) {
            activeSlider = 0;
            updateSliderValue(screenX);
            return true;
        } else if (musicSliderRect.contains(screenX, screenY)) {
            activeSlider = 1;
            updateSliderValue(screenX);
            return true;
        } else if (sfxSliderRect.contains(screenX, screenY)) {
            activeSlider = 2;
            updateSliderValue(screenX);
            return true;
        }

        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        screenY = height - screenY;

        if (pressedBack && backButtonRect.contains(screenX, screenY)) {
            if (listener != null) {
                listener.exitScreen(this, EXIT_BACK);
            }
        }

        pressedBack = false;
        pressedTestSound = false;
        activeSlider = -1;

        hoverBack = backButtonRect.contains(screenX, screenY);
        hoverTestSound = testSoundRect.contains(screenX, screenY);

        return true;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        screenY = height - screenY;

        if (activeSlider >= 0) {
            updateSliderValue(screenX);
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        screenY = height - screenY;

        boolean wasHoveringBack = hoverBack;
        boolean wasHoveringTest = hoverTestSound;

        hoverBack = backButtonRect.contains(screenX, screenY);
        hoverTestSound = testSoundRect.contains(screenX, screenY);

        return wasHoveringBack != hoverBack || wasHoveringTest != hoverTestSound;
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.ESCAPE) {
            if (listener != null) {
                listener.exitScreen(this, EXIT_BACK);
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
    public boolean scrolled(float amountX, float amountY) {
        return false;
    }

    @Override
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
        return false;
    }
}
