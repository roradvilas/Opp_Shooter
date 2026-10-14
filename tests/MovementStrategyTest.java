import java.awt.event.KeyEvent;
import javax.swing.JPanel;

public final class MovementStrategyTest {
    public static void main(String[] args) {
        World open = new WorldBuilder().size(20, 15).obstacles(0).seed(17).build();
        for (PlayerType type : PlayerType.values()) {
            for (MovementType mode : MovementType.values()) {
                Player player = PlayerFactory.forType(type).createPlayer(1, 200, 200);
                player.setMovement(mode);
                double multiplier = switch (mode) {
                    case SNEAK -> 0.5;
                    case WALK -> 1.0;
                    case SPRINT -> 1.5;
                    case DASH -> 3.0;
                };
                player.move(false, false, false, true, open, 1000);
                near(player.x - 200, player.speed * multiplier);
                player.x = player.y = 200;
                player.move(true, false, false, true, open, 1001);
                near(Math.hypot(player.x - 200, player.y - 200), player.speed * multiplier);
                double x = player.x, y = player.y;
                player.move(true, true, true, true, open, 1002);
                near(player.x, x); near(player.y, y);
                player.x = 63; player.y = 100;
                for (int i = 0; i < 300; i++) {
                    player.move(true, false, true, false, open, 1100 + i * 16);
                    check(!open.collidesWithWall(player.x, player.y, player.radius), "Movement crossed border");
                }
            }
        }
        Player player = new NormalPlayer(1, 200, 200);
        player.setMovement(MovementType.DASH);
        player.move(false, false, false, false, open, 1000);
        check(player.nextDashAt == 0, "Idle input consumed dash");
        displacement(player, open, 1000, 9.6);
        displacement(player, open, 1149, 9.6);
        displacement(player, open, 1150, 3.2);
        player.setMovement(MovementType.WALK);
        player.setMovement(MovementType.DASH);
        displacement(player, open, 1999, 3.2);
        displacement(player, open, 2000, 9.6);

        Input input = new Input();
        JPanel source = new JPanel();
        check(input.movementType() == MovementType.WALK, "Walk default");
        press(input, source, KeyEvent.VK_SHIFT);
        check(input.movementType() == MovementType.SPRINT, "Sprint key");
        press(input, source, KeyEvent.VK_CONTROL);
        check(input.movementType() == MovementType.SNEAK, "Sneak key priority");
        press(input, source, KeyEvent.VK_SPACE);
        check(input.movementType() == MovementType.DASH, "Dash key priority");
        input.reset();
        check(input.movementType() == MovementType.WALK, "Reset movement keys");
        System.out.println("PASS: four movement strategies, class multipliers, diagonal normalization, collisions, dash boundaries/cooldown, keyboard controls");
    }

    private static void displacement(Player player, World world, long now, double expected) {
        double x = player.x;
        player.move(false, false, false, true, world, now);
        near(player.x - x, expected);
    }
    private static void press(Input input, JPanel source, int code) {
        input.keyPressed(new KeyEvent(source, KeyEvent.KEY_PRESSED, 0, 0, code, KeyEvent.CHAR_UNDEFINED));
    }
    private static void near(double actual, double expected) {
        check(Math.abs(actual - expected) < 1e-9, "Expected " + expected + ", got " + actual);
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
