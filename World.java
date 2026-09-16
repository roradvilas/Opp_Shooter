import java.awt.*;
import java.util.*;

public class World {
    public static final int TILE = 40;
    private final int width = 60;
    private final int height = 40;
    private final boolean[][] walls = new boolean[height][width];
    private final ArrayList<Point> loot = new ArrayList<>();

    public World() { generate(); }

    private void generate() {
        Random random = new Random(7); // same deterministic map on every client/server

        for (int y = 0; y < height; y++)
            for (int x = 0; x < width; x++)
                walls[y][x] = x == 0 || y == 0 || x == width - 1 || y == height - 1;

        for (int i = 0; i < 55; i++) {
            int x = 2 + random.nextInt(width - 5);
            int y = 2 + random.nextInt(height - 5);
            int w = 1 + random.nextInt(4);
            int h = 1 + random.nextInt(3);
            for (int yy = y; yy < Math.min(height - 1, y + h); yy++)
                for (int xx = x; xx < Math.min(width - 1, x + w); xx++)
                    walls[yy][xx] = true;
        }

        for (int i = 0; i < 35; i++) {
            int x = 2 + random.nextInt(width - 4);
            int y = 2 + random.nextInt(height - 4);
            if (!walls[y][x])
                loot.add(new Point(x * TILE + TILE / 2, y * TILE + TILE / 2));
        }
    }

    public int getWidth() { return width * TILE; }
    public int getHeight() { return height * TILE; }
    public ArrayList<Point> getLoot() { return loot; }

    public boolean isWall(int pixelX, int pixelY) {
        int tx = pixelX / TILE, ty = pixelY / TILE;
        if (tx < 0 || ty < 0 || tx >= width || ty >= height) return true;
        return walls[ty][tx];
    }

    public boolean collidesWithWall(double x, double y, int radius) {
        return isWall((int)(x - radius), (int)(y - radius))
            || isWall((int)(x + radius), (int)(y - radius))
            || isWall((int)(x - radius), (int)(y + radius))
            || isWall((int)(x + radius), (int)(y + radius));
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

        g.setColor(new Color(240,190,60));
        for (Point p : loot) {
            int sx = p.x - cameraX, sy = p.y - cameraY;
            g.fillRect(sx - 7, sy - 7, 14, 14);
        }
    }
}
