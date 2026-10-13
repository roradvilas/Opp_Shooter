import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class Main {
    public static void main(String[] args) {
        PlayerType type = (PlayerType)JOptionPane.showInputDialog(null,
                "Choose your player type", "Player selection", JOptionPane.QUESTION_MESSAGE,
                null, PlayerType.values(), PlayerType.HEAVY);
        if (type == null) return;
        JFrame frame = new JFrame("Hell Is Others Lite - PvP");
        Game game = new Game(type);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.add(game);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        game.start();
    }
}
