package Game.Test;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;
import java.util.List;

public class TopDownGame extends ApplicationAdapter {
    private ShapeRenderer shapeRenderer;
    private Vector2 playerPosition;
    private float playerSpeed = 300f; // Pixels per second
    private float playerSize = 50f;  // Size of the square
    private List<Rectangle> walls;
    private OrthographicCamera camera;
    private float viewportWidth = 800;
    private float viewportHeight = 600;

    @Override
    public void create() {
        shapeRenderer = new ShapeRenderer();
        playerPosition = new Vector2((Gdx.graphics.getWidth() / 2f) - 50, Gdx.graphics.getHeight() / 2f);

        // Initialize camera
        camera = new OrthographicCamera(viewportWidth, viewportHeight);
        camera.position.set(playerPosition.x + playerSize/2, playerPosition.y + playerSize/2, 0);
        camera.update();

        // Initialize walls
        walls = new ArrayList<>();
        walls.add(new Rectangle(100, 100, 200, 50)); // Example wall
        walls.add(new Rectangle(400, 300, 50, 200)); // Example wall
        walls.add(new Rectangle(600, 100, 150, 50)); // Example wall
    }

    @Override
    public void render() {
        // Clear the screen
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Update player position based on input
        handleInput(Gdx.graphics.getDeltaTime());

        // Update camera position to follow player
        camera.position.set(playerPosition.x + playerSize/2, playerPosition.y + playerSize/2, 0);
        camera.update();

        // Set projection matrix to use the camera
        shapeRenderer.setProjectionMatrix(camera.combined);

        // Draw the player and walls
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Draw walls
        shapeRenderer.setColor(1, 0, 0, 1); // Red color for walls
        for (Rectangle wall : walls) {
            shapeRenderer.rect(wall.x, wall.y, wall.width, wall.height);
        }

        // Draw player
        shapeRenderer.setColor(0, 1, 0, 1); // Green color for player
        shapeRenderer.rect(playerPosition.x, playerPosition.y, playerSize, playerSize);

        shapeRenderer.end();
    }

    private void handleInput(float deltaTime) {
        Vector2 newPosition = new Vector2(playerPosition);

        if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.UP)) {
            newPosition.y += playerSpeed * deltaTime;
        }
        if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.DOWN)) {
            newPosition.y -= playerSpeed * deltaTime;
        }
        if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.LEFT)) {
            newPosition.x -= playerSpeed * deltaTime;
        }
        if (Gdx.input.isKeyPressed(com.badlogic.gdx.Input.Keys.RIGHT)) {
            newPosition.x += playerSpeed * deltaTime;
        }

        // Check for collisions with walls
        Rectangle playerRect = new Rectangle(newPosition.x, newPosition.y, playerSize, playerSize);
        boolean collides = false;
        for (Rectangle wall : walls) {
            if (playerRect.overlaps(wall)) {
                collides = true;
                break;
            }
        }

        // Update position only if no collision
        if (!collides) {
            playerPosition.set(newPosition);
        }
    }

    @Override
    public void resize(int width, int height) {
        // Update viewport when window size changes
        float aspectRatio = (float) width / (float) height;
        camera.viewportWidth = viewportHeight * aspectRatio;
        camera.viewportHeight = viewportHeight;
        camera.update();
    }

    @Override
    public void dispose() {
        shapeRenderer.dispose();
        GameLauncher.isGameRunning = false; // Reset the flag to allow restarting
        SimpleLibGDXWindow.resetState(); // Use the new reset method
        System.out.println("Game disposed and resources released.");
    }
}