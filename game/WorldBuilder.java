package game;

import java.util.ArrayList;
import java.util.Random;

/** Builds independent maps in tile coordinates. Reusing a seed reproduces a map. */
public final class WorldBuilder {
    private int width = 60, height = 40;
    private int obstacleCount = 55, lootAttempts = 0;
    private long seed = System.currentTimeMillis();

    public WorldBuilder size(int width, int height) {
        if (width < 8 || height < 8) throw new IllegalArgumentException("Map must be at least 8 by 8 tiles");
        this.width = width;
        this.height = height;
        return this;
    }

    public WorldBuilder seed(long seed) { this.seed = seed; return this; }

    public WorldBuilder obstacles(int count) {
        if (count < 0) throw new IllegalArgumentException("Obstacle count cannot be negative");
        obstacleCount = count;
        return this;
    }

    public WorldBuilder lootAttempts(int count) {
        if (count < 0) throw new IllegalArgumentException("Loot attempts cannot be negative");
        lootAttempts = count;
        return this;
    }

    public World build() {
        Random random = new Random(seed);
        boolean[][] walls = new boolean[height][width];
        ArrayList<Loot> loot = new ArrayList<>();
        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++)
                walls[y][x] = x == 0 || y == 0 || x == width - 1 || y == height - 1;

        for (int i = 0; i < obstacleCount; i++) {
            int x = 2 + random.nextInt(width - 5), y = 2 + random.nextInt(height - 5);
            int w = 1 + random.nextInt(4), h = 1 + random.nextInt(3);
            for (int yy = y; yy < Math.min(height - 1, y + h); yy++)
                for (int xx = x; xx < Math.min(width - 1, x + w); xx++) walls[yy][xx] = true;
        }
        for (int i = 0; i < lootAttempts; i++) {
            int x = 2 + random.nextInt(width - 4), y = 2 + random.nextInt(height - 4);
            int pixelX = x * World.TILE + World.TILE / 2, pixelY = y * World.TILE + World.TILE / 2;
            if (!walls[y][x] && World.hasLootClearance(pixelX, pixelY, loot))
                loot.add(new Loot(pixelX, pixelY, random.nextBoolean() ? LootType.AMMO : LootType.HEALTH));
        }
        return new World(width, height, walls, loot, seed);
    }
}
