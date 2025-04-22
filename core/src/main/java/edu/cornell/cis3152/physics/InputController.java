/*
 * InputController.java
 *
 * This class buffers in input from the devices and converts it into its
 * semantic meaning. If your game had an option that allows the player to
 * remap the control keys, you would store this information in this class.
 * That way, the main GameEngine does not have to keep track of the current
 * key mapping.
 *
 * Based on the original PhysicsDemo Lab by Don Holden, 2007
 *
 * Author:  Walker M. White
 * Version: 2/8/2025
 */
package edu.cornell.cis3152.physics;

import com.badlogic.gdx.*;
import com.badlogic.gdx.math.*;

import com.badlogic.gdx.utils.Array;
import edu.cornell.gdiac.util.*;

/**
 * Class for reading player input.
 *
 * This supports both a keyboard and X-Box controller. In previous solutions,
 * we only detected the X-Box controller on start-up. This class allows us to
 * hot-swap in a controller on the fly.
 */
public class InputController {
    // Sensitivity for moving crosshair with gameplay
    private static final float GP_ACCELERATE = 1.0f;
    private static final float GP_MAX_SPEED  = 10.0f;
    private static final float GP_THRESHOLD  = 0.01f;

    /** The singleton instance of the input controller */
    private static InputController theController = null;

    /**
     * Returns the singleton instance of the input controller
     *
     * @return the singleton instance of the input controller
     */
    public static InputController getInstance() {
        if (theController == null) {
            theController = new InputController();
        }
        return theController;
    }

    // Fields to manage buttons
    /** Whether the reset button was pressed. */
    private boolean resetPressed;
    private boolean resetPrevious;
    /** Whether the button to advanced worlds was pressed. */
    private boolean nextPressed;
    private boolean nextPrevious;
    /** Whether the button to step back worlds was pressed. */
    private boolean prevPressed;
    private boolean prevPrevious;
    /** Whether the primary action button was pressed. */
    private boolean primePressed;
    private boolean primePrevious;
    /** Whether the secondary action button was pressed. */
    private boolean secondPressed;
    private boolean secondPrevious;



    /** Whether the debug toggle was pressed. */
    private boolean debugPressed;
    private boolean debugPrevious;
    /** Whether the exit button was pressed. */
    private boolean exitPressed;
    private boolean exitPrevious;



    /** Whether the stun action button was pressed. */
    private boolean shiftPressed;
    /** Whether the teleport button was pressed */

    private boolean m1Pressed;
    private boolean teleportPrevious;

    /** Whether interact stun button was pressed */
    private boolean interactPressed;


    /** How much did we move horizontally? */
    private float horizontal;
    /** How much did we move vertically? */
    private float vertical;
    /** The crosshair position (for raddoll) */
    private Vector2 crosshair;
    /** The crosshair cache (for using as a return value) */
    private Vector2 crosscache;
    /** For the gamepad crosshair control */
    private float momentum;

    /** An X-Box controller (if it is connected) */
    XBoxController xbox;

    /** Training Mode(arena) support */
    private boolean arenaMode = false;
    private boolean spawnDwellerPressed = false;
    private boolean spawnDwellerPrevious = false;
    private boolean spawnCritterPressed = false;
    private boolean spawnCritterPrevious = false;
    private boolean spawnGuardPressed = false;
    private boolean spawnGuardPrevious = false;
    private boolean invulPressed = false;
    private boolean invulPrev = false;

    public boolean isArenaMode() {
        return arenaMode;
    }

    public void setArenaMode(boolean arenaMode) {
        this.arenaMode = arenaMode;
    }

    public boolean didPressInvulnerability() {
        return invulPressed;
    }

    public boolean isSpawningDweller() {
        return arenaMode && spawnDwellerPressed && !spawnDwellerPrevious;
    }

    public boolean isSpawningCritter() {
        return arenaMode && spawnCritterPressed && !spawnCritterPrevious;
    }

    public boolean isSpawningGuard() {
        return arenaMode && spawnGuardPressed && !spawnGuardPrevious;
    }

    public boolean isInteractDown() {
        return interactPressed;
    }

    /**
     * Returns the amount of sideways movement.
     *
     * -1 = left, 1 = right, 0 = still
     *
     * @return the amount of sideways movement.
     */
    public float getHorizontal() {
        return horizontal;
    }

    /**
     * Returns the amount of vertical movement.
     *
     * -1 = down, 1 = up, 0 = still
     *
     * @return the amount of vertical movement.
     */
    public float getVertical() {
        return vertical;
    }

