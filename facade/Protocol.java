package facade;

import bridge.Player;
import bridge.PlayerType;
import game.Loot;
import game.LootType;
import observer.PlayerEvent;
import prototype.Bullet;
import strategy.MovementType;
import strategy.WeaponType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The wire format in one place: both ends encode and decode through this class, so neither the
 * server nor the client builds or splits protocol strings itself.
 *
 * <pre>
 * client to server: SPAWN|TYPE|WEAPON   INPUT|w|s|a|d|shoot|mouseX|mouseY|MOVEMENT
 * server to client: WELCOME|id|seed   SPAWNED   DIED   EVENT|TYPE|amount
 *                   STATE|lastId|players|bullets|loot
 * </pre>
 */
public final class Protocol {
    public static final String SPAWNED = "SPAWNED";
    public static final String DIED = "DIED";

    public record SpawnRequest(PlayerType type, WeaponType weapon) {}

    private Protocol() {}

    public static String spawn(PlayerType type, WeaponType weapon) {
        return "SPAWN|" + type.name() + "|" + weapon.name();
    }

    /** Null if the message is not a well-formed SPAWN (or legacy JOIN) request. */
    public static SpawnRequest parseSpawn(String msg) {
        String[] p = msg.split("\\|", -1);
        if (!p[0].equals("SPAWN") && !p[0].equals("JOIN")) return null;
        if (p.length != 2 && p.length != 3) return null;
        try {
            return new SpawnRequest(PlayerType.valueOf(p[1]),
                    p.length == 3 ? WeaponType.valueOf(p[2]) : WeaponType.ASSAULT_RIFLE);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static String input(PlayerInput in) {
        return "INPUT|" + bit(in.w()) + "|" + bit(in.s()) + "|" + bit(in.a()) + "|" + bit(in.d())
                + "|" + bit(in.shoot()) + "|" + in.mouseX() + "|" + in.mouseY() + "|" + in.movement().name();
    }

    /** Null if the message is not a well-formed INPUT. */
    public static PlayerInput parseInput(String msg) {
        String[] p = msg.split("\\|");
        if ((p.length != 8 && p.length != 9) || !p[0].equals("INPUT")) return null;
        try {
            double x = Double.parseDouble(p[6]), y = Double.parseDouble(p[7]);
            MovementType mode = p.length == 9 ? MovementType.valueOf(p[8]) : MovementType.WALK;
            if (!Double.isFinite(x) || !Double.isFinite(y)) return null;
            return new PlayerInput(p[1].equals("1"), p[2].equals("1"), p[3].equals("1"),
                    p[4].equals("1"), p[5].equals("1"), x, y, mode, null);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static String welcome(int playerId, long worldSeed) {
        return "WELCOME|" + playerId + "|" + worldSeed;
    }

    public static String event(PlayerEvent event) {
        return "EVENT|" + event.type().name() + "|" + event.amount();
    }

    public static String state(int lastSpawnedId, Collection<Player> players,
                               Collection<Bullet> bullets, Collection<Loot> loot) {
        StringBuilder s = new StringBuilder("STATE|").append(lastSpawnedId).append("|");
        boolean first = true;
        for (Player p : players) {
            if (!first) s.append(';');
            first = false;
            s.append(p.id).append(',').append(Math.round(p.x)).append(',')
             .append(Math.round(p.y)).append(',').append(p.health).append(',')
             .append(p.ammo).append(',').append(p.loot).append(',')
             .append(p.type.name()).append(',').append(p.maxHealth).append(',')
             .append(p.angle).append(',').append(p.weaponType.name());
        }

        s.append("|");
        first = true;
        for (Bullet b : bullets) {
            if (!first) s.append(';');
            first = false;
            s.append(b.id).append(',').append(Math.round(b.x)).append(',').append(Math.round(b.y));
        }

        s.append("|");
        first = true;
        for (Loot l : loot) {
            if (!first) s.append(';');
            first = false;
            s.append(l.x()).append(',').append(l.y()).append(',').append(l.type().name());
        }
        return s.toString();
    }

    /** Null if the message is not a well-formed STATE. */
    public static GameState parseState(String msg) {
        String[] parts = msg.split("\\|", -1);
        if (!parts[0].equals("STATE") || parts.length < 5) return null;
        try {
            Map<Integer, PlayerView> players = new HashMap<>();
            if (!parts[2].isEmpty()) {
                for (String row : parts[2].split(";")) {
                    String[] p = row.split(",");
                    if (p.length < 6) continue;
                    int id = Integer.parseInt(p[0]);
                    players.put(id, new PlayerView(
                            id, Double.parseDouble(p[1]), Double.parseDouble(p[2]),
                            Integer.parseInt(p[3]), Integer.parseInt(p[4]), Integer.parseInt(p[5]),
                            p.length >= 8 ? PlayerType.valueOf(p[6]) : PlayerType.NORMAL,
                            p.length >= 8 ? Integer.parseInt(p[7]) : 100,
                            p.length >= 10 ? Double.parseDouble(p[8]) : 0,
                            p.length >= 10 ? WeaponType.valueOf(p[9]) : WeaponType.ASSAULT_RIFLE));
                }
            }

            List<BulletView> bullets = new ArrayList<>();
            if (!parts[3].isEmpty()) {
                for (String row : parts[3].split(";")) {
                    String[] p = row.split(",");
                    if (p.length >= 3)
                        bullets.add(new BulletView(Long.parseLong(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2])));
                }
            }

            List<Loot> loot = new ArrayList<>();
            if (!parts[4].isEmpty()) {
                for (String row : parts[4].split(";")) {
                    String[] p = row.split(",");
                    if (p.length >= 2)
                        loot.add(new Loot(Integer.parseInt(p[0]), Integer.parseInt(p[1]),
                                p.length >= 3 ? LootType.valueOf(p[2]) : LootType.AMMO));
                }
            }
            return new GameState(players, bullets, loot);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static int bit(boolean b) { return b ? 1 : 0; }
}
