package game;

import java.awt.*;
import java.util.*;

public class World {
    public static final int TILE = 40;
    public static final int LOOT_EXCLUSION_RADIUS = 2 * TILE;
    private final int width;
    private final int height;
    private final boolean[][] walls;
    private final ArrayList<Loot> loot;
    private final long seed;

    public World() { this(new WorldBuilder().build()); }

    private World(World generated) {
        this(generated.width, generated.height, generated.walls, generated.loot, generated.seed);
    }

    World(int width, int height, boolean[][] walls, ArrayList<Loot> loot, long seed) {
        this.width = width;
        this.height = height;
        this.walls = walls;
        this.loot = loot;
        this.seed = seed;
    }

    public long getSeed() { return seed; }
    public int getWidth() { return width * TILE; }
    public int getHeight() { return height * TILE; }
    public ArrayList<Loot> getLoot() { return loot; }

    /** Places one pickup outside every existing pickup's two-tile radius. */
    public boolean spawnLoot(Random random) {
        synchronized (loot) {
            ArrayList<Point> available = new ArrayList<>();
            for (int y = 1; y < height - 1; y++) {
                for (int x = 1; x < width - 1; x++) {
                    Point position = new Point(x * TILE + TILE / 2, y * TILE + TILE / 2);
                    if (!collidesWithWall(position.x, position.y, 16)
                            && hasLootClearance(position.x, position.y, loot))
                        available.add(position);
                }
            }
            if (available.isEmpty()) return false;
            Point position = available.get(random.nextInt(available.size()));
            LootType[] types = LootType.values();
            LootType type = types[random.nextInt(types.length)];
            loot.add(new Loot(position.x, position.y, type));
            return true;
        }
    }

    public static boolean hasLootClearance(int x, int y, java.util.List<Loot> existingLoot) {
        for (Loot pickup : existingLoot) {
            double dx = (double)x - pickup.x(), dy = (double)y - pickup.y();
            if (dx * dx + dy * dy <= LOOT_EXCLUSION_RADIUS * LOOT_EXCLUSION_RADIUS) return false;
        }
        return true;
    }

    public boolean isWall(int pixelX, int pixelY) {
        int tx = Math.floorDiv(pixelX, TILE), ty = Math.floorDiv(pixelY, TILE);
        if (tx < 0 || ty < 0 || tx >= width || ty >= height) return true;
        return walls[ty][tx];
    }

    public boolean collidesWithWall(double x, double y, int radius) {
        return isWall((int)Math.floor(x - radius), (int)Math.floor(y - radius))
            || isWall((int)Math.floor(x + radius), (int)Math.floor(y - radius))
            || isWall((int)Math.floor(x - radius), (int)Math.floor(y + radius))
            || isWall((int)Math.floor(x + radius), (int)Math.floor(y + radius));
    }

    public void draw(Graphics2D g, int cameraX, int cameraY, int screenWidth, int screenHeight) {
        int startX = Math.max(0, cameraX / TILE - 1);
        int startY = Math.max(0, cameraY / TILE - 1);
        int endX = Math.min(width, (cameraX + screenWidth) / TILE + 2);
        int endY = Math.min(height, (cameraY + screenHeight) / TILE + 2);

        g.setColor(new Color(35,35,40));
        g.fillRect(0,0,screenWidth,screenHeight);

        for (int y = startY; y < endY; y++) {
            for (int x = startX; x < endX; x++) {
                int sx = x * TILE - cameraX, sy = y * TILE - cameraY;
                g.setColor(walls[y][x] ? new Color(70,70,75) : new Color(45,45,48));
                g.fillRect(sx, sy, TILE, TILE);
                g.setColor(new Color(52,52,55));
                g.drawRect(sx, sy, TILE, TILE);
            }
        }

    }
}