    /**
     * Returns the current position of the crosshairs on the screen.
     *
     * This value does not return the actual reference to the crosshairs
     * position. That way this method can be called multiple times without any
     * fear that the position has been corrupted. However, it does return the
     * same object each time. So if you modify the object, the object will be
     * reset in a subsequent call to this getter.
     *
     * @return the current position of the crosshairs on the screen.
     */
    public Vector2 getCrossHair() {
        return crosscache.set(crosshair);
    }

    public Vector2 getMouse() {
        return new Vector2(Gdx.input.getX(), Gdx.input.getY());
    }

    /**
     * Returns true if the primary action button was pressed.
     *
     * This is a one-press button. It only returns true at the moment it was
     * pressed, and returns false at any frame afterwards.
     *
     * @return true if the primary action button was pressed.
     */
    public boolean didPrimary() {
        return primePressed && !primePrevious;
    }

    /**
     * Returns true if the secondary action button was pressed.
     *
     * This is a one-press button. It only returns true at the moment it was
     * pressed, and returns false at any frame afterwards.
     *
     * @return true if the secondary action button was pressed.
     */
    public boolean didSecondary() {
        return secondPressed && !secondPrevious;
    }


    /**
     * Returns true if the reset button was pressed.
     *
     * @return true if the reset button was pressed.
     */
    public boolean didReset() {
        return resetPressed && !resetPrevious;
    }

    /**
     * Returns true if the player wants to go to the next level.
     *
     * @return true if the player wants to go to the next level.
     */
    public boolean didAdvance() {
        return nextPressed && !nextPrevious;
    }

    /**
     * Returns true if the player wants to go to the previous level.
     *
     * @return true if the player wants to go to the previous level.
     */
    public boolean didRetreat() {
        return prevPressed && !prevPrevious;
    }

    /**
     * Returns true if the player wants to go toggle the debug mode.
     *
     * @return true if the player wants to go toggle the debug mode.
     */
    public boolean didDebug() {
        return debugPressed && !debugPrevious;
    }

    /**
     * Returns true if the exit button was pressed.
     *
     * @return true if the exit button was pressed.
     */
    public boolean didExit() {
        return exitPressed && !exitPrevious;
    }
    /**
     * Returns true if the stun modifier button was pressed
     *
     * @return true if the stun modifier button was pressed
     */
    public boolean inStunMode() {
        return shiftPressed;
    }
    /**
     * Returns true if the create teleport button was pressed
     *
     * @return true if the create teleport button was pressed
     */
    public boolean didM1() {
        return m1Pressed && !teleportPrevious;

    }

    /**
     * Returns true if the take teleport button was pressed
     *
     * @return true if the take teleport button was pressed
     */
    public boolean didTakeTeleport() { return m1Pressed && !teleportPrevious;}


    /**
     * Creates a new input controller
     *
     * The input controller attempts to connect to the X-Box controller at
     * device 0, if it exists. Otherwise, it falls back to the keyboard
     * control.
     */
    public InputController() {
        // If we have a game-pad for id, then use it.
        Array<XBoxController> controllers = Controllers.get().getXBoxControllers();
        if (controllers.size > 0) {
            xbox = controllers.get( 0 );
        } else {
            xbox = null;
        }
        crosshair = new Vector2();
        crosscache = new Vector2();
    }

    /**
     * Syncs the keyboard to the current animation frame.
     *
     * The method provides both the input bounds and the drawing scale. It needs
     * the drawing scale to convert screen coordinates to world coordinates.
     * The bounds are for the crosshair. They cannot go outside of this zone.
     *
     * @param bounds The input bounds for the crosshair.
     * @param scale  The drawing scale
     */
    public void sync(Rectangle bounds, Vector2 scale) {
        // Copy state from last animation frame
        // Helps us ignore buttons that are held down
        primePrevious  = primePressed;
        secondPrevious = secondPressed;
        resetPrevious  = resetPressed;
        debugPrevious  = debugPressed;
        exitPrevious = exitPressed;
        nextPrevious = nextPressed;
        prevPrevious = prevPressed;

        teleportPrevious = m1Pressed;

        if (arenaMode) {
            spawnDwellerPrevious = spawnDwellerPressed;
            spawnGuardPrevious = spawnGuardPressed;
            spawnCritterPrevious = spawnCritterPressed;
            invulPressed = invulPrev;
        }


        // Check to see if a GamePad is connected
        if (xbox != null && xbox.isConnected()) {
            readGamepad(bounds, scale);
            readKeyboard(bounds, scale, true); // Read as a back-up
        } else {
            readKeyboard(bounds, scale, false);
        }
    }

