package edu.cornell.cis3152.physics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.utils.Disposable;

/**
 * Manages all audio for the game.
 *
 * This singleton class allows controlling volume globally and keeps track
 * of all playing sounds and music.
 */
public class AudioManager implements Disposable {
    /** Singleton instance of the AudioManager */
    private static AudioManager instance;

    /** Master volume level (0.0 to 1.0) */
    private float masterVolume = 1.0f;
    /** Music volume level (0.0 to 1.0) */
    private float musicVolume = 1.0f;
    /** Sound effects volume level (0.0 to 1.0) */
    private float sfxVolume = 1.0f;

    /** Currently playing background music */
    private Music currentMusic;

    /**
     * Returns the singleton instance of the AudioManager.
     */
    public static AudioManager getInstance() {
        if (instance == null) {
            instance = new AudioManager();
        }
        return instance;
    }

    /**
     * Private constructor to enforce singleton pattern.
     */
    private AudioManager() {
        // Initialize preferences or load saved volume settings here
        // For now, we'll use default values
    }

    /**
     * Sets the master volume level.
     *
     * @param volume Volume level from 0.0 (silent) to 1.0 (full volume)
     */
    public void setMasterVolume(float volume) {
        masterVolume = volume;
        updateMusicVolume();
    }

    /**
     * Sets the music volume level.
     *
     * @param volume Volume level from 0.0 (silent) to 1.0 (full volume)
     */
    public void setMusicVolume(float volume) {
        musicVolume = volume;
        updateMusicVolume();
    }

    /**
     * Sets the sound effects volume level.
     *
     * @param volume Volume level from 0.0 (silent) to 1.0 (full volume)
     */
    public void setSfxVolume(float volume) {
        sfxVolume = volume;
    }

    /**
     * Gets the master volume level.
     *
     * @return Master volume level from 0.0 to 1.0
     */
    public float getMasterVolume() {
        return masterVolume;
    }

    /**
     * Gets the music volume level.
     *
     * @return Music volume level from 0.0 to 1.0
     */
    public float getMusicVolume() {
        return musicVolume;
    }

    /**
     * Gets the sound effects volume level.
     *
     * @return Sound effects volume level from 0.0 to 1.0
     */
    public float getSfxVolume() {
        return sfxVolume;
    }

    /**
     * Updates the volume of currently playing music based on master and music volume settings.
     */
    private void updateMusicVolume() {
        if (currentMusic != null) {
            currentMusic.setVolume(masterVolume * musicVolume);
        }
    }

    /**
     * Plays background music.
     *
     * @param music The Music instance to play
     * @param looping Whether the music should loop
     */
    public void playMusic(Music music, boolean looping) {
        // Stop any currently playing music
        if (currentMusic != null) {
            currentMusic.stop();
        }

        currentMusic = music;
        if (currentMusic != null) {
            currentMusic.setLooping(looping);
            currentMusic.setVolume(masterVolume * musicVolume);
            currentMusic.play();
        }
    }

    /**
     * Plays a sound effect.
     *
     * @param sound The Sound instance to play
     * @return The sound ID for controlling playback
     */
    public long playSound(Sound sound) {
        return sound.play(masterVolume * sfxVolume);
    }

    /**
     * Plays a sound effect with volume and pitch control.
     *
     * @param sound The Sound instance to play
     * @param volume Volume in the range [0,1]
     * @param pitch Pitch multiplier, 1 = normal pitch
     * @param pan Panning in the range -1 (full left) to 1 (full right)
     * @return The sound ID for controlling playback
     */
    public long playSound(Sound sound, float volume, float pitch, float pan) {
        return sound.play(masterVolume * sfxVolume * volume, pitch, pan);
    }

    /**
     * Stops all playing sound effects.
     */
    public void stopAllSounds() {
        // In a real implementation, you'd want to keep track of all playing sounds
        // and stop them individually
    }

    /**
     * Pauses the current background music.
     */
    public void pauseMusic() {
        if (currentMusic != null) {
            currentMusic.pause();
        }
    }

    /**
     * Resumes the current background music if paused.
     */
    public void resumeMusic() {
        if (currentMusic != null) {
            currentMusic.play();
        }
    }

    /**
     * Stops the current background music.
     */
    public void stopMusic() {
        if (currentMusic != null) {
            currentMusic.stop();
            currentMusic = null;
        }
    }

    /**
     * Disposes all audio resources.
     */
    @Override
    public void dispose() {
        stopMusic();
        stopAllSounds();
        instance = null;
    }

    /**
     * Saves the current volume settings.
     * In a real implementation, this would save to preferences.
     */
    public void saveSettings() {
        // In a real implementation, you'd save to preferences here
        // For example:
        // Preferences prefs = Gdx.app.getPreferences("game_settings");
        // prefs.putFloat("master_volume", masterVolume);
        // prefs.putFloat("music_volume", musicVolume);
        // prefs.putFloat("sfx_volume", sfxVolume);
        // prefs.flush();
    }

    /**
     * Loads saved volume settings.
     * In a real implementation, this would load from preferences.
     */
    public void loadSettings() {
        // In a real implementation, you'd load from preferences here
        // For example:
        // Preferences prefs = Gdx.app.getPreferences("game_settings");
        // masterVolume = prefs.getFloat("master_volume", 1.0f);
        // musicVolume = prefs.getFloat("music_volume", 1.0f);
        // sfxVolume = prefs.getFloat("sfx_volume", 1.0f);
    }
}
