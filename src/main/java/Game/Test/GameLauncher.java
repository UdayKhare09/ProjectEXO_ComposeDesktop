package Game.Test;

public class GameLauncher {
    public static boolean isGameRunning = false;

    public static void launch() {
        if (isGameRunning) {
            return; // Prevent duplicate windows
        }
        isGameRunning = true;
        Thread gameThread = new Thread(() -> {
            TopDownGame game = new TopDownGame();
            SimpleLibGDXWindow.launch(game);
        });
        gameThread.setDaemon(true);
        gameThread.start();
    }
}