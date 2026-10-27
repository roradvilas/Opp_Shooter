package game;

import abstractfactory.CombatFactory;
import abstractfactory.TrainingFactory;
import facade.GameFacade;

/**
 * Usage: java game.ServerMain [--training] [port]
 * <p>
 * --training: players cannot die, get 999 ammo and logged shots, occasional moves are flagged as invalid and undone, and two bots chase the closest player.
 */
public class ServerMain {
    private static final int TRAINING_BOTS = 2;
    /** Chance per move that training mode flags it as an invalid packet and undoes it. */
    private static final double TRAINING_INVALID_MOVE_CHANCE = 0.005;

    public static void main(String[] args) throws Exception {
        int port = 8080;
        boolean training = true;
        for (String arg : args) {
            if (arg.equals("--training")) training = true;
            else port = Integer.parseInt(arg);
        }

        if (training) {
            GameFacade game = new GameFacade(new TrainingFactory(), TRAINING_INVALID_MOVE_CHANCE);
            new GameServer(port, game, new BotController(game, TRAINING_BOTS)).start();
        } else {
            new GameServer(port, new GameFacade(new CombatFactory())).start();
        }
    }
}
