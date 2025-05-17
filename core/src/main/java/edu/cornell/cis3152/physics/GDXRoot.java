/*
 * GDXRoot.java
 *
 * This is the primary class file for running the game. It is the "static main"
 * of LibGDX. In this lab we once again return to using Game (instead of
 * ApplicationAdapter), as scene management is so much easier. Once again, take
 * note of the use of ScreenListener to allow scene switching.
 *
 * Based on the original PhysicsDemo Lab by Don Holden, 2007
 *
 * Author:  Walker M. White
 * Version: 2/8/2025
 */
package edu.cornell.cis3152.physics;

import com.badlogic.gdx.*;
import com.badlogic.gdx.utils.ScreenUtils;
import edu.cornell.cis3152.physics.platform.PlatformScene;
import edu.cornell.gdiac.util.*;
import edu.cornell.gdiac.assets.*;
import edu.cornell.gdiac.graphics.*;



public class GDXRoot extends Game implements ScreenListener {
    /** AssetDirectory to load game assets (textures, sounds, etc.) */
    private AssetDirectory directory;
    /** The SpriteBatch used for drawing the screens */
    private SpriteBatch batch;
    /** The loading scene that loads assets asynchronously */
    private LoadingScene loading;
    /** The main menu scene */
    private MainMenuScene mainMenu;
    /** The pause scene */
    private PauseScene pauseScene;
    /** The settings scene */
    private SettingsScene settingsScene;
    /** The game screen that was active when pause/settings was opened */
    private PlatformScene pausedScreen;
    /** Array of Arena controllers (one per map) */
    private PlatformScene[] controllers;
    /** Index of the current Arena */
    private int current;
    /** Array of map keys for each level */


    private String[] maps = {"level_select", "tutorial1", "level2", "level3", "level4", "level5", "level6", "level7", "level9", "level10"};
    private String[] tiled = {"maps/level_select_map_10.tmx", "maps/tutorial.tmx", "maps/level_2.tmx", "maps/level_3.tmx", "maps/level_4.tmx", "maps/level_5.tmx","maps/level_6.tmx", "maps/level_7.tmx", "maps/level_9.tmx", "maps/level_10.tmx"};


    /** Current map index for switching levels */
    private int currentMapIndex = 0;

    /** Constructor */
    public GDXRoot() { }

    /**
     * Called when the application is first created.
     * Initializes the SpriteBatch and the loading scene.
     */
    public void create() {
        batch = new SpriteBatch();

        // Initialize the AudioManager (this also loads saved settings)
        AudioManager.getInstance();

        loading = new LoadingScene("assets.json", batch, 1);
        loading.setScreenListener(this);
        setScreen(loading);
    }

    /**
     * Called when the application is disposed.
     * Disposes of the current screen, controllers, SpriteBatch, and asset directory.
     */
    public void dispose() {
        setScreen(null);
        if (loading != null) {
            loading.dispose();
            loading = null;
        }
        if (mainMenu != null) {
            mainMenu.dispose();
            mainMenu = null;
        }
        if (pauseScene != null) {
            pauseScene.dispose();
            pauseScene = null;
        }
        if (settingsScene != null) {
            settingsScene.dispose();
            settingsScene = null;
        }
        if (controllers != null) {
            for (int i = 0; i < controllers.length; i++) {
                controllers[i].dispose();
            }
            controllers = null;
        }
        if (batch != null) {
            batch.dispose();
            batch = null;
        }
        if (directory != null) {
            directory.unloadAssets();
            directory.dispose();
            directory = null;
        }
        super.dispose();
    }

    /**
     * Called when the application is resized.
     *
     * @param width  The new width in pixels
     * @param height The new height in pixels
     */
    public void resize(int width, int height) {
        if (loading != null) {
            loading.resize(width, height);
        }
        if (mainMenu != null) {
            mainMenu.resize(width, height);
        }
        if (settingsScene != null) {
            settingsScene.resize(width, height);
        }
        if (controllers != null) {
            for (int i = 0; i < controllers.length; i++) {
                controllers[i].resize(width, height);
            }
        }
    }

