package game;

import bridge.Player;
import bridge.PlayerType;
import facade.GameFacade;
import facade.PlayerInput;
import strategy.MovementType;
import strategy.WeaponType;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Server-side bots. They play through the same {@link GameFacade} calls a network client uses
 * (spawn, applyInput) and always walk toward the closest human player, routing around walls.
 */
public final class BotController {
    /** Bots stop this close to their target instead of jittering on top of it. */
    private static final double STOP_DISTANCE = 60;
    /**
     * Ignore offsets this small on an axis so the bot does not zigzag. It must stay under the 4 px of
     * slack a player has in a one-tile corridor, or a bot a few pixels off-centre jams on a corner.
     */
    private static final double DEAD_ZONE = 2;
    private static final int[][] STEPS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    private final GameFacade game;
    private final List<Integer> botIds = new ArrayList<>();
    /** The map is rebuilt from the shared seed, exactly like a client does, to find routes. */
    private final boolean[][] open;
    private final int cols, rows;

    public BotController(GameFacade game, int count) {
        this.game = game;
        for (int i = 1; i <= count; i++) botIds.add(-i);

        World world = new WorldBuilder().seed(game.worldSeed()).build();
        cols = world.getWidth() / World.TILE;
        rows = world.getHeight() / World.TILE;
        open = new boolean[rows][cols];
        for (int ty = 0; ty < rows; ty++)
            for (int tx = 0; tx < cols; tx++)
                open[ty][tx] = !world.isWall(tx * World.TILE, ty * World.TILE);
    }

    /** Respawns missing bots, then gives every bot one tick of input chasing its closest human. */
    public void update(long now) {
        PlayerType[] types = PlayerType.values();
        for (int i = 0; i < botIds.size(); i++) {
            int id = botIds.get(i);
            if (!game.hasPlayer(id)) game.spawn(id, types[i % types.length], WeaponType.ASSAULT_RIFLE);
        }

        List<Player> all = game.players();
        Map<Integer, int[][]> routes = new HashMap<>();
        for (int id : botIds) {
            Player bot = find(all, id);
            Player target = closestHuman(all, bot);
            if (bot == null || target == null) continue;
            int[][] route = routes.computeIfAbsent(tileY(target) * cols + tileX(target),
                    k -> distancesFrom(tileX(target), tileY(target)));
            game.applyInput(id, chase(bot, target, route), now);
        }
    }

    private Player find(List<Player> players, int id) {
        for (Player p : players) if (p.id == id) return p;
        return null;
    }

    private Player closestHuman(List<Player> players, Player bot) {
        if (bot == null) return null;
        Player closest = null;
        double best = Double.MAX_VALUE;
        for (Player p : players) {
            if (botIds.contains(p.id) || p.health <= 0) continue;
            double distance = Math.hypot(p.x - bot.x, p.y - bot.y);
            if (distance < best) { best = distance; closest = p; }
        }
        return closest;
    }

    private PlayerInput chase(Player bot, Player target, int[][] route) {
        boolean moving = Math.hypot(target.x - bot.x, target.y - bot.y) > STOP_DISTANCE;
        double goalX = target.x, goalY = target.y;

        // Head for the centre of the neighbouring tile that is one step closer to the target.
        int bx = tileX(bot), by = tileY(bot);
        int steps = route[by][bx];
        if (steps > 0) {
            for (int[] d : STEPS) {
                int nx = bx + d[0], ny = by + d[1];
                if (nx >= 0 && ny >= 0 && nx < cols && ny < rows && route[ny][nx] == steps - 1) {
                    goalX = nx * World.TILE + World.TILE / 2.0;
                    goalY = ny * World.TILE + World.TILE / 2.0;
                    break;
                }
            }
        }

        double dx = goalX - bot.x, dy = goalY - bot.y;
        return new PlayerInput(moving && dy < -DEAD_ZONE, moving && dy > DEAD_ZONE,
                moving && dx < -DEAD_ZONE, moving && dx > DEAD_ZONE,
                false, target.x, target.y, MovementType.WALK, null);
    }

    /** Breadth-first walking distance, in tiles, from every reachable tile to the given tile; -1 if unreachable. */
    private int[][] distancesFrom(int tx, int ty) {
        int[][] dist = new int[rows][cols];
        for (int[] row : dist) Arrays.fill(row, -1);
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        dist[ty][tx] = 0;
        queue.add(new int[]{tx, ty});
        while (!queue.isEmpty()) {
            int[] c = queue.poll();
            for (int[] d : STEPS) {
                int nx = c[0] + d[0], ny = c[1] + d[1];
                if (nx < 0 || ny < 0 || nx >= cols || ny >= rows || !open[ny][nx] || dist[ny][nx] >= 0) continue;
                dist[ny][nx] = dist[c[1]][c[0]] + 1;
                queue.add(new int[]{nx, ny});
            }
        }
        return dist;
    }

    private int tileX(Player p) { return Math.max(0, Math.min(cols - 1, (int)p.x / World.TILE)); }
    private int tileY(Player p) { return Math.max(0, Math.min(rows - 1, (int)p.y / World.TILE)); }
}
