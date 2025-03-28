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
import edu.cornell.cis3152.physics.platform.PlatformScene;
import edu.cornell.gdiac.util.*;
import edu.cornell.gdiac.assets.*;
import edu.cornell.gdiac.graphics.*;

import edu.cornell.cis3152.physics.platform.Arena;

public class GDXRoot extends Game implements ScreenListener {
    /** AssetDirectory to load game assets (textures, sounds, etc.) */
    private AssetDirectory directory;
    /** The SpriteBatch used for drawing the screens */
    private SpriteBatch batch;
    /** The loading scene that loads assets asynchronously */
    private LoadingScene loading;
    /** Array of Arena controllers (one per map) */
    private PlatformScene[] controllers;
    /** Index of the current Arena */
    private int current;
    /** Array of map keys for each level */
    private String[] maps = {"platform-constants", "platform-constants1"};
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

            // Create one Arena for each map key
            controllers = new PlatformScene[maps.length];
            for (int i = 0; i < maps.length; i++) {
                controllers[i] = new PlatformScene(directory, maps[i]);
                controllers[i].setScreenListener(this);
                controllers[i].setSpriteBatch(batch);
                controllers[i].reset();
            }
            current = 0;
            setScreen(controllers[current]);
        }
        else if (exitCode == PlatformScene.EXIT_NEXT) {
            current = (current + 1) % controllers.length;
            setScreen(controllers[current]);
        }
        else if (exitCode == PlatformScene.EXIT_PREV) {
            // Go back to the previous Arena
            current = (current + controllers.length - 1) % controllers.length;
            setScreen(controllers[current]);
        }
        else if (exitCode == Arena.EXIT_ARENA) {
            // Create a new Arena with a fixed map key ("arena")
            Arena arena = new Arena(directory, "arena");
            arena.setScreenListener(this);
            arena.setSpriteBatch(batch);
            setScreen(arena);
        }
        else if (exitCode == PlatformScene.EXIT_QUIT) {
            // Quit the application
            Gdx.app.exit();
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