    /**
     * Reads input from an X-Box controller connected to this computer.
     *
     * The method provides both the input bounds and the drawing scale. It needs
     * the drawing scale to convert screen coordinates to world coordinates. The
     * bounds are for the crosshair. They cannot go outside of this zone.
     *
     * @param bounds The input bounds for the crosshair.
     * @param scale  The drawing scale
     */
    private void readGamepad(Rectangle bounds, Vector2 scale) {
        resetPressed = xbox.getStart();
        exitPressed  = xbox.getBack();
        nextPressed  = xbox.getRBumper();
        prevPressed  = xbox.getLBumper();
        primePressed = xbox.getA();
        debugPressed  = xbox.getY();

        // Increase animation frame, but only if trying to move
        horizontal = xbox.getLeftX();
        vertical   = xbox.getLeftY();
        secondPressed = xbox.getRightTrigger() > 0.6f;

        // Move the crosshairs with the right stick.
        shiftPressed = xbox.getA();
        crosscache.set(xbox.getLeftX(), xbox.getLeftY());
        if (crosscache.len2() > GP_THRESHOLD) {
            momentum += GP_ACCELERATE;
            momentum = Math.min(momentum, GP_MAX_SPEED);
            crosscache.scl(momentum);
            crosscache.scl(1/scale.x,1/scale.y);
            crosshair.add(crosscache);
        } else {
            momentum = 0;
        }
        clampPosition(bounds);
        // need gamepad support here for arena/training
    }

    /**
     * Reads input from the keyboard.
     *
     * This controller reads from the keyboard regardless of whether or not an
     * X-Box controller is connected. However, if a controller is connected,
     * this method gives priority to the X-Box controller.
     *
     * @param secondary true if the keyboard should give priority to a gamepad
     */
    private void readKeyboard(Rectangle bounds, Vector2 scale, boolean secondary) {
        // Give priority to gamepad results
        resetPressed = (secondary && resetPressed) || (Gdx.input.isKeyPressed(Input.Keys.R));
        debugPressed = (secondary && debugPressed) || (Gdx.input.isKeyPressed(Input.Keys.B));
        primePressed = (secondary && primePressed) || (Gdx.input.isKeyPressed(Input.Keys.W));
        secondPressed = (secondary && secondPressed) || (Gdx.input.isKeyPressed(Input.Keys.SPACE));
        prevPressed = (secondary && prevPressed) || (Gdx.input.isKeyPressed(Input.Keys.P));
        nextPressed = (secondary && nextPressed) || (Gdx.input.isKeyPressed(Input.Keys.N));
        exitPressed  = (secondary && exitPressed) || (Gdx.input.isKeyPressed(Input.Keys.ESCAPE));
        //takeTeleportPressed = (secondary && takeTeleportPressed) || (Gdx.input.isKeyPressed(Input.Keys.T));

        //teleportPressed = (secondary && teleportPressed) || (Gdx.input.isKeyPressed(Input.Buttons.LEFT));
        //Mouse
        //teleportPressed = teleportPressed || Gdx.input.isButtonPressed(Input.Buttons.RIGHT);


        // Directional controls
        horizontal = (secondary ? horizontal : 0.0f);
        if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            horizontal += 1.0f;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            horizontal -= 1.0f;
        }

        vertical = (secondary ? vertical : 0.0f);
        if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            vertical += 1.0f;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            vertical -= 1.0f;
        }

        shiftPressed = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT);

        interactPressed = Gdx.input.isKeyPressed(Input.Keys.E);

        // Mouse results
        m1Pressed = Gdx.input.isButtonPressed(Input.Buttons.LEFT);



        crosshair.set(Gdx.input.getX(), Gdx.input.getY());
        crosshair.scl(1/scale.x,-1/scale.y);
        crosshair.y += bounds.height;
        clampPosition(bounds);

        if (arenaMode) {
            spawnCritterPressed = Gdx.input.isKeyPressed(Input.Keys.NUM_1);
            spawnGuardPressed = Gdx.input.isKeyPressed(Input.Keys.NUM_2);
            spawnDwellerPressed = Gdx.input.isKeyPressed(Input.Keys.NUM_3);
            invulPressed = Gdx.input.isKeyPressed(Input.Keys.I);
        }
    }

    /**
     * Clamps the cursor position so that it does not go outside the window
     *
     * While this is not usually a problem with mouse control, this is critical
     * for the gamepad controls.
     */
    private void clampPosition(Rectangle bounds) {
        crosshair.x = Math.max(bounds.x, Math.min(bounds.x+bounds.width, crosshair.x));
        crosshair.y = Math.max(bounds.y, Math.min(bounds.y+bounds.height, crosshair.y));
    }
}
