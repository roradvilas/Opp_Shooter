package game;

import java.util.ArrayList;
import java.util.Random;

/** Builds a map in tile coordinates. Reusing a seed reproduces the same walls. */
public final class WorldGenerator {
    private static final int WIDTH = 60, HEIGHT = 40, OBSTACLES = 55;

    private long seed = System.currentTimeMillis();

    public WorldGenerator seed(long seed) { this.seed = seed; return this; }

    public World generate() {
        Random random = new Random(seed);
        boolean[][] walls = new boolean[HEIGHT][WIDTH];
        for (int y = 0; y < HEIGHT; y++)
            for (int x = 0; x < WIDTH; x++)
                walls[y][x] = x == 0 || y == 0 || x == WIDTH - 1 || y == HEIGHT - 1;

        for (int i = 0; i < OBSTACLES; i++) {
            int x = 2 + random.nextInt(WIDTH - 5), y = 2 + random.nextInt(HEIGHT - 5);
            int w = 1 + random.nextInt(4), h = 1 + random.nextInt(3);
            for (int yy = y; yy < Math.min(HEIGHT - 1, y + h); yy++)
                for (int xx = x; xx < Math.min(WIDTH - 1, x + w); xx++) walls[yy][xx] = true;
        }
        return new World(WIDTH, HEIGHT, walls, new ArrayList<>(), seed);
    }
}
