package game;

import abstractfactory.CombatFactory;
import abstractfactory.TrainingFactory;
import builder.CombatPlayerBuilder;
import builder.TrainingPlayerBuilder;
import facade.GameFacade;

/**
 * Usage: java game.ServerMain [--training] [port]
 * <p>
 * --training: players cannot die, get plated armor and 999 ammo, and two bots chase the closest player.
 */
public class ServerMain {
    private static final int TRAINING_BOTS = 2;

    public static void main(String[] args) throws Exception {
        int port = 8080;
        boolean training = false;
        for (String arg : args) {
            if (arg.equals("--training")) training = true;
            else port = Integer.parseInt(arg);
        }

        if (training) {
            GameFacade game = new GameFacade(new TrainingFactory(), new TrainingPlayerBuilder());
            new GameServer(port, game, new BotController(game, TRAINING_BOTS)).start();
        } else {
            new GameServer(port, new GameFacade(new CombatFactory(), new CombatPlayerBuilder())).start();
        }
    }
}