    /**
     * Called when a child screen requests an exit.
     * This method switches to the appropriate Arena based on the exit code.
     *
     * @param screen   The screen that is exiting.
     * @param exitCode The exit code indicating the desired action.
     */
    public void exitScreen(Screen screen, int exitCode) {
        if (screen == loading) {
            directory = loading.getAssets();
            loading.dispose();
            loading = null;

            mainMenu = new MainMenuScene(directory, batch);
            mainMenu.setScreenListener(this);

            // Create one Arena for each map key
            controllers = new PlatformScene[maps.length];
            boolean isLevelSelect = false;
            for (int i = 0; i < maps.length; i++) {
                if(i == 2){
                    isLevelSelect = true;
                }
                System.out.println(maps[i]);
                controllers[i] = new PlatformScene(directory, maps[i], tiled[i], isLevelSelect);
                controllers[i].setScreenListener(this);
                controllers[i].setSpriteBatch(batch);
                controllers[i].reset();
                isLevelSelect = false;
            }

            setScreen(mainMenu);
        }
        else if (screen == mainMenu) {
            switch (exitCode) {
                case MainMenuScene.EXIT_PLAY:
                    current = 1;
                    setScreen(controllers[current]);
                    break;
                case MainMenuScene.EXIT_SETTINGS:
                    // Show settings screen
                    if (settingsScene == null) {
                        settingsScene = new SettingsScene(directory, batch, mainMenu);
                        settingsScene.setScreenListener(this);
                    }
                    pausedScreen = null; // No game is being paused
                    setScreen(settingsScene);
                    break;
                case MainMenuScene.EXIT_CREDITS:
                    // TODO: Implement credits screen
                    // For now, just return to main menu
                    setScreen(mainMenu);
                    break;
                case MainMenuScene.EXIT_EXIT:
                    Gdx.app.exit();
                    break;
            }
        }
        else if (screen instanceof PlatformScene) {
            if (exitCode == PlatformScene.EXIT_NEXT) {
                current = (current + 1) % controllers.length;
                setScreen(controllers[current]);
            } else if (exitCode == PlatformScene.EXIT_PREV) {
                // Go back to the previous Arena
                current = (current + controllers.length - 1) % controllers.length;
                setScreen(controllers[current]);
            } else if (exitCode == PlatformScene.EXIT_LEVELSELECT)
            {
                setScreen(controllers[2]);
            }
            else if (exitCode == PlatformScene.FROM_LEVELSELECT){
                int dest = ((PlatformScene) screen).getDoorDestination();
                setScreen(controllers[dest]);
            }
            else if (exitCode == PlatformScene.EXIT_PAUSE) {
                if (screen instanceof PlatformScene) {
                    pausedScreen = (PlatformScene) screen;
                    if (pauseScene == null) {
                        pauseScene = new PauseScene(directory, batch, screen);
                        pauseScene.setScreenListener(this);
                    }

                    setScreen(pauseScene);
                }
            }
        }
        else if (screen == pauseScene) {
            switch (exitCode) {
                case PauseScene.EXIT_RESUME:
                    // Resume the game
                    ((PlatformScene)pausedScreen).setResumingFromPause(true);
                    setScreen(pausedScreen);
                    break;
                case PauseScene.EXIT_RESTART:
                    // Return to main menu
                    ((PlatformScene)pausedScreen).setResumingFromPause(false);
                    setScreen(pausedScreen);
                    break;
                case PauseScene.EXIT_LEVELSELECT:
                    setScreen(controllers[2]);
                    break;
                case PauseScene.EXIT_SETTINGS:
                    // Open settings screen
                    if (settingsScene == null) {
                        settingsScene = new SettingsScene(directory, batch, pauseScene);
                        settingsScene.setScreenListener(this);
                    }
                    setScreen(settingsScene);
                    break;
                case PauseScene.EXIT_QUIT:
                    setScreen(mainMenu);
                    break;
            }
        }
        else if (screen == settingsScene) {
            if (exitCode == SettingsScene.EXIT_BACK) {
                settingsScene.resetScreen();
                if (pausedScreen != null) {
                    pauseScene.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
                    setScreen(pauseScene);
                } else {
                    mainMenu.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
                    setScreen(mainMenu);
                }

                AudioManager.getInstance().loadSettings();
            }
        }
    }

    /**
     * The render method.
     * Delegates rendering to the current screen.
     */
    public void render() {
        super.render();
    }
}
