package Game.Test;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

public class SimpleLibGDXWindow {
    private static boolean isLibGDXRunning = false;
    public static Lwjgl3Application application;

    public static synchronized void launch(ApplicationAdapter game) {
        if (isLibGDXRunning) {
            return; // Prevent duplicate windows
        }

        isLibGDXRunning = true;
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Top-Down Game");
        config.setWindowedMode(800, 600);

        try {
            application = new Lwjgl3Application(game, config);
        } finally {
            // This will execute when the application exits
            isLibGDXRunning = false;
        }
    }

    // Add a method to reset the state
    public static synchronized void resetState() {
        isLibGDXRunning = false;
        application = null;
    }
}